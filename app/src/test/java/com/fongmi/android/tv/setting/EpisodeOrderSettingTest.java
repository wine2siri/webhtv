package com.fongmi.android.tv.setting;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EpisodeOrderSettingTest {

    @Test
    public void sameTitleAcrossSourcesUsesSameIdentity() {
        assertEquals(EpisodeOrderSetting.normalize("仙逆"), EpisodeOrderSetting.normalize(" 仙逆 "));
        assertEquals(EpisodeOrderSetting.normalize("凡人修仙传"), EpisodeOrderSetting.normalize("凡人·修仙传"));
    }
}
