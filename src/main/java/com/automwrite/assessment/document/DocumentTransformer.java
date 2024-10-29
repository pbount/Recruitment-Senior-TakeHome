package com.automwrite.assessment.document;

import lombok.AllArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@AllArgsConstructor
public class DocumentTransformer {

    private static final String TRANSFORM_TONE_PROMPT = "Transform the tone of following text to %s. Respond only with " +
            "the transformed text. The text to be transformed: %s";

    private XWPFDocument document;
    private String tone;

    public static DocumentTransformer of(XWPFDocument document, String tone) {
        return new DocumentTransformer(document, tone);
    }

    public CompletableFuture<XWPFDocument> transform(Function<String, CompletableFuture<String>> transformer) {
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                document.getParagraphs().stream()
                        .flatMap(paragraph -> paragraph.getRuns().stream())
                        .map(run -> {
                            if (run.getText(0) == null) {
                                return CompletableFuture.completedFuture(null);
                            } else {
                                // TODO transformed text is set to pos 0
                                return transformer.apply(TRANSFORM_TONE_PROMPT.formatted(tone, run.getText(0)))
                                        .thenAccept(transformedText -> run.setText(transformedText, 0));
                            }
                        }).toArray(CompletableFuture[]::new)
        );

        return allFutures.thenApply(v -> document);
    }
}
