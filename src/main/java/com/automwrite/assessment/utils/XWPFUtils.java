package com.automwrite.assessment.utils;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class XWPFUtils {

    public static void replaceParagraphText(XWPFParagraph paragraph, String text) {
        List<XWPFRun> runs = paragraph.getRuns();

        if (!runs.isEmpty()) {
            runs.get(0).setText(text, 0);

            for (int i = runs.size() - 1; i > 0; i--) {
                paragraph.removeRun(i);
            }
        }
    }

    public static XWPFDocument clone(XWPFDocument document) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.write(outputStream);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

        return new XWPFDocument(inputStream);
    }

}
