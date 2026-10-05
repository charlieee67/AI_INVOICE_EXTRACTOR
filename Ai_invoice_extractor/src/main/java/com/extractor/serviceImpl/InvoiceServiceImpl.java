package com.extractor.serviceImpl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.extractor.dto.InvoiceRequest;
import com.extractor.dto.InvoiceResponse;
import com.extractor.dto.ValidationResult;
import com.extractor.entities.Invoice;
import com.extractor.exception.ApiException;
import com.extractor.mapper.InvoiceMapper;
import com.extractor.repositories.InvoiceRepository;
import com.extractor.services.AiExtractionService;
import com.extractor.services.InvoiceService;
import com.extractor.services.PdfExtractionService;
import com.extractor.services.ValidationService;
import com.extractor.util.LenientBigDecimalDeserializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;


@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final String CONFIRMED = "CONFIRMED";
    private static final ObjectMapper AI_PARSER = buildAiParser();

    private final InvoiceRepository invoiceRepository;
    private final PdfExtractionService pdfExtractionService;
    private final AiExtractionService aiExtractionService;
    private final ValidationService validationService;

    // ------------------------------------------------------------------
    // Upload flow. No @Transactional here: the AI call is slow and would
    // keep a database connection busy. save() has its own transaction.
    // ------------------------------------------------------------------
    @Override
    public InvoiceResponse extractInvoice(MultipartFile file) {
        validationService.validateFile(file);                              // 1. throws if bad file
        String text = pdfExtractionService.extractText(file);             // 2. throws if no text
        String json = isolateJson(aiExtractionService.extractInvoice(text)); // 3. throws if AI fails
        InvoiceRequest request = parseAiJson(json);                        // 4. throws if unusable

        Invoice invoice = InvoiceMapper.toEntity(request);
        invoice.setSourceFileName(InvoiceMapper.limit(file.getOriginalFilename(), 255)); // stored as text only
        invoice.setRawAiResponse(json);                                    // original AI answer, never edited later
        invoice.setStatus(PENDING_REVIEW);

        if (invoiceRepository.existsByVendorNameAndInvoiceNumber(invoice.getVendorName(), invoice.getInvoiceNumber())) {
            throw new ApiException(HttpStatus.CONFLICT, "Invoice " + invoice.getInvoiceNumber()
                    + " from " + invoice.getVendorName() + " already exists");
        }

        ValidationResult result = validationService.validate(invoice);     // 5. warnings, no exception
        invoice.setValidationPassed(result.passed());

        Invoice saved = save(invoice);                                     // 6. saves invoice + items
        return InvoiceMapper.toResponse(saved, result.warnings(), true);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getById(Long id) {
        Invoice invoice = find(id);
        return InvoiceMapper.toResponse(invoice, validationService.validate(invoice).warnings(), true);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getAll(String status, Pageable pageable) {
        Page<Invoice> page = (status == null || status.isBlank())
                ? invoiceRepository.findAll(pageable)
                : invoiceRepository.findByStatus(status.trim().toUpperCase(), pageable);
        return page.map(invoice -> InvoiceMapper.toResponse(invoice, List.of(), false));
    }

    @Override
    @Transactional
    public InvoiceResponse update(Long id, InvoiceRequest request) {
        Invoice invoice = find(id);

        if (CONFIRMED.equals(invoice.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "A confirmed invoice cannot be edited");
        }

        String vendor = InvoiceMapper.limit(request.vendorName(), 200);
        String number = InvoiceMapper.limit(request.invoiceNumber(), 50);
        if (invoiceRepository.existsByVendorNameAndInvoiceNumberAndIdNot(vendor, number, id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Another invoice with this vendor and number already exists");
        }

        InvoiceMapper.applyFields(invoice, request);      // rawAiResponse is never touched
        InvoiceMapper.replaceItems(invoice, request.items());

        ValidationResult result = validationService.validate(invoice);
        invoice.setValidationPassed(result.passed());

        Invoice saved = invoiceRepository.saveAndFlush(invoice);   // flush so updatedAt is fresh in the response
        return InvoiceMapper.toResponse(saved, result.warnings(), true);
    }

    @Override
    @Transactional
    public InvoiceResponse confirm(Long id) {
        Invoice invoice = find(id);

        if (!PENDING_REVIEW.equals(invoice.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Only invoices that are pending review can be confirmed");
        }

        ValidationResult result = validationService.validate(invoice);   // check again, do not trust the stored flag
        if (!result.passed()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Fix these problems before confirming: " + String.join("; ", result.warnings()));
        }

        invoice.setValidationPassed(true);
        invoice.setStatus(CONFIRMED);
        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        return InvoiceMapper.toResponse(saved, List.of(), true);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        invoiceRepository.delete(find(id));   // cascade removes the items
    }

    // ---------------------------- helpers ----------------------------

    private Invoice find(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invoice not found with id " + id));
    }

    private Invoice save(Invoice invoice) {
        try {
            return invoiceRepository.save(invoice);
        } catch (DataIntegrityViolationException e) {
            log.warn("Invoice could not be saved: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.CONFLICT,
                    "Invoice could not be saved. It may already exist or contain values that are too long");
        }
    }

    /** Cuts everything before the first { and after the last }, so ```json fences or chatter do not break parsing. */
    private String isolateJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "AI did not return invoice data. Please try again");
        }
        return raw.substring(start, end + 1);
    }

    private InvoiceRequest parseAiJson(String json) {
    	InvoiceRequest request;
        try {
            request = AI_PARSER.readValue(json, InvoiceRequest.class);
        } catch (JacksonException e) {
            log.warn("AI answer could not be parsed: {}", e.getClass().getSimpleName());
            throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "AI returned data in an unexpected format. Please try again");
        }
        if (isBlank(request.invoiceNumber()) || isBlank(request.vendorName())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not find the invoice number or vendor name in this PDF");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Could not find any line items in this PDF");
        }
        return request;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ObjectMapper buildAiParser() {
        SimpleModule amounts = new SimpleModule()
                .addDeserializer(BigDecimal.class, new LenientBigDecimalDeserializer());
        return JsonMapper.builder()
                .addModule(amounts)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }
}