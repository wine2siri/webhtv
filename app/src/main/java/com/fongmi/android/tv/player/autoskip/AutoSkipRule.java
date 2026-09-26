package com.fongmi.android.tv.player.autoskip;

public class AutoSkipRule {

    public static final AutoSkipRule EMPTY = new AutoSkipRule(0, 0);

    private final long openingMs;
    private final long endingMs;

    public AutoSkipRule(long openingMs, long endingMs) {
        this.openingMs = Math.max(0, openingMs);
        this.endingMs = Math.max(0, endingMs);
    }

    public long getOpeningMs() {
        return openingMs;
    }

    public long getEndingMs() {
        return endingMs;
    }

    public boolean isEmpty() {
        return openingMs <= 0 && endingMs <= 0;
    }
}
