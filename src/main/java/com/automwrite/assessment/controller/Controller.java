package com.automwrite.assessment.controller;

import com.automwrite.assessment.service.llm.LlmService;
import com.automwrite.assessment.service.storage.*;
import com.automwrite.assessment.service.storage.impl.*;
import com.automwrite.assessment.service.transposition.*;
import com.automwrite.assessment.service.transposition.impl.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

@Slf4j
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class Controller {

    private final LlmService llmService;
    private final DocxToneManagementService transpose;
    private final FileStorageServiceImpl fileStorage;

    /**
     * You should extract the tone from the `toneFile` and update the `contentFile` to convey the same content
     * but using the extracted tone.
     * @param toneFile File to extract the tone from
     * @param contentFile File to apply the tone to
     * @return A response indicating that the processing has completed
     */
    @PostMapping("/test")
    public ResponseEntity<String> test(@RequestParam MultipartFile toneFile, @RequestParam MultipartFile contentFile) throws Exception {
        requireValidFile(toneFile);
        requireValidFile(contentFile);

        String toneDocumentFileName = toneFile.getOriginalFilename();
        String contentDocumentFileName = contentFile.getOriginalFilename();

        XWPFDocument toneDocument = new XWPFDocument(toneFile.getInputStream());
        fileStorage.write(toneDocumentFileName, FileCategory.TONE_SOURCE, toneDocument);

        XWPFDocument contentDocument = new XWPFDocument(contentFile.getInputStream());
        fileStorage.write(contentDocumentFileName, FileCategory.ORIGINAL_TONE, contentDocument);

        // Process Documents
        StylisticTone tone = transpose.extractTone(toneDocument);
        XWPFDocument updatedDoc = transpose.applyTone(contentDocument, tone);
        fileStorage.write(contentDocumentFileName, FileCategory.ADJUSTED_TONE, updatedDoc);

        // Simple response to indicate that everything completed
        return ResponseEntity.ok("File successfully uploaded, processing completed");
    }


    public void requireValidFile(MultipartFile file) {
        if (file == null) {
            throw new IllegalArgumentException("File must be provided.");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }
    }
}
