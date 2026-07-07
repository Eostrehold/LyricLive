package com.eostrehold.lyriclive.client.lrc;

import com.eostrehold.lyriclive.LyricLive;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LRC 歌词文件解析器。
 * 支持标准 LRC 格式，包括：
 * - 元数据标签：[ti:标题], [ar:艺术家], [al:专辑], [by:作者]
 * - 时间标签：[mm:ss.SSS] 歌词文本
 * - 多个时间标签指向同一行歌词
 */
public class LrcParser {
    // 匹配元数据标签，如 [ti:标题]
    private static final Pattern METADATA_PATTERN = Pattern.compile("\\[([a-z]+):(.+)\\]");
    // 匹配时间标签，如 [01:23.456]
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]");

    /**
     * 解析 LRC 文件
     * @param filePath LRC 文件路径
     * @return 解析后的歌词对象
     * @throws IOException 如果读取文件失败
     */
    public LyricTrack parse(Path filePath) throws IOException {
        LyricLive.LOGGER.info("开始解析 LRC 文件: {}", filePath);

        LyricTrack track = new LyricTrack();

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (line.isEmpty()) {
                    continue;
                }

                try {
                    parseLine(line, track);
                } catch (Exception e) {
                    LyricLive.LOGGER.warn("解析第 {} 行时出错: {} - {}", lineNumber, line, e.getMessage());
                }
            }
        }

        LyricLive.LOGGER.info("LRC 解析完成: {}", track);
        return track;
    }

    /**
     * 解析单行内容
     */
    private void parseLine(String line, LyricTrack track) {
        // 尝试解析元数据标签
        Matcher metadataMatcher = METADATA_PATTERN.matcher(line);
        if (metadataMatcher.matches()) {
            String key = metadataMatcher.group(1).toLowerCase();
            String value = metadataMatcher.group(2).trim();
            processMetadata(key, value, track);
            return;
        }

        // 尝试解析歌词行（可能有多个时间标签）
        // 格式：[00:01.000][00:05.000] 歌词文本
        int lastBracketEnd = line.lastIndexOf(']');
        if (lastBracketEnd < 0) {
            return;
        }

        String text = line.substring(lastBracketEnd + 1).trim();
        if (text.isEmpty()) {
            return;
        }

        // 查找所有时间标签
        String timestampsPart = line.substring(0, lastBracketEnd + 1);
        Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(timestampsPart);

        while (timestampMatcher.find()) {
            long timestamp = parseTimestamp(timestampMatcher);
            track.addLyric(new LrcLyric(timestamp, text));
        }
    }

    /**
     * 处理元数据标签
     */
    private void processMetadata(String key, String value, LyricTrack track) {
        switch (key) {
            case "ti" -> track.setTitle(value);
            case "ar" -> track.setArtist(value);
            case "al" -> track.setAlbum(value);
            case "by" -> track.setAuthor(value);
            default -> LyricLive.LOGGER.debug("忽略未知元数据标签: [{}:{}]", key, value);
        }
    }

    /**
     * 解析时间戳为毫秒
     * @return 毫秒数
     */
    private long parseTimestamp(Matcher matcher) {
        int minutes = Integer.parseInt(matcher.group(1));
        int seconds = Integer.parseInt(matcher.group(2));

        int millis = 0;
        String millisStr = matcher.group(3);
        if (millisStr != null) {
            // 处理不同精度：.1 = 100ms, .12 = 120ms, .123 = 123ms
            if (millisStr.length() == 1) {
                millis = Integer.parseInt(millisStr) * 100;
            } else if (millisStr.length() == 2) {
                millis = Integer.parseInt(millisStr) * 10;
            } else {
                millis = Integer.parseInt(millisStr);
            }
        }

        return (minutes * 60L + seconds) * 1000L + millis;
    }
}