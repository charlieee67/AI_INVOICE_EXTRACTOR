package com.extractor.dto;

import java.util.List;

public record ValidationResult(boolean passed, List<String> warnings) {
	
}