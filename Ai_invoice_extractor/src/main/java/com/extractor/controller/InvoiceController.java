package com.extractor.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.extractor.dto.InvoiceResponse;
import com.extractor.services.InvoiceService;

@RequestMapping("/api")
@RestController
public class InvoiceController {
	
	
	private final InvoiceService invoiceService;
	
	public InvoiceController(InvoiceService invoiceService) {
		this.invoiceService=invoiceService;
	}
	
	@PostMapping("/invoice/extract")
	public ResponseEntity<InvoiceResponse> extract(@RequestParam("file") MultipartFile file ) {
		
		InvoiceResponse invoiceResponse= invoiceService.extractInvoice(file);
		
		return ResponseEntity.status(HttpStatus.CREATED).body(invoiceResponse);
		
	}
}
