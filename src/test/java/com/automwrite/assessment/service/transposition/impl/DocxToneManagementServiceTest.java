package com.automwrite.assessment.service.transposition.impl;

import com.automwrite.assessment.service.llm.LlmService;
import com.automwrite.assessment.service.transposition.StylisticTone;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
    @DisplayName("Test that the llm service will be called for each paragraph")
    public void testApplyToneForASingleValidDocument() throws Exception {
        /* Given */
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph para1 = document.createParagraph();
        para1.createRun().setText("This is the first paragraph.");
        XWPFParagraph para2 = document.createParagraph();
        para2.createRun().setText("This is the second paragraph.");

        when(llmService.generateText(anyString())).thenReturn("Transformed paragraph content.");

        /* When */
        XWPFDocument resultDocument = docxToneManagementService.applyTone(document, StylisticTone.FORMAL);

        /* Then */
        verify(llmService, times(2)).generateText(anyString());
        assertEquals("Transformed paragraph content.", resultDocument.getParagraphs().get(0).getText());
        assertEquals("Transformed paragraph content.", resultDocument.getParagraphs().get(1).getText());
    }

    @Test
    @DisplayName("Test that Styling is retained in the output file")
    public void testThatStyleIsRetainedForTheOutputFile() throws Exception {
        /* Given */
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setItalic(true);
        run.setText("Styled paragraph.");

        when(llmService.generateText(anyString())).thenReturn("Transformed content.");

        /* When */
        XWPFDocument resultDocument = docxToneManagementService.applyTone(document, StylisticTone.FORMAL);

        /* Then */
        XWPFRun resultRun = resultDocument.getParagraphs().get(0).getRuns().get(0);
        assertTrue(resultRun.isBold());
        assertTrue(resultRun.isItalic());
        assertEquals("Transformed content.", resultRun.getText(0));
    }

    @Test
    @DisplayName("Test that given an empty document the llm service is not called and the output is also an empty document")
    public void testThatEmptyDocumentProducesEmptyDocument() throws Exception {
        /* Given */
        XWPFDocument document = new XWPFDocument();

        /* When */
        XWPFDocument resultDocument = docxToneManagementService.applyTone(document, StylisticTone.FORMAL);

        /* Then */
        verify(llmService, never()).generateText(anyString());
        assertEquals(0, resultDocument.getParagraphs().size());
    }

    @Test
    @DisplayName("Test that given a document with empty paragraphs, the empty paragraphs are ignored")
    public void testEmptyParagraphsIgnored() throws Exception {
        /* Given */
        XWPFDocument document = new XWPFDocument();
        document.createParagraph();
        XWPFParagraph para = document.createParagraph();
        para.createRun().setText("Non-empty paragraph.");
        document.createParagraph();

        when(llmService.generateText(anyString())).thenReturn("Transformed content.");

        /* When */
        XWPFDocument resultDocument = docxToneManagementService.applyTone(document, StylisticTone.FORMAL);

        /* Then */
        verify(llmService, times(1)).generateText(anyString());
        assertEquals("", resultDocument.getParagraphs().get(0).getText());
        assertEquals("Transformed content.", resultDocument.getParagraphs().get(1).getText());
        assertEquals("", resultDocument.getParagraphs().get(2).getText());
    }

    @Test
    @DisplayName("Test that document with a large number of paragraphs is processed")
    public void testApplyTone_LargeDocument() throws Exception {
        /* Given */
        XWPFDocument document = new XWPFDocument();
        for (int i = 0; i < 1000; i++) {
            XWPFParagraph para = document.createParagraph();
            para.createRun().setText("Paragraph number " + i);
        }

        when(llmService.generateText(anyString())).thenReturn("Transformed content.");

        /* When */
        XWPFDocument resultDocument = docxToneManagementService.applyTone(document, StylisticTone.FORMAL);

        /* Then */
        verify(llmService, times(1000)).generateText(anyString());
        assertEquals("Transformed content.", resultDocument.getParagraphs().get(0).getText());
        assertEquals("Transformed content.", resultDocument.getParagraphs().get(999).getText());
    }

    @Test
    @DisplayName("Test that all calls are made concurrently")
    public void testApplyTone_AsynchronousProcessing() throws Exception {
        /* Given */
        XWPFDocument document = new XWPFDocument();
        for (int i = 0; i < 10; i++) {
            XWPFParagraph para = document.createParagraph();
            para.createRun().setText("Paragraph " + i);
        }

        when(llmService.generateText(anyString())).thenAnswer(invocation -> {
            Thread.sleep(100);
            return "Transformed content.";
        });

        /* When */
        long startTime = System.currentTimeMillis();
        XWPFDocument resultDocument = docxToneManagementService.applyTone(document, StylisticTone.FORMAL);
        long duration = System.currentTimeMillis() - startTime;

        /* Then */
        assertTrue(duration < 1000);
        for (XWPFParagraph para : resultDocument.getParagraphs()) {
            assertEquals("Transformed content.", para.getText());
        }
    }

    @Test
    @DisplayName("Test context window useage")
    public void testApplyTone_ContextWindowUsage() throws Exception {
        /* Given */
        XWPFDocument document = new XWPFDocument();
        for (int i = 0; i < 5; i++) {
            XWPFParagraph para = document.createParagraph();
            para.createRun().setText("Paragraph " + i);
        }

        when(llmService.generateText(anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            assertTrue(prompt.contains("Paragraph"));
            return "Transformed content.";
        });

        /* When */
        XWPFDocument resultDocument = docxToneManagementService.applyTone(document, StylisticTone.FORMAL);

        /* Then */
        for (XWPFParagraph para : resultDocument.getParagraphs()) {
            assertEquals("Transformed content.", para.getText());
        }
    }
    // endregion
}
