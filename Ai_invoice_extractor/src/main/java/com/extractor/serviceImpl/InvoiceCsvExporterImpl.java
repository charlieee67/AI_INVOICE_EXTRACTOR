package com.extractor.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.extractor.entities.Invoice;
import com.extractor.entities.InvoiceItem;
import com.extractor.exception.ApiException;
import com.extractor.services.InvoiceCsvExporter;

/** Builds a CSV file with one row per invoice line item (invoice fields are repeated on each row). */
@Component
public class InvoiceCsvExporterImpl implements InvoiceCsvExporter{

    private static final String[] HEADERS = {
            "invoice_number", "invoice_date", "due_date", "vendor_name", "vendor_gstin",
            "customer_name", "place_of_supply", "currency",
            "line_no", "description", "hsn_code", "quantity", "unit_price",
            "taxable_value", "tax_rate", "tax_amount", "line_total",
            "invoice_taxable_amount", "cgst_amount", "sgst_amount", "igst_amount", "invoice_total"
    };

    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder().setHeader(HEADERS).build();

    public byte[] export(List<Invoice> invoices) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
            writer.write('\uFEFF');   // BOM (byte order mark): makes Excel read the file as UTF-8, so symbols like ₹ show correctly

            try (CSVPrinter printer = new CSVPrinter(writer, FORMAT)) {   // closing the printer flushes the writer
                for (Invoice inv : invoices) {
                    List<InvoiceItem> items = inv.getItems().stream()
                            .sorted(Comparator.comparing(InvoiceItem::getLineNo))
                            .toList();
                    for (InvoiceItem item : items) {
                        printer.printRecord(
                                safeText(inv.getInvoiceNumber()),
                                inv.getInvoiceDate(),
                                inv.getDueDate(),
                                safeText(inv.getVendorName()),
                                safeText(inv.getVendorGstin()),
                                safeText(inv.getCustomerName()),
                                safeText(inv.getPlaceOfSupply()),
                                safeText(inv.getCurrency()),
                                item.getLineNo(),
                                safeText(item.getDescription()),
                                safeText(item.getHsnCode()),
                                plain(item.getQuantity()),
                                plain(item.getUnitPrice()),
                                plain(item.getTaxableValue()),
                                plain(item.getTaxRate()),
                                plain(item.getTaxAmount()),
                                plain(item.getLineTotal()),
                                plain(inv.getTaxableAmount()),
                                plain(inv.getCgstAmount()),
                                plain(inv.getSgstAmount()),
                                plain(inv.getIgstAmount()),
                                plain(inv.getTotalAmount()));
                    }
                }
            }
            return out.toByteArray();

        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create the CSV file");
        }
    }

    /**
     * CSV injection defence: a text cell that starts with = + - @ can run as a formula in Excel.
     * Vendor names and descriptions come from the AI (untrusted), so we put a ' in front of such text.
     * Only used for text columns, so real negative numbers are not touched.
     */
    private static String safeText(String value) {
        if (value == null) {
            return "";
        }
        if (!value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }

    /** toPlainString avoids scientific notation such as 1E+3. */
    private static String plain(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }
}