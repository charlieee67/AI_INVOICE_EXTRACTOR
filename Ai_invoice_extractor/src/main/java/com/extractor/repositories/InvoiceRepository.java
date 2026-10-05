package com.extractor.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.extractor.entities.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByVendorNameAndInvoiceNumber(String vendorName, String invoiceNumber);

    boolean existsByVendorNameAndInvoiceNumberAndIdNot(String vendorName, String invoiceNumber, Long id);

    Page<Invoice> findByStatus(String status, Pageable pageable);
}