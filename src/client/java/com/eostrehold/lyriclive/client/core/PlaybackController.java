package com.eostrehold.lyriclive.client.core;

import com.eostrehold.lyriclive.LyricLive;

/**
 * 播放控制器，管理歌曲的播放、暂停、停止等操作。
 * 使用 System.nanoTime() 保证高精度时间同步。
 */
public class PlaybackController {
    private PlaybackState state = PlaybackState.STOPPED;
    private long startNanoTime;      // 播放开始时的系统纳秒时间
    private long offsetMillis = 0;   // 时间偏移量（毫秒），用于调整起始时间
    private long pauseNanoTime;      // 暂停时的系统纳秒时间

    /**
     * 开始播放
     */
    public void play() {
        if (state == PlaybackState.PLAYING) {
            return;
        }

        if (state == PlaybackState.PAUSED) {
            // 从暂停状态恢复：调整起始时间以补偿暂停期间的时间
            long pauseDuration = System.nanoTime() - pauseNanoTime;
            startNanoTime += pauseDuration;
        } else {
            // 从停止状态开始：重置起始时间
            startNanoTime = System.nanoTime();
        }

        state = PlaybackState.PLAYING;
        LyricLive.LOGGER.info("播放开始");
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (state != PlaybackState.PLAYING) {
            return;
        }

        pauseNanoTime = System.nanoTime();
        state = PlaybackState.PAUSED;
        LyricLive.LOGGER.info("播放暂停");
    }

    /**
     * 继续播放（与 play 相同，但从暂停状态恢复）
     */
    public void resume() {
        play();
    }

    /**
     * 停止播放
     */
    public void stop() {
        state = PlaybackState.STOPPED;
        offsetMillis = 0;
        LyricLive.LOGGER.info("播放停止");
    }

    /**
     * 获取当前播放状态
     */
    public PlaybackState getState() {
        return state;
    }

    /**
     * 获取当前播放时间（毫秒）
     * @return 当前时间（毫秒），如果未播放则返回 0
     */
    public long getCurrentTimeMillis() {
        if (state == PlaybackState.STOPPED) {
            return 0;
        }

        long currentNanoTime;
        if (state == PlaybackState.PAUSED) {
            currentNanoTime = pauseNanoTime;
        } else {
            currentNanoTime = System.nanoTime();
        }

        long elapsedNanos = currentNanoTime - startNanoTime;
        long elapsedMillis = elapsedNanos / 1_000_000;
        return elapsedMillis + offsetMillis;
    }

    /**
     * 设置播放起始时间（毫秒）
     * @param timeMs 起始时间（毫秒）
     */
    public void setStartTime(long timeMs) {
        this.offsetMillis = timeMs;
        if (state == PlaybackState.PLAYING) {
            startNanoTime = System.nanoTime();
        }
        LyricLive.LOGGER.info("设置起始时间: {}ms", timeMs);
    }

    /**
     * 获取当前时间偏移量
     */
    public long getOffsetMillis() {
        return offsetMillis;
    }

    /**
     * 调整播放进度（快进/快退）
     * @param deltaMs 调整量（毫秒），正数为快进，负数为快退
     */
    public void seek(long deltaMs) {
        offsetMillis += deltaMs;
        if (offsetMillis < 0) {
            offsetMillis = 0;
        }
        LyricLive.LOGGER.info("调整播放进度: {}ms, 当前偏移: {}ms", deltaMs, offsetMillis);
    }

    /**
     * 跳转到指定时间
     * @param timeMs 目标时间（毫秒）
     */
    public void seekTo(long timeMs) {
        this.offsetMillis = timeMs;
        if (state == PlaybackState.PLAYING) {
            startNanoTime = System.nanoTime();
        }
        LyricLive.LOGGER.info("跳转到: {}ms", timeMs);
    }

    /**
     * 检查是否正在播放
     */
    public boolean isPlaying() {
        return state == PlaybackState.PLAYING;
    }

    /**
     * 检查是否已暂停
     */
    public boolean isPaused() {
        return state == PlaybackState.PAUSED;
    }

    /**
     * 检查是否已停止
     */
    public boolean isStopped() {
        return state == PlaybackState.STOPPED;
    }
}