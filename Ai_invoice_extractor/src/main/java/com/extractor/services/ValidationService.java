package com.extractor.services;

import org.springframework.web.multipart.MultipartFile;

import com.extractor.dto.ValidationResult;
import com.extractor.entities.Invoice;

public interface ValidationService {

    /** Checks the uploaded file. Throws ApiException if the file is not acceptable. */
    void validateFile(MultipartFile file);

    /** Checks the invoice numbers and rules. Never throws; returns warnings instead. */
    ValidationResult validate(Invoice invoice);
}