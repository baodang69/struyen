package com.struyen.struyen.utils;

import com.struyen.struyen.entities.Chapter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChapterComparatorTest {

    @Test
    void shouldSortChaptersInNaturalNumericOrder() {
        List<Chapter> chapters = new ArrayList<>(List.of(
                Chapter.builder().chapterName("2").build(),
                Chapter.builder().chapterName("1").build(),
                Chapter.builder().chapterName("10").build(),
                Chapter.builder().chapterName("1.5").build(),
                Chapter.builder().chapterName("2.5").build(),
                Chapter.builder().chapterName("100").build(),
                Chapter.builder().chapterName("0.01").build()
        ));

        chapters.sort(ChapterComparator.INSTANCE);

        List<String> sortedNames = chapters.stream().map(Chapter::getChapterName).toList();
        assertEquals(List.of("0.01", "1", "1.5", "2", "2.5", "10", "100"), sortedNames);
    }

    @Test
    void shouldHandleNonNumericAndSpecialChapterNames() {
        List<Chapter> chapters = new ArrayList<>(List.of(
                Chapter.builder().chapterName("Extra").build(),
                Chapter.builder().chapterName("Chương 2").build(),
                Chapter.builder().chapterName("Chương 1").build(),
                Chapter.builder().chapterName("One-shot").build()
        ));

        chapters.sort(ChapterComparator.INSTANCE);

        List<String> sortedNames = chapters.stream().map(Chapter::getChapterName).toList();
        assertEquals(List.of("Chương 1", "Chương 2", "Extra", "One-shot"), sortedNames);
    }
}

