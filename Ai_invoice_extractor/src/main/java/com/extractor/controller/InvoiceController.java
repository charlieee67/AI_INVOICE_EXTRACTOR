package com.extractor.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.extractor.dto.InvoiceRequest;
import com.extractor.dto.InvoiceResponse;
import com.extractor.entities.Invoice;
import com.extractor.repositories.InvoiceRepository;
import com.extractor.services.InvoiceCsvExporter;
import com.extractor.services.InvoiceService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

@RequestMapping("/api")
@RestController
public class InvoiceController {
	
	
	private final InvoiceService invoiceService;
	private final InvoiceCsvExporter csvExporter;
	private final InvoiceRepository invoiceRepository;
	
	public InvoiceController(InvoiceCsvExporter csvExporter,InvoiceService invoiceService,InvoiceRepository invoiceRepository) {
		this.invoiceService=invoiceService;
		this.csvExporter=csvExporter;
		this.invoiceRepository=invoiceRepository;
	}
	
	@PostMapping("/invoice/extract")
	public ResponseEntity<InvoiceResponse> extract(@RequestParam("file") MultipartFile file ) {
		
		InvoiceResponse invoiceResponse= invoiceService.extractInvoice(file);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(invoiceResponse);
		
	}
	
	@GetMapping("/invoices")
	public Page<InvoiceResponse> getInvoices(
	        @RequestParam String status,
	        @PageableDefault(
	                page = 0,
	                size = 10,
	                sort = "id",
	                direction = Sort.Direction.DESC
	        ) Pageable pageable) {

	    return invoiceService.getAll(status, pageable);
	}	
	
	@GetMapping("/invoices/{id}")
	public InvoiceResponse getInvoice(@PathVariable Long id ) {
		return invoiceService.getById(id);
	}
	
	@PutMapping("/invoices/{id}")
	public InvoiceResponse updateInvoice(@PathVariable Long id,@RequestBody InvoiceRequest invoiceRequest) {
		return invoiceService.update(id, invoiceRequest);
	}
		
	@PostMapping("/invoices/{id}/confirm")
	public InvoiceResponse confirmByUser(@PathVariable Long id) {
		return invoiceService.confirm(id);		
	}
	
	@PostMapping("/invoices/{id}")
	public void deleteInvoice(@PathVariable Long id) {
		invoiceService.delete(id);
	}
	
	@GetMapping("/invoices/export/csv")
    public ResponseEntity<byte[]> exportCsv() {

        List<Invoice> invoices = invoiceRepository.findAll();

        byte[] csv = csvExporter.export(invoices);

        return ResponseEntity.ok()
                .header(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=invoices.csv"
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
