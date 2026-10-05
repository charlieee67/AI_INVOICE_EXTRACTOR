package com.extractor.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.extractor.dto.InvoiceRequest;
import com.extractor.dto.InvoiceResponse;

public interface InvoiceService {

    InvoiceResponse extractInvoice(MultipartFile file);

    InvoiceResponse getById(Long id);

    Page<InvoiceResponse> getAll(String status, Pageable pageable);

    InvoiceResponse update(Long id, InvoiceRequest request);

    InvoiceResponse confirm(Long id);

    void delete(Long id);
    
}