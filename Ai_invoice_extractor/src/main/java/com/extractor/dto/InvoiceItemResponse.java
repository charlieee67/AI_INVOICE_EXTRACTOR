package com.extractor.dto;

import java.math.BigDecimal;

public record InvoiceItemResponse(
        Long id,
        Integer lineNo,
        String description,
        String hsnCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxableValue,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal lineTotal) {
}