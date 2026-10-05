package com.extractor.mapper;

import java.math.BigDecimal;
import java.util.List;

import com.extractor.dto.InvoiceItemRequest;
import com.extractor.dto.InvoiceItemResponse;
import com.extractor.dto.InvoiceRequest;
import com.extractor.dto.InvoiceResponse;
import com.extractor.entities.Invoice;
import com.extractor.entities.InvoiceItem;

public final class InvoiceMapper {

    private InvoiceMapper() {
    }

    public static Invoice toEntity(InvoiceRequest request) {
        Invoice invoice = new Invoice();
        applyFields(invoice, request);
        replaceItems(invoice, request.items());
        return invoice;
    }

    /** Copies the editable fields. Never touches rawAiResponse, status or source file name. */
    public static void applyFields(Invoice invoice, InvoiceRequest r) {
        invoice.setInvoiceNumber(limit(r.invoiceNumber(), 50));
        invoice.setInvoiceDate(r.invoiceDate());
        invoice.setDueDate(r.dueDate());
        invoice.setVendorName(limit(r.vendorName(), 200));
        invoice.setVendorGstin(upper(limit(r.vendorGstin(), 15)));
        invoice.setCustomerName(limit(r.customerName(), 200));
        invoice.setPlaceOfSupply(limit(r.placeOfSupply(), 100));
        invoice.setCurrency(isBlank(r.currency()) ? "INR" : upper(limit(r.currency(), 3)));
        invoice.setTaxableAmount(r.taxableAmount());
        invoice.setCgstAmount(orZero(r.cgstAmount()));
        invoice.setSgstAmount(orZero(r.sgstAmount()));
        invoice.setIgstAmount(orZero(r.igstAmount()));
        invoice.setTotalAmount(r.totalAmount());
    }

    /** Removes the old items and adds the new ones. Uses clear() so orphanRemoval deletes the old rows. */
    public static void replaceItems(Invoice invoice, List<InvoiceItemRequest> items) {
        invoice.getItems().clear();
        if (items == null) {
            return;
        }
        int position = 1;
        for (InvoiceItemRequest i : items) {
            InvoiceItem item = new InvoiceItem();
            item.setLineNo(i.lineNo() != null ? i.lineNo() : position);
            item.setDescription(isBlank(i.description()) ? "(no description)" : limit(i.description(), 500));
            item.setHsnCode(limit(i.hsnCode(), 10));
            item.setQuantity(i.quantity());
            item.setUnitPrice(i.unitPrice());
            item.setTaxableValue(i.taxableValue());
            item.setTaxRate(i.taxRate());
            item.setTaxAmount(i.taxAmount());
            item.setLineTotal(i.lineTotal());
            invoice.addItem(item);
            position++;
        }
    }

    /** includeItems = false is used for list screens, so we do not load every item (avoids many extra queries). */
    public static InvoiceResponse toResponse(Invoice inv, List<String> warnings, boolean includeItems) {
        List<InvoiceItemResponse> items = includeItems
                ? inv.getItems().stream().map(InvoiceMapper::toItemResponse).toList()
                : List.of();
        return new InvoiceResponse(
                inv.getId(), inv.getInvoiceNumber(), inv.getInvoiceDate(), inv.getDueDate(),
                inv.getVendorName(), inv.getVendorGstin(), inv.getCustomerName(), inv.getPlaceOfSupply(),
                inv.getCurrency(), inv.getTaxableAmount(), inv.getCgstAmount(), inv.getSgstAmount(),
                inv.getIgstAmount(), inv.getTotalAmount(), inv.getStatus(), inv.getValidationPassed(),
                warnings, inv.getSourceFileName(), inv.getCreatedAt(), inv.getUpdatedAt(), items);
    }

    private static InvoiceItemResponse toItemResponse(InvoiceItem i) {
        return new InvoiceItemResponse(i.getId(), i.getLineNo(), i.getDescription(), i.getHsnCode(),
                i.getQuantity(), i.getUnitPrice(), i.getTaxableValue(), i.getTaxRate(),
                i.getTaxAmount(), i.getLineTotal());
    }

    public static String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private static String upper(String value) {
        return value == null ? null : value.toUpperCase();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}