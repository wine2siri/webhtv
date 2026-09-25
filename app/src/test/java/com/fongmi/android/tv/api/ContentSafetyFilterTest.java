package com.fongmi.android.tv.api;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ContentSafetyFilterTest {

    @Test
    public void blocksAdultLowAgeAndLgbtMetadata() {
        assertTrue(ContentSafetyFilter.isBlocked("成人情色电影"));
        assertTrue(ContentSafetyFilter.isBlocked("普通标题", "少儿", "更新中"));
        assertTrue(ContentSafetyFilter.isBlocked("普通标题", "剧情", "LGBTQ 主题"));
    }

    @Test
    public void keepsOrdinaryFilmAndSeriesCards() {
        assertFalse(ContentSafetyFilter.isBlocked("流浪地球", "科幻", "太阳危机下的冒险"));
        assertFalse(ContentSafetyFilter.isBlocked("繁花", "剧情", "沪上往事"));
    }
}
