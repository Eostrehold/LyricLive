package com.eostrehold.lyriclive.client.lrc;

/**
 * 表示单行歌词，包含时间戳和歌词文本。
 */
public class LrcLyric {
    private final long timestamp; // 时间戳，单位：毫秒
    private final String text;    // 歌词文本

    public LrcLyric(long timestamp, String text) {
        this.timestamp = timestamp;
        this.text = text;
    }

    /**
     * 获取时间戳（毫秒）
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 获取歌词文本
     */
    public String getText() {
        return text;
    }

    /**
     * 将时间戳格式化为 mm:ss.SSS 格式（用于调试）
     */
    public String formatTimestamp() {
        long totalSeconds = timestamp / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long millis = timestamp % 1000;
        return String.format("%02d:%02d.%03d", minutes, seconds, millis);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", formatTimestamp(), text);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LrcLyric other = (LrcLyric) obj;
        return timestamp == other.timestamp && text.equals(other.text);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(timestamp) * 31 + text.hashCode();
    }
}