package com.extractor.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record InvoiceRequest(
        @NotBlank String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        @NotBlank String vendorName,
        String vendorGstin,
        String customerName,
        String placeOfSupply,
        String currency,
        BigDecimal taxableAmount,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal totalAmount,
        @NotEmpty @Valid List<InvoiceItemRequest> items) {
}