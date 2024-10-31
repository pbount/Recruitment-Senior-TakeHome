package com.automwrite.assessment.utils;

import java.util.List;
import java.util.function.BiConsumer;

public class CollectionUtils {
    public static <T> void forEachIndexed(List<T> list, BiConsumer<Integer, T> action) {
        for (int i = 0; i < list.size(); i++) {
            action.accept(i, list.get(i));
        }
    }
}
