package com.automwrite.assessment.utils;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.util.*;

public class XWPFUtils {

    public void replaceParagraphText(XWPFParagraph paragraph, String text) {
        List<XWPFRun> runs = paragraph.getRuns();

        if (!runs.isEmpty()) {
            runs.get(0).setText(text, 0);

            for (int i = runs.size() - 1; i > 0; i--) {
                paragraph.removeRun(i);
            }
        }
    }

}
