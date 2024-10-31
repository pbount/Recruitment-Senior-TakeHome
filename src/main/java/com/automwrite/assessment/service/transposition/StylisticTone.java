package com.automwrite.assessment.service.transposition;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum StylisticTone {
    CASUAL,
    FORMAL,
    GRANDILOQUENT;

    public static String toCommaSeparatedString() {
        return Arrays.stream(StylisticTone.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    public static StylisticTone fromString(String toneString) {
        if (toneString == null) {
            throw new IllegalArgumentException("Tone string cannot be null");
        }
        try {
            return StylisticTone.valueOf(toneString.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tone: '" + toneString + "'. None of the values: '" + toCommaSeparatedString() + "' were matched");
        }
    }
}