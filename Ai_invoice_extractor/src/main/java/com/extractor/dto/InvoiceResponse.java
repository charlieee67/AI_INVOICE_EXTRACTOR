package com.extractor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        String vendorName,
        String vendorGstin,
        String customerName,
        String placeOfSupply,
        String currency,
        BigDecimal taxableAmount,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal totalAmount,
        String status,
        Boolean validationPassed,
        List<String> warnings,
        String sourceFileName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<InvoiceItemResponse> items) {
}