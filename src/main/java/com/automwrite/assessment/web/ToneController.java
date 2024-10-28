package com.automwrite.assessment.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.automwrite.assessment.tone.ToneService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class ToneController {

    private ToneService toneService;

    /**
     * You should extract the tone from the `toneFile` and update the `contentFile` to convey the same content
     * but using the extracted tone.
     * @param toneFile File to extract the tone from
     * @param contentFile File to apply the tone to
     * @return A response indicating that the processing has completed
     */
    @PostMapping("/transform/tone")
    public ResponseEntity<Map<String, String>> transformTone(@RequestParam MultipartFile toneFile,
                                                             @RequestParam MultipartFile contentFile)
            throws IOException {
        Objects.requireNonNull(toneFile, "Received toneFile must not be null");
        Objects.requireNonNull(contentFile, "Received contentFile must not be null");

        XWPFDocument toneDocument = new XWPFDocument(toneFile.getInputStream());
        String toneDocumentTitle = titleOf(toneDocument);
        XWPFDocument contentDocument = new XWPFDocument(contentFile.getInputStream());
        String contentDocumentTitle = titleOf(contentDocument);

        log.info("Received toneFile '{}' and contentFile '{}' for tone transformation", toneDocumentTitle,
                contentDocumentTitle);

        return toneService.transformTone(toneDocument, contentDocument)
                .thenApply(saveFile())
                .thenApply(logAndReturnSuccess(toneDocumentTitle, contentDocumentTitle))
                .exceptionally(logFailureAndReturnError(toneDocumentTitle, contentDocumentTitle))
                .join();
    }

    private Function<XWPFDocument, XWPFDocument> saveFile() {
        return xwpfDocument -> saveDocument(xwpfDocument, "different tones/transformed tone.docx");
    }

    private Function<Throwable, ResponseEntity<Map<String, String>>> logFailureAndReturnError(String toneDocumentTitle,
                                                                                              String contentDocumentTitle) {
        return ex -> {
            String errorMessage = "Failed to transform toneFile '%s' and contentFile '%s'".formatted(toneDocumentTitle,
                    contentDocumentTitle);
            log.error(errorMessage);
            // TODO return different errors and messages based on exception
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", errorMessage,
                    "error", ex.getMessage()
            ));
        };
    }

    private Function<XWPFDocument, ResponseEntity<Map<String, String>>> logAndReturnSuccess(String toneDocumentTitle,
                                                                                            String contentDocumentTitle) {
        return transformedDocument -> {
            log.info("Successfully transformed toneFile '{}' and contentFile '{}' for tone transformation", toneDocumentTitle,
                    contentDocumentTitle);
            return ResponseEntity.ok(Map.of(
                    "message", "Files successfully uploaded, processing completed"
            ));
        };
    }

    public static XWPFDocument saveDocument(XWPFDocument document, String filePath) {
        try (var outputStream = Files.newOutputStream(Paths.get(filePath))) {
            document.write(outputStream);
            return document;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String titleOf(XWPFDocument toneDocument) {
        // TODO could be nulls here?
        return toneDocument.getProperties().getCoreProperties().getTitle();
    }
}
