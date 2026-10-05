package com.extractor.serviceImpl;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.extractor.dto.ValidationResult;
import com.extractor.entities.Invoice;
import com.extractor.entities.InvoiceItem;
import com.extractor.exception.ApiException;
import com.extractor.services.ValidationService;

@Service
public class ValidationServiceImpl implements ValidationService {

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;               // 5 MB
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");       // line and sum checks
    private static final BigDecimal TOTAL_TOLERANCE = new BigDecimal("1.00"); // allows a round-off line
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final Pattern GSTIN_PATTERN =
            Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    // ---------- File checks (hard errors: throw) ----------

    @Override
    public void validateFile(MultipartFile file) {
    	System.out.println(file);
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please upload a file that is not empty");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large. Maximum allowed size is 5 MB");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF files are allowed");
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only PDF files are allowed");
        }
        try {
            byte[] header = file.getInputStream().readNBytes(5);
            String start = new String(header, StandardCharsets.ISO_8859_1);
            if (!start.startsWith("%PDF-")) {
                throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File content is not a real PDF");
            }
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
    }

    // ---------- Invoice checks (soft warnings: never throw) ----------

    @Override
    public ValidationResult validate(Invoice invoice) {
        List<String> warnings = new ArrayList<>();
        BigDecimal itemsTaxableSum = BigDecimal.ZERO;
        boolean everyItemHasTaxableValue = true;

        for (InvoiceItem item : invoice.getItems()) {
            String line = "Line " + item.getLineNo() + ": ";

            // quantity x unit price = taxable value (discounts can cause a mismatch)
            if (item.getQuantity() != null && item.getUnitPrice() != null && item.getTaxableValue() != null) {
                BigDecimal expected = item.getQuantity().multiply(item.getUnitPrice());
                if (differs(expected, item.getTaxableValue( ), TOLERANCE)) {
                    warnings.add(line + "quantity x unit price is " + money(expected)
                            + " but taxable value is " + money(item.getTaxableValue()));
                }
            }

            // taxable value x rate / 100 = tax amount
            if (item.getTaxableValue() != null && item.getTaxRate() != null && item.getTaxAmount() != null) {
                BigDecimal expectedTax = item.getTaxableValue().multiply(item.getTaxRate())
                        .divide(HUNDRED, 2, RoundingMode.HALF_UP);
                if (differs(expectedTax, item.getTaxAmount(), TOLERANCE)) {
                    warnings.add(line + "tax should be " + money(expectedTax)
                            + " but invoice shows " + money(item.getTaxAmount()));
                }
            }

            // taxable value + tax = line total
            if (item.getTaxableValue() != null && item.getTaxAmount() != null && item.getLineTotal() != null) {
                BigDecimal expectedTotal = item.getTaxableValue().add(item.getTaxAmount());
                if (differs(expectedTotal, item.getLineTotal(), TOLERANCE)) {
                    warnings.add(line + "taxable value + tax is " + money(expectedTotal)
                            + " but line total is " + money(item.getLineTotal()));
                }
            }

            if (item.getTaxableValue() != null) {
                itemsTaxableSum = itemsTaxableSum.add(item.getTaxableValue());
            } else {
                everyItemHasTaxableValue = false;
            }
        }

        // sum of item taxable values = invoice taxable amount
        if (invoice.getTaxableAmount() == null) {
            warnings.add("Taxable amount is missing");
        } else if (!invoice.getItems().isEmpty() && everyItemHasTaxableValue
                && differs(itemsTaxableSum, invoice.getTaxableAmount(), TOLERANCE)) {
            warnings.add("Items add up to " + money(itemsTaxableSum)
                    + " but invoice taxable amount is " + money(invoice.getTaxableAmount()));
        }

        // taxable amount + CGST + SGST + IGST = total
        if (invoice.getTotalAmount() == null) {
            warnings.add("Total amount is missing");
        } else if (invoice.getTaxableAmount() != null) {
            BigDecimal expected = invoice.getTaxableAmount()
                    .add(invoice.getCgstAmount()).add(invoice.getSgstAmount()).add(invoice.getIgstAmount());
            if (differs(expected, invoice.getTotalAmount(), TOTAL_TOLERANCE)) {
                warnings.add("Total should be " + money(expected)
                        + " but invoice shows " + money(invoice.getTotalAmount()));
            }
        }

        // IGST cannot appear together with CGST / SGST
        if (isNonZero(invoice.getIgstAmount())
                && (isNonZero(invoice.getCgstAmount()) || isNonZero(invoice.getSgstAmount()))) {
            warnings.add("IGST and CGST/SGST are both present. Only one type is normally used");
        }

        // dates
        if (invoice.getInvoiceDate() != null && invoice.getDueDate() != null
                && invoice.getDueDate().isBefore(invoice.getInvoiceDate())) {
            warnings.add("Due date is before the invoice date");
        }

        // GSTIN format
        if (invoice.getVendorGstin() != null && !GSTIN_PATTERN.matcher(invoice.getVendorGstin()).matches()) {
            warnings.add("Vendor GSTIN format looks wrong: " + invoice.getVendorGstin());
        }

        return new ValidationResult(warnings.isEmpty(), warnings);
    }

    private static boolean differs(BigDecimal a, BigDecimal b, BigDecimal tolerance) {
        return a.subtract(b).abs().compareTo(tolerance) > 0;   // compareTo, never equals, for BigDecimal
    }

    private static boolean isNonZero(BigDecimal value) {
        return value != null && value.signum() != 0;
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}