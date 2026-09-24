package com.fongmi.android.tv.drive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DriveCookieSyncTest {

    @Test
    public void derivesHubOriginFromConfigUrl() {
        assertEquals("http://192.168.0.1:8899", DriveCookieSync.hubBase("http://192.168.0.1:8899/api/config/elite"));
        assertEquals("https://media.example.com", DriveCookieSync.hubBase("https://media.example.com/config.json"));
        assertEquals("", DriveCookieSync.hubBase("file:///tmp/config.json"));
    }

    @Test
    public void loginNavigationStaysOnProviderHttpsDomain() {
        assertTrue(DriveCookieSync.isAllowedLoginUrl("quark", "https://passport.quark.cn/login"));
        assertTrue(DriveCookieSync.isAllowedLoginUrl("baidu", "https://wappass.baidu.com/passport"));
        assertFalse(DriveCookieSync.isAllowedLoginUrl("uc", "http://drive.uc.cn/"));
        assertFalse(DriveCookieSync.isAllowedLoginUrl("uc", "https://uc.cn.evil.example/"));
    }

    @Test
    public void mergesCookiesByNameWithoutDuplicatingOldValue() {
        assertEquals("a=2; b=3", DriveCookieSync.mergeCookies("a=1", "a=2; b=3"));
    }
}
