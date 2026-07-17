package com.eostrehold.lyriclive.client.util;

public final class LyricUtils {
    private LyricUtils() {}

    /**
     * Format milliseconds as mm:ss
     */
    public static String fmtTime(long ms) {
        if (ms <= 0) return "00:00";
        long sec = ms / 1000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    /**
     * Truncate string with ellipsis if too long
     */
    public static String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
}
