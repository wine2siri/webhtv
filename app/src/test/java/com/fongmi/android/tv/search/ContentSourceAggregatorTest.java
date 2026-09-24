package com.fongmi.android.tv.search;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContentSourceAggregatorTest {

    @Test
    public void normalizesUnicodeWhitespaceAndPunctuation() {
        assertTrue(ContentSourceAggregator.sameContent("关索岭", "2025", "电视剧", "关索岭！", "2025", "TV Series"));
        assertTrue(ContentSourceAggregator.sameContent("A B-C", "", "", "ａｂｃ", "2025", ""));
    }

    @Test
    public void rejectsDifferentKnownYears() {
        assertFalse(ContentSourceAggregator.sameContent("重生", "2024", "电影", "重生", "2025", "电影"));
    }

    @Test
    public void rejectsKnownMovieSeriesConflict() {
        assertFalse(ContentSourceAggregator.sameContent("三体", "2024", "电影", "三体", "2024", "电视剧"));
    }

    @Test
    public void treatsMovieGenreEndingInPianAsMovie() {
        assertTrue(ContentSourceAggregator.sameContent("关索岭", "2025", "剧情片", "关索岭", "2025", "电影"));
    }
}
