package com.extractor.services;

import java.util.List;

import com.extractor.entities.Invoice;

public interface InvoiceCsvExporter {

    byte[] export(List<Invoice> invoices);

}