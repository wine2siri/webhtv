package com.fongmi.android.tv.playback;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NavigationSourceSelectionPolicyTest {

    @Test
    public void injoyNavigationPrefersBackendRankedFirstSource() {
        assertTrue(NavigationSourceSelectionPolicy.preferRankedFirst(
                "影卓追剧-nav14",
                "http://192.168.0.1:8899/EliteMedia/py/douban_nav.py?v=20260928-nav16"));
    }

    @Test
    public void renamedNavigationStillMatchesBySpiderApi() {
        assertTrue(NavigationSourceSelectionPolicy.preferRankedFirst(
                "custom-nav",
                "https://example.test/py/douban_nav.py?v=17"));
    }

    @Test
    public void standaloneEmbyKeepsItsHistorySelection() {
        assertFalse(NavigationSourceSelectionPolicy.preferRankedFirst(
                "Shared_Emby_啤梨",
                "http://192.168.0.1:8899/EliteMedia/py/emby_proxy.py"));
    }
}
