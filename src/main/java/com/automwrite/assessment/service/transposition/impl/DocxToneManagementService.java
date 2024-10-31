package com.automwrite.assessment.service.transposition.impl;

import com.automwrite.assessment.service.llm.*;
import com.automwrite.assessment.service.transposition.*;
import com.automwrite.assessment.utils.*;
import lombok.experimental.*;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

@Service
@ExtensionMethod({CollectionUtils.class})
public class DocxToneManagementService implements ToneManagementService<XWPFDocument> {

    private final LlmService llmService;

    /* Controls the amount of text we include in the prompt, to help transformations of smaller ambiguous blocks of text lacking context*/
    private final int contextWindow = 6;

    public DocxToneManagementService(LlmService llmService) {
        this.llmService = llmService;
    }

    private static final String EXTRACT_TONE_PROMPT_TEMPLATE = """
        Respond only with one of the following words [%s], that best describes the tone of the text\s
        that follows and nothing else: '%s'
   \s""";



//    Rewrite the given paragraph in the specified stylistic tone if needed and return only the revised paragraph.
//    Tone: '%s'
//    before: '%s'
//    after: '%s'
//    paragraph: '%s'
//
//    Strict Restrictions:
//            - Important! Use the before and after fields only to understand the context of the paragraph. The paragraph section should follow the context of the before paragraph.
//         - Preserve structured information: Leave specific items like addresses, dates, numerical data, and proper names unchanged.
//         - Respect tone and context. If the context of the input is unclear, or if the tone already fits the requested style, do not make changes.
//         - The rewritten text should closely match the input in length.
//            - If the output significantly differs in length or tone, return the original text.
//         - Provide only the revised text without explanations or comments.
//         - If no text is provided, return a single space character.

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

//            1. Preserve structured information: Leave specific items like addresses, dates, numerical data, and proper names unchanged.
//        2. Respect tone and context. If the context of the input is unclear, or if the tone already fits the requested style, do not make changes.
//        3. The rewritten text should closely match the input in length.
//            4. If the output significantly differs in length or tone, return the original text.
//        5. Provide only the revised text without explanations or comments.
//        6. If no readable text is in the brackets, return a single space character.

    @Override
    public StylisticTone extractTone(XWPFDocument file) throws Exception {
        StringBuilder paragraphs = new StringBuilder();

        file.getParagraphs().forEach(paragraph -> {
            paragraphs.append(paragraph.getText()).append("\n");
        });

        String prompt = String.format(EXTRACT_TONE_PROMPT_TEMPLATE, StylisticTone.toCommaSeparatedString(), paragraphs);


        String textResult = llmService.generateText(prompt);

        return StylisticTone.fromString(textResult);
    }

    @Override
    public XWPFDocument applyTone(XWPFDocument file, StylisticTone tone) throws Exception {
        /* Clone the existing file */
        XWPFDocument toneShiftedFile = new XWPFDocument(file.getPackage());

        List<XWPFParagraph> originalParagraphs = file.getParagraphs().stream().filter( paragraph -> !paragraph.getText().isEmpty()).toList();
        List<XWPFParagraph> toneShiftedParagraphs = toneShiftedFile.getParagraphs().stream().filter( paragraph -> !paragraph.getText().isEmpty()).toList();
        System.out.println("Original Paragraphs: " + originalParagraphs.size() + " ToneShifted Paragraphs: " + toneShiftedParagraphs.size());
        int paragraphCount = originalParagraphs.size();

        /* Iterate through each paragraph and modify the tone of the contents */
//        List<CompletableFuture<String>> paragraphContentCompletableFutures = paragraphs.stream()
//                .map(paragraph -> CompletableFuture.supplyAsync(() -> rewriteParagraph(paragraph, tone)))
//                .toList();

//        List<CompletableFuture<String>> paragraphContentCompletableFutures = IntStream.range(0, paragraphCount)
//            .mapToObj(i -> {
//                XWPFParagraph previousParagraph = (i > 0) ? originalParagraphs.get(i - 1) : null;
//                XWPFParagraph currentParagraph = toneShiftedParagraphs.get(i);
//                XWPFParagraph nextParagraph = (i < paragraphCount - 1) ? originalParagraphs.get(i + 1) : null;
//
//                return CompletableFuture.supplyAsync(() -> rewriteParagraph(currentParagraph, tone, previousParagraph, nextParagraph));
//            }).toList();

        List<CompletableFuture<String>> paragraphContentCompletableFutures = IntStream.range(0, paragraphCount)
                .mapToObj(i -> {
                    XWPFParagraph currentParagraph = toneShiftedParagraphs.get(i);

                    // Get a list of the previous n paragraphs, if available
                    List<XWPFParagraph> previousParagraphs = i == 0 ? Collections.emptyList()
                            : originalParagraphs.subList(Math.max(0, i - contextWindow), i);

                    // Get a list of the next n paragraphs, if available
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

        /* Replace the paragraphs of the new (cloned) File with the tone adjusted content */
        toneShiftedParagraphs.forEachIndexed((index, paragraph) -> {
            String toneShiftedContent = paragraphContents.get(index);
            XWPFUtils utils = new XWPFUtils();
            utils.replaceParagraphText(paragraph, toneShiftedContent);
        });

        return toneShiftedFile;
    }

    private String rewriteParagraph(XWPFParagraph paragraph, StylisticTone tone) {
        return rewriteParagraph(paragraph, tone, "", "");
    }

    private String rewriteParagraph(XWPFParagraph paragraph, StylisticTone tone, List<XWPFParagraph> before, List<XWPFParagraph> after) {
        String beforeText = before.stream()
                .map(XWPFParagraph::getText)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.joining(" "));

        String afterText = after.stream()
                .map(XWPFParagraph::getText)
                .filter(text -> !text.isEmpty())
                .collect(Collectors.joining(" "));

        return rewriteParagraph(paragraph, tone, beforeText, afterText);
    }

    private String rewriteParagraph(XWPFParagraph paragraph, StylisticTone tone, XWPFParagraph before, XWPFParagraph after) {
        String previousParagraphText = "";
        String nextParagraphText = "";

        if (before != null) {
            previousParagraphText = before.getText();
        }

        if (after != null) {
            nextParagraphText = after.getText();
        }

        return rewriteParagraph(paragraph, tone, previousParagraphText, nextParagraphText);
    }

    private String rewriteParagraph(XWPFParagraph paragraph, StylisticTone tone, String before, String after) {
        String prompt = String.format(APPLY_TONE_PROMPT_TEMPLATE, tone, before, paragraph.getText(), after);
        return llmService.generateText(prompt);
    }

//    private XWPFParagraph replaceParagraphText(XWPFParagraph paragraph, String content) {
//        List<XWPFRun> runs = paragraph.getRuns();
//
//        for (int i = runs.size() - 1; i >= 0; i--) {
//            paragraph.removeRun(i);
//        }
//
//        XWPFRun newRun = paragraph.createRun();
//        newRun.setText(content, 0);
//        paragraph.addRun(newRun);
//
//        return paragraph;
//    }

}
