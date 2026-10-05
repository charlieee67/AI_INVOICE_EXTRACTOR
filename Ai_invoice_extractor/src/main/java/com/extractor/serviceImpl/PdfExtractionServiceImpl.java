package com.extractor.serviceImpl;

import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.extractor.exception.ApiException;
import com.extractor.services.PdfExtractionService;

@Service
public class PdfExtractionServiceImpl implements PdfExtractionService {

    private static final int MAX_PAGES = 10;
    private static final int MAX_TEXT_LENGTH = 20_000;

    @Override
    public String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {

            if (document.getNumberOfPages() > MAX_PAGES) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "PDF has too many pages. Maximum allowed is " + MAX_PAGES);
            }

            String text = new PDFTextStripper().getText(document);

            if (text == null || text.isBlank()) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "No readable text found. Scanned or image-only PDFs are not supported yet");
            }

            text = text.strip();
            if (text.length() > MAX_TEXT_LENGTH) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PDF contains too much text for one invoice");
            }
            return text;

        } catch (InvalidPasswordException e) {          // must come before IOException
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PDF is password-protected");
        } catch (IOException e) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Could not read the PDF. The file may be corrupted");
        }
    }
}