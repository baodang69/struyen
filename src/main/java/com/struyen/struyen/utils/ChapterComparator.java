package com.struyen.struyen.utils;

import com.struyen.struyen.entities.Chapter;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bo so sanh thu tu chuong tu nhien (Natural Chapter Number Comparator).
 * Ho tro sap xep chinh xac cac chuong so thuc, so nguyen va ky tu dac biet
 * (vi du: 1, 1.5, 2, 2.5, 10, 100 thay vi sap xep theo chuoi 1, 10, 100, 2).
 */
public class ChapterComparator implements Comparator<Chapter> {

    public static final ChapterComparator INSTANCE = new ChapterComparator();
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)");

    @Override
    public int compare(Chapter c1, Chapter c2) {
        if (c1 == null && c2 == null) return 0;
        if (c1 == null) return -1;
        if (c2 == null) return 1;

        String name1 = c1.getChapterName();
        String name2 = c2.getChapterName();

        if (name1 == null) name1 = "";
        if (name2 == null) name2 = "";

        Double num1 = extractNumber(name1);
        Double num2 = extractNumber(name2);

        if (num1 != null && num2 != null) {
            int numCompare = Double.compare(num1, num2);
            if (numCompare != 0) {
                return numCompare;
            }
        } else if (num1 != null) {
            return -1;
        } else if (num2 != null) {
            return 1;
        }

        return name1.compareToIgnoreCase(name2);
    }

    public static Double extractNumber(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException ignored) {
            Matcher matcher = NUMBER_PATTERN.matcher(str);
            if (matcher.find()) {
                try {
                    return Double.parseDouble(matcher.group(1));
                } catch (NumberFormatException ignored2) {
                    return null;
                }
            }
            return null;
        }
    }
}

