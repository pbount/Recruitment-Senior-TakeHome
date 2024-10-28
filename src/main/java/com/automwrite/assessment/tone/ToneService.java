package com.automwrite.assessment.tone;

import java.util.concurrent.CompletableFuture;

import org.apache.poi.xwpf.usermodel.XWPFDocument;

public interface ToneService {
    CompletableFuture<XWPFDocument> transformTone(XWPFDocument toneFile, XWPFDocument contentFile);
}
