package com.automwrite.assessment.tone.impl;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import com.automwrite.assessment.document.DocumentTransformer;
import com.automwrite.assessment.llm.LlmService;
import com.automwrite.assessment.tone.ToneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToneServiceImpl implements ToneService {

    private static final String IDENTIFY_TONE_PROMPT = "Identify the the tone of the following text and " +
                                                       "respond only with the tone in a single word: %s";

    private static final String PROMPT_HACK = "Identify the tone of the following text, choose one from Casual, Formal " +
                                              "and Grandiloquent, only respond with the tone. Text: %s";

    private final LlmService llmService;;

    @Override
    public CompletableFuture<XWPFDocument> transformTone(XWPFDocument toneFile, XWPFDocument contentFile) {
        Objects.requireNonNull(toneFile, "toneFile must not be null");
        Objects.requireNonNull(contentFile, "contentFile must not be null");

        String toneFileText = getText(toneFile);

        return identifyToneOfText(toneFileText)
                .thenCompose(transformToneOf(contentFile));
    }

    private Function<String, CompletionStage<XWPFDocument>> transformToneOf(XWPFDocument document) {
        return tone -> {
            log.info("Identified target tone of transformation is '{}'.", tone);
            log.debug("Transforming tone of text:\n{}", getText(document));

            return DocumentTransformer.of(document, tone)
                    .transform(llmService::generateTextAsync);
        };
    }

    private CompletableFuture<String> identifyToneOfText(String text) {
        log.debug("Identifying tone of text:\n{}", text);
        return llmService.generateTextAsync(PROMPT_HACK.formatted(text));
    }

    private static String getText (XWPFDocument document) {
        XWPFWordExtractor extractor = new XWPFWordExtractor(document);
        return extractor.getText();
    }
}
