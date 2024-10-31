package com.automwrite.assessment.service.transposition.impl;

import com.automwrite.assessment.service.llm.LlmService;
import com.automwrite.assessment.service.transposition.StylisticTone;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DocxToneManagementServiceTest {

    @Mock
    private LlmService llmService;

    @InjectMocks
    private DocxToneManagementService docxToneManagementService;

    private XWPFDocument mockDocument;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockDocument = new XWPFDocument();
    }

    // region ToneExtractionTests
    @Test
    @DisplayName("Test that a valid response from the llmService maps to a valid enum option")
    void testExtractToneWhenResponseValid() throws Exception {
        /* Given */
        String validResponse = "CASUAL";
        when(llmService.generateText(anyString())).thenReturn(validResponse);

        /* When */
        StylisticTone tone = docxToneManagementService.extractTone(mockDocument);

        /* Then */
        assertEquals(StylisticTone.CASUAL, tone);
    }

    @Test
    @DisplayName("Test that an invalid response from the llm produces the expected error")
    void testExtractToneWhenResponseInvalid() {
        /* Given */
        String invalidResponse = "INVALID_TONE";
        when(llmService.generateText(anyString())).thenReturn(invalidResponse);
        String expected = "Invalid tone: 'INVALID_TONE'. None of the values: 'CASUAL, FORMAL, GRANDILOQUENT' were matched";

        /* Then */
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            docxToneManagementService.extractTone(mockDocument);
        });
        String actual = exception.getMessage();

        /* Then */
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Test that an empty string from the llm produces the expected error")
    void testExtractToneWhenResponseEmpty() {
        /* Given */
        String emptyResponse = "";
        when(llmService.generateText(anyString())).thenReturn(emptyResponse);
        String expected = "Invalid tone: ''. None of the values: 'CASUAL, FORMAL, GRANDILOQUENT' were matched";

        /* When */
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            docxToneManagementService.extractTone(mockDocument);
        });
        String actual = exception.getMessage();

        /* Then */
        assertEquals(expected, actual);
    }
    // endregion ToneExtractionTests

    // region ApplyToneTests
    @Test
    @DisplayName("Apply tone to a document with multiple paragraphs")
    void testApplyTone() throws Exception {
        // TODO
    }
    // endregion
}
