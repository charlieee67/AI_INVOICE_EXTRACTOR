package com.extractor.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;

public record InvoiceItemRequest(
        Integer lineNo,
        @NotBlank String description,
        String hsnCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxableValue,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal lineTotal) {
}