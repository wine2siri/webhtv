package com.fongmi.android.tv.api.loader;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderNull;

import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PyLoaderTest {

    @Test
    public void transientFailureDoesNotPoisonCache() {
        ConcurrentHashMap<String, Spider> spiders = new ConcurrentHashMap<>();
        AtomicInteger attempts = new AtomicInteger();
        Spider expected = new Spider() {};

        Spider failed = PyLoader.getOrCreate(spiders, "nav", () -> {
            if (attempts.incrementAndGet() == 1) throw new InterruptedException("canceled startup request");
            return expected;
        });
        Spider recovered = PyLoader.getOrCreate(spiders, "nav", () -> {
            attempts.incrementAndGet();
            return expected;
        });

        assertTrue(failed instanceof SpiderNull);
        assertSame(expected, recovered);
        assertSame(expected, spiders.get("nav"));
    }
}
