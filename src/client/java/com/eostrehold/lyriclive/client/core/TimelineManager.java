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

    // ponytail: 按时间戳缓存歌词索引，同毫秒内多次查询直接命中缓存。
    // 已知限制：假设同一毫秒内歌词索引不会变化（对歌词场景完全成立）。
    // 未来升级路径：如有更高精度需求，可改为按帧号缓存。
    private long cachedTimeMs = Long.MIN_VALUE;
    private int cachedLyricIndex = -1;
    private int cachedContextCenter = -1;
    private int cachedContextSurround = -1;
    private List<LrcLyric> cachedContext = List.of();

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
        invalidateCache();
    }

    /**
     * 获取当前歌词文本
     * @return 当前歌词文本，如果没有则返回 null
     */
    public String getCurrentLyricText() {
        if (currentTrack == null || currentTrack.isEmpty()) {
            return null;
        }

        int newIndex = resolveCurrentLyricIndex();
        if (newIndex < 0) return null;
        if (newIndex != currentLyricIndex) {
            currentLyricIndex = newIndex;
            LyricLive.LOGGER.debug("歌词切换: {}", currentTrack.getLyrics().get(newIndex).getText());
        }
        return currentTrack.getLyrics().get(newIndex).getText();
    }

    /**
     * 获取当前歌词对象
     * @return 当前歌词对象，如果没有则返回 null
     */
    public LrcLyric getCurrentLyric() {
        if (currentTrack == null || currentTrack.isEmpty()) {
            return null;
        }

        int newIndex = resolveCurrentLyricIndex();
        if (newIndex < 0) return null;
        currentLyricIndex = newIndex;
        return currentTrack.getLyrics().get(newIndex);
    }

    /**
     * 获取当前歌词上下文行，包含当前行前后指定数量的歌词。
     * 结果按 (centerIndex, surroundCount) 缓存，歌词不变时直接复用。
     * @param surroundingLineCount 当前歌词前后各取多少行
     * @return 当前歌词上下文列表
     */
    public List<LrcLyric> getCurrentLyricContext(int surroundingLineCount) {
        if (currentTrack == null || currentTrack.isEmpty()) {
            return List.of();
        }

        int centerIndex = resolveCurrentLyricIndex();
        if (centerIndex < 0) return List.of();
        currentLyricIndex = centerIndex;

        if (centerIndex == cachedContextCenter && surroundingLineCount == cachedContextSurround) {
            return cachedContext;
        }

        int first = Math.max(0, centerIndex - surroundingLineCount);
        int last = Math.min(currentTrack.getLyricCount() - 1, centerIndex + surroundingLineCount);
        List<LrcLyric> ctx = new ArrayList<>(last - first + 1);
        for (int i = first; i <= last; i++) {
            ctx.add(currentTrack.getLyrics().get(i));
        }
        cachedContext = ctx;
        cachedContextCenter = centerIndex;
        cachedContextSurround = surroundingLineCount;
        return ctx;
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
        invalidateCache();
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

    private int resolveCurrentLyricIndex() {
        if (currentTrack == null || currentTrack.isEmpty()) return -1;
        long timeMs = playbackController.getCurrentTimeMillis();
        if (timeMs != cachedTimeMs) {
            cachedTimeMs = timeMs;
            cachedLyricIndex = currentTrack.getCurrentLyricIndex(timeMs);
        }
        return cachedLyricIndex;
    }

    private void invalidateCache() {
        cachedTimeMs = Long.MIN_VALUE;
        cachedLyricIndex = -1;
        cachedContextCenter = -1;
        cachedContext = List.of();
    }
}
