package com.eostrehold.lyriclive.client.core;

import com.eostrehold.lyriclive.LyricLive;
import com.eostrehold.lyriclive.client.lrc.LrcLyric;
import com.eostrehold.lyriclive.client.lrc.LrcParser;
import com.eostrehold.lyriclive.client.lrc.LyricTrack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 时间轴管理器，负责加载歌词轨道并提供当前歌词信息。
 */
public class TimelineManager {
    private final PlaybackController playbackController;
    private final LrcParser lrcParser;
    private LyricTrack currentTrack;
    private int currentLyricIndex = -1;

    public TimelineManager(PlaybackController playbackController) {
        this.playbackController = playbackController;
        this.lrcParser = new LrcParser();
    }

    /**
     * 加载 LRC 文件
     * @param filePath LRC 文件路径
     * @throws IOException 如果读取文件失败
     */
    public void loadLyricFile(Path filePath) throws IOException {
        LyricLive.LOGGER.info("加载歌词文件: {}", filePath);
        this.currentTrack = lrcParser.parse(filePath);
        this.currentLyricIndex = -1;
    }

    /**
     * 获取当前歌词文本
     * @return 当前歌词文本，如果没有则返回 null
     */
    public String getCurrentLyricText() {
        if (currentTrack == null || currentTrack.isEmpty()) {
            return null;
        }

        long currentTime = playbackController.getCurrentTimeMillis();
        int newIndex = currentTrack.getCurrentLyricIndex(currentTime);
        if (newIndex >= 0) {
            LrcLyric lyric = currentTrack.getLyrics().get(newIndex);
            if (newIndex != currentLyricIndex) {
                currentLyricIndex = newIndex;
                LyricLive.LOGGER.debug("歌词切换: {}", lyric.getText());
            }
            return lyric.getText();
        }

        return null;
    }

    /**
     * 获取当前歌词对象
     * @return 当前歌词对象，如果没有则返回 null
     */
    public LrcLyric getCurrentLyric() {
        if (currentTrack == null || currentTrack.isEmpty()) {
            return null;
        }

        long currentTime = playbackController.getCurrentTimeMillis();
        int newIndex = currentTrack.getCurrentLyricIndex(currentTime);
        if (newIndex >= 0) {
            currentLyricIndex = newIndex;
            return currentTrack.getLyrics().get(newIndex);
        }
        return null;
    }

    /**
     * 获取当前歌词上下文行，包含当前行前后指定数量的歌词。
     * @param surroundingLineCount 当前歌词前后各取多少行
     * @return 当前歌词上下文列表
     */
    public List<LrcLyric> getCurrentLyricContext(int surroundingLineCount) {
        if (currentTrack == null || currentTrack.isEmpty()) {
            return List.of();
        }

        long currentTime = playbackController.getCurrentTimeMillis();
        int centerLyricIndex = currentTrack.getCurrentLyricIndex(currentTime);
        if (centerLyricIndex < 0) {
            return List.of();
        }

        currentLyricIndex = centerLyricIndex;

        int firstLyricIndex = Math.max(0, centerLyricIndex - surroundingLineCount);
        int lastLyricIndex = Math.min(currentTrack.getLyricCount() - 1, centerLyricIndex + surroundingLineCount);
        List<LrcLyric> lyricContext = new ArrayList<>();
        for (int lyricIndex = firstLyricIndex; lyricIndex <= lastLyricIndex; lyricIndex++) {
            lyricContext.add(currentTrack.getLyrics().get(lyricIndex));
        }
        return lyricContext;
    }

    /**
     * 获取下一首歌词
     * @return 下一首歌词对象，如果没有则返回 null
     */
    public LrcLyric getNextLyric() {
        if (currentTrack == null || currentTrack.isEmpty() || currentLyricIndex < 0) {
            return null;
        }

        return currentTrack.getNextLyric(currentLyricIndex);
    }

    /**
     * 获取上一首歌词
     * @return 上一首歌词对象，如果没有则返回 null
     */
    public LrcLyric getPreviousLyric() {
        if (currentTrack == null || currentTrack.isEmpty() || currentLyricIndex < 0) {
            return null;
        }

        return currentTrack.getPreviousLyric(currentLyricIndex);
    }

    /**
     * 获取当前歌词索引
     */
    public int getCurrentLyricIndex() {
        return currentLyricIndex;
    }

    /**
     * 获取当前加载的歌词轨道
     */
    public LyricTrack getCurrentTrack() {
        return currentTrack;
    }

    /**
     * 检查是否已加载歌词
     */
    public boolean hasLyrics() {
        return currentTrack != null && !currentTrack.isEmpty();
    }

    /**
     * 获取歌词总数
     */
    public int getLyricCount() {
        if (currentTrack == null) {
            return 0;
        }
        return currentTrack.getLyricCount();
    }

    /**
     * 清除当前歌词
     */
    public void clearLyrics() {
        this.currentTrack = null;
        this.currentLyricIndex = -1;
        LyricLive.LOGGER.info("歌词已清除");
    }

    /**
     * 重新加载当前歌词文件（如果已加载）
     */
    public void reloadLyrics(Path filePath) throws IOException {
        if (filePath != null) {
            loadLyricFile(filePath);
        }
    }
}
