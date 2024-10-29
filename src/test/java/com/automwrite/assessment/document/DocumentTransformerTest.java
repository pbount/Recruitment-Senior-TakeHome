package com.automwrite.assessment.document;

import com.automwrite.assessment.Application;
import com.automwrite.assessment.llm.LlmService;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {Application.class})
class DocumentTransformerTest {

    @Autowired
    private LlmService llmService;

    private XWPFDocument document;

    @BeforeEach
    void setUp() throws IOException {
        try (FileInputStream fis = new FileInputStream("different tones/automwrite - A - Casual tone.docx")) {
            document = new XWPFDocument(fis);
        }
    }

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Test
    void testPlayfulToneTransformation() {
        assertThat(apiKey).isNotEmpty();
        String tone = "playful";
        XWPFDocument transformedDocument = DocumentTransformer.of(document, tone)
                .transform(llmService::generateTextAsync)
                .join();
         String doesItMatchTone = llmService.generateText("Is the following text %s, respond only with yes or no: %s".formatted(tone,
                 new XWPFWordExtractor(transformedDocument).getText()));
         assertThat(doesItMatchTone).isEqualToIgnoringCase("yes");
        try (var outputStream = Files.newOutputStream(Paths.get("different tones/testresult.docx"))) {
            transformedDocument.write(outputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}