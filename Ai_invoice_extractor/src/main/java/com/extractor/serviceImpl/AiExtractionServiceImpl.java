package com.extractor.serviceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.extractor.exception.ApiException;
import com.extractor.services.AiExtractionService;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

/** Calls the Google Gemini API and returns the raw text reply (expected to be JSON). */
@Slf4j
@Service
public class AiExtractionServiceImpl implements AiExtractionService {

    private static final String PLACEHOLDER = "{{INVOICE_TEXT}}";

    private final RestClient restClient;
    private final String model;
    private final String promptTemplate;

    public AiExtractionServiceImpl(
            @Value("${ai.api-key}") String apiKey,
            @Value("${ai.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${ai.model}") String model) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);    // 5 seconds to connect
        factory.setReadTimeout(30_000);      // 30 seconds to wait for the answer

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("x-goog-api-key", apiKey.trim())   // trim: removes accidental spaces
                .build();
        this.model = model.trim();
        this.promptTemplate = loadPrompt();
    }

    @Override
    public String extractInvoice(String invoiceText) {
        String prompt = promptTemplate.replace(PLACEHOLDER, invoiceText);

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "maxOutputTokens", 8000));

        try {
            JsonNode response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            String text = response == null ? ""
                    : response.path("candidates").path(0).path("content")
                              .path("parts").path(0).path("text").asString("");
            if (text.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI service returned an empty answer");
            }
            return text;

        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            log.error("AI API returned HTTP {}", status);   // never log the invoice text or the key
            if (status == 429) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                        "AI free limit reached. Please wait a minute and try again");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "AI service returned an error. Please try again later");
        } catch (ResourceAccessException e) {
            log.error("AI API call failed or timed out");
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,
                    "AI service took too long to answer. Please try again");
        }
    }

    private static String loadPrompt() {
        try (InputStream in = new ClassPathResource("prompts/invoice-extraction.txt").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load the AI prompt file", e);
        }
    }
}