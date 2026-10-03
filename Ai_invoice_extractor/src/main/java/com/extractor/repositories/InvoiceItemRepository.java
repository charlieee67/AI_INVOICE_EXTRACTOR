package com.extractor.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.extractor.entities.InvoiceItem;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long>{
	
	
	
}
