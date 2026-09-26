package com.fongmi.android.tv.player.autoskip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.MediaEdition;

import org.junit.Test;

import java.util.List;

public class AutoSkipChapterPolicyTest {

    private static final long DURATION_MS = 24 * 60_000L;

    @Test
    public void namedOpeningAndEndingCreatePromptOnlyCandidate() {
        List<MediaEdition> chapters = List.of(
                chapter(0, 0, "片头曲"),
                chapter(1, 92_000, "正片"),
                chapter(2, 22 * 60_000, "Ending"));

        AutoSkipChapterPolicy.Proposal result = AutoSkipChapterPolicy.detect(chapters, DURATION_MS, true, true);

        assertEquals(92_000, result.getOpeningMs());
        assertEquals(2 * 60_000, result.getEndingMs());
        assertTrue(!result.getFingerprint().isEmpty());
    }

    @Test
    public void manualSideIsNeverProposed() {
        List<MediaEdition> chapters = List.of(
                chapter(0, 0, "OP"),
                chapter(1, 90_000, "Episode"),
                chapter(2, 22 * 60_000, "ED"));

        AutoSkipChapterPolicy.Proposal result = AutoSkipChapterPolicy.detect(chapters, DURATION_MS, false, true);

        assertEquals(0, result.getOpeningMs());
        assertEquals(2 * 60_000, result.getEndingMs());
    }

    @Test
    public void genericOrOutOfWindowChaptersAreIgnored() {
        List<MediaEdition> chapters = List.of(
                chapter(0, 0, "Chapter 1"),
                chapter(1, 12 * 60_000, "Opening"),
                chapter(2, 23 * 60_000 + 55_000, "Credits"));

        assertTrue(AutoSkipChapterPolicy.detect(chapters, DURATION_MS, true, true).isEmpty());
    }

    private static MediaEdition chapter(int index, long positionMs, String label) {
        return MediaEdition.edition(index, positionMs * 1000L, label, index == 0);
    }
}
