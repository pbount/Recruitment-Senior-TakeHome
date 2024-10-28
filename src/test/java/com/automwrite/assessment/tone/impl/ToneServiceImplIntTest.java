package com.automwrite.assessment.tone.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.automwrite.assessment.Application;
import com.automwrite.assessment.llm.LlmService;
import com.automwrite.assessment.tone.ToneService;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {Application.class})
class ToneServiceImplIntTest {

    private static final String DIFFERENT_TONES_DIRECTORY = "different tones/";

    private static final String CASUAL_TONE_DOCX_FILE_NAME = DIFFERENT_TONES_DIRECTORY + "automwrite - A - Casual tone.docx";
    private static final String FORMAL_TONE_DOCX_FILE_NAME = DIFFERENT_TONES_DIRECTORY + "automwrite - B - Formal tone.docx";
    private static final String GRANDILOQUENT_TONE_FILE_NAME = DIFFERENT_TONES_DIRECTORY + "automwrite - C - Grandiloquent tone.docx";
    private static final File CASUAL_TONE_DOCX_FILE = new File(CASUAL_TONE_DOCX_FILE_NAME);
    private static final File FORMAL_TONE_FILE = new File(FORMAL_TONE_DOCX_FILE_NAME);
    private static final File GRANDILOQUENT_TONE_FILE = new File(GRANDILOQUENT_TONE_FILE_NAME);
    private static final String CASUAL_TONE = "casual";
    private static final String FORMAL_TONE = "formal";
    private static final String GRANDILOQUENT_TONE = "grandiloquent";
    private static final Logger log = LoggerFactory.getLogger(ToneServiceImplIntTest.class);

    @Autowired
    private ToneService toneService;

    @Autowired
    private LlmService llmService;

    @Value("${anthropic.api.key}")
    private String apiKey;

    static Stream<Arguments> toneTransformationCombinations() {
        return Stream.of(
                Arguments.of(CASUAL_TONE_DOCX_FILE, FORMAL_TONE_FILE)
                // Disabled these tests not to overload the LLM API.
                /*Arguments.of(CASUAL_TONE_DOCX_FILE, CASUAL_TONE_DOCX_FILE),
                Arguments.of(CASUAL_TONE_DOCX_FILE, GRANDILOQUENT_TONE_FILE),
                Arguments.of(FORMAL_TONE_FILE, FORMAL_TONE_FILE),
                Arguments.of(FORMAL_TONE_FILE, GRANDILOQUENT_TONE_FILE),
                Arguments.of(FORMAL_TONE_FILE, CASUAL_TONE_DOCX_FILE),
                Arguments.of(GRANDILOQUENT_TONE_FILE, GRANDILOQUENT_TONE_FILE),
                Arguments.of(GRANDILOQUENT_TONE_FILE, CASUAL_TONE_DOCX_FILE),
                Arguments.of(GRANDILOQUENT_TONE_FILE, FORMAL_TONE_FILE)*/
        );
    }

    @ParameterizedTest
    @MethodSource("toneTransformationCombinations")
    void transformationSucceeds(File toneFile, File targetFile) throws IOException {
        assertThat(apiKey).isNotEmpty();
        CompletableFuture<XWPFDocument> futureResult = toneService.transformTone(parseDocx(toneFile),
                parseDocx(targetFile));
        XWPFDocument transformedDocument = futureResult.join();
        String transformedText = new XWPFWordExtractor(transformedDocument).getText();
        llmService.generateText(("Identify if the tone of the following text is %s, " +
                                 "only respond with yes or no. Text: %s").formatted(targetTone(toneFile), transformedText));
        assertThat(transformedText).isEqualTo("yes");
    }

    private String targetTone(File toneFile) {
        return switch (toneFile.getName()) {
            case CASUAL_TONE_DOCX_FILE_NAME -> CASUAL_TONE;
            case FORMAL_TONE_DOCX_FILE_NAME -> FORMAL_TONE;
            case GRANDILOQUENT_TONE_FILE_NAME -> GRANDILOQUENT_TONE;
            default -> throw new IllegalArgumentException("Unknown tone file: " + toneFile.getName());
        };
    }

    private XWPFDocument parseDocx(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        return new XWPFDocument(fis);
    }
}