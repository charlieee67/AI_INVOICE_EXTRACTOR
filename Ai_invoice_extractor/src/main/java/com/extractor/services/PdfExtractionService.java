package com.extractor.services;

import org.springframework.web.multipart.MultipartFile;

public interface PdfExtractionService {

    String extractText(MultipartFile file);
    
}