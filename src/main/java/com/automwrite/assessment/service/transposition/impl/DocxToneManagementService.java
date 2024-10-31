package com.automwrite.assessment.service.transposition.impl;

import com.automwrite.assessment.service.llm.LlmService;
import com.automwrite.assessment.service.transposition.ToneManagementService;
import com.automwrite.assessment.service.transposition.StylisticTone;
import com.automwrite.assessment.utils.XWPFUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

@Service
public class DocxToneManagementService implements ToneManagementService<XWPFDocument> {

    private final LlmService llmService;

    /* Controls the amount of text we include in the prompt, to help transformations of smaller ambiguous blocks of text lacking context */
    private final int contextWindow = 15;

    public DocxToneManagementService(LlmService llmService) {
        this.llmService = llmService;
    }

    private static final String TONE_EXTRACTION_PROMPT_TEMPLATE = """
        Respond only with one of the following words [%s], that best describes the tone of the text\s
        that follows and nothing else: '%s'
   \s""";

    private static final String APPLY_TONE_PROMPT_TEMPLATE = """
        This is a STRICT text transformation task, not a conversational task. Follow the instructions exactly.

        Rewrite ONLY the text between [brackets] in the specified tone. Use the surrounding text to understand the context but don't use it in the output.

        Tone: %s
        Text: %s [%s] %s

        STRICT OUTPUT RULES:
        1. Output first character must be first character replacing bracketed content
        2. Output last character must be last character replacing bracketed content
        3. Only modify the text if its tone significantly differs from the requested tone; otherwise, leave it unchanged.
        4. No greetings, context, or other text permitted
        5. No explanations
        6. Direct replacement only
        7. No Guesswork. If context is unclear, return the original text.
        8. If no text present in brackets, return a single space character.
        9. All abbreviations must remain EXACTLY as they appear. Never expand abbreviations.

        Example input: "This is a test. [The test is hard]. The test has concluded."
        Example output: The test is difficult

        FAILURE CONDITIONS:
        - Any output starting before bracket content
        - Any output continuing after bracket content
        - Any explanatory text
        - Transforming ambiguous text
   \s""";

    /**
     * Extracts the stylistic tone from the provided Word document by analyzing its content.
     *
     * <p>This method reads all paragraphs from the given {@link XWPFDocument}, concatenates their text,
     * and constructs a prompt based on a predefined template. It then uses the LLM service to generate
     * a textual representation of the document's stylistic tone. The resulting text is parsed to create
     * a {@link StylisticTone} object, which represents the extracted tone.
     *
     * @param file the Word document from which to extract the tone
     * @return a {@link StylisticTone} representing the extracted tone of the document
     * @throws Exception if an error occurs during processing
     */
    @Override
    public StylisticTone extractTone(XWPFDocument file) throws Exception {
        StringBuilder paragraphs = new StringBuilder();

        file.getParagraphs().forEach(paragraph -> {
            paragraphs.append(paragraph.getText()).append("\n");
        });

        String prompt = String.format(TONE_EXTRACTION_PROMPT_TEMPLATE, StylisticTone.toCommaSeparatedString(), paragraphs);

        String textResult = llmService.generateText(prompt);

        return StylisticTone.fromString(textResult);
    }

    /**
     * Applies the specified stylistic tone to the provided Word document by rewriting its content.
     *
     * <p>This method clones the given {@link XWPFDocument} to retain the original styling. It filters out
     * paragraphs that contain no text and processes each paragraph asynchronously. For each paragraph,
     * it rewrites the content to match the specified {@link StylisticTone}, using a context window of
     * surrounding paragraphs for better coherence. After all paragraphs have been processed, it replaces
     * the text in the cloned document with the tone-adjusted content and returns the modified document.
     *
     * @param file the Word document to which the tone will be applied
     * @param tone the {@link StylisticTone} to apply to the document
     * @return a new {@link XWPFDocument} with the content adjusted to the specified tone
     * @throws Exception if an error occurs during processing
     */
    @Override
    public XWPFDocument applyTone(XWPFDocument file, StylisticTone tone) throws Exception {
        /* Clone the existing file in order to retain styling */
        XWPFDocument toneShiftedFile = XWPFUtils.clone(file);

        /* Filter out all paragraphs that don't contain text */
        List<XWPFParagraph> originalParagraphs = file.getParagraphs().stream()
                .filter( paragraph -> !paragraph.getText().isEmpty()).toList();

        List<XWPFParagraph> toneShiftedParagraphs = toneShiftedFile.getParagraphs().stream()
                .filter( paragraph -> !paragraph.getText().isEmpty()).toList();

        int paragraphCount = originalParagraphs.size();

        List<CompletableFuture<String>> paragraphContentCompletableFutures = IntStream.range(0, paragraphCount)
                .mapToObj(i -> {
                    /* Select the Current paragraph and build a window of context around it */
                    XWPFParagraph currentParagraph = toneShiftedParagraphs.get(i);

                    /* Get a list of previous paragraphs if available - for context */
                    List<XWPFParagraph> previousParagraphs = i == 0 ? Collections.emptyList()
                            : originalParagraphs.subList(Math.max(0, i - contextWindow), i);

                    /* Get a list of next paragraphs if available - for context */
                    List<XWPFParagraph> nextParagraphs = i == paragraphCount - 1 ? Collections.emptyList()
                            : originalParagraphs.subList(i + 1, Math.min(paragraphCount, i + contextWindow + 1));

                    return CompletableFuture.supplyAsync(() -> rewriteParagraph(currentParagraph, tone, previousParagraphs, nextParagraphs));
                }).toList();

        /* Await for completion */
        CompletableFuture.allOf(paragraphContentCompletableFutures.toArray(new CompletableFuture[0])).join();

        /* Join Completed results */
        List<String> paragraphContents =  paragraphContentCompletableFutures.stream()
            .map(CompletableFuture::join)
            .toList();

        /* Replace the paragraphs of the new (cloned) document with the tone adjusted content */
        IntStream.range(0, toneShiftedParagraphs.size()).forEach(i -> {
            String toneShiftedContent = paragraphContents.get(i);
            XWPFParagraph paragraph = toneShiftedParagraphs.get(i);
            XWPFUtils.replaceParagraphText(paragraph, toneShiftedContent);
        });

        return toneShiftedFile;
    }

    private String rewriteParagraph(XWPFParagraph paragraph, StylisticTone tone, List<XWPFParagraph> before, List<XWPFParagraph> after) {
        String beforeText = before.stream()
                .map(XWPFParagraph::getText)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.joining("\n"));

        String afterText = after.stream()
                .map(XWPFParagraph::getText)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.joining("\n"));

        return rewriteParagraph(paragraph, tone, beforeText, afterText);
    }

    private String rewriteParagraph(XWPFParagraph paragraph, StylisticTone tone, String before, String after) {
        String prompt = String.format(APPLY_TONE_PROMPT_TEMPLATE, tone, before, paragraph.getText(), after);
        System.out.println(prompt);
        return llmService.generateText(prompt);
    }

}
