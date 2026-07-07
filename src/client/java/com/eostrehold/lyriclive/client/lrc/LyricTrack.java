package com.eostrehold.lyriclive.client.lrc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表示整首歌词，包含元数据和歌词列表。
 */
public class LyricTrack {
    private String title;      // 歌曲标题
    private String artist;     // 艺术家
    private String album;      // 专辑
    private String author;     // 歌词作者
    private final List<LrcLyric> lyrics; // 歌词列表（按时间排序）

    public LyricTrack() {
        this.lyrics = new ArrayList<>();
    }

    public LyricTrack(String title, String artist, String album, String author, List<LrcLyric> lyrics) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.author = author;
        this.lyrics = new ArrayList<>(lyrics);
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public String getAuthor() {
        return author;
    }

    public List<LrcLyric> getLyrics() {
        return Collections.unmodifiableList(lyrics);
    }

    // Setters
    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    /**
     * 添加歌词行（自动保持时间顺序）
     */
    public void addLyric(LrcLyric lyric) {
        lyrics.add(lyric);
        lyrics.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
    }

    /**
     * 获取指定时间对应的歌词索引
     * @param timeMs 当前时间（毫秒）
     * @return 歌词索引，如果没有找到则返回 -1
     */
    public int getCurrentLyricIndex(long timeMs) {
        if (lyrics.isEmpty()) {
            return -1;
        }

        // 从后向前查找第一个时间戳小于等于当前时间的歌词
        for (int i = lyrics.size() - 1; i >= 0; i--) {
            if (lyrics.get(i).getTimestamp() <= timeMs) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 获取指定时间对应的歌词
     * @param timeMs 当前时间（毫秒）
     * @return 歌词行，如果没有找到则返回 null
     */
    public LrcLyric getCurrentLyric(long timeMs) {
        int index = getCurrentLyricIndex(timeMs);
        return index >= 0 ? lyrics.get(index) : null;
    }

    /**
     * 获取下一首歌词
     * @param currentIndex 当前歌词索引
     * @return 下一首歌词，如果没有则返回 null
     */
    public LrcLyric getNextLyric(int currentIndex) {
        int nextIndex = currentIndex + 1;
        if (nextIndex < lyrics.size()) {
            return lyrics.get(nextIndex);
        }
        return null;
    }

    /**
     * 获取上一首歌词
     * @param currentIndex 当前歌词索引
     * @return 上一首歌词，如果没有则返回 null
     */
    public LrcLyric getPreviousLyric(int currentIndex) {
        int prevIndex = currentIndex - 1;
        if (prevIndex >= 0) {
            return lyrics.get(prevIndex);
        }
        return null;
    }

    /**
     * 获取歌词总数
     */
    public int getLyricCount() {
        return lyrics.size();
    }

    /**
     * 检查歌词是否为空
     */
    public boolean isEmpty() {
        return lyrics.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isEmpty()) {
            sb.append("标题: ").append(title).append("\n");
        }
        if (artist != null && !artist.isEmpty()) {
            sb.append("艺术家: ").append(artist).append("\n");
        }
        sb.append("歌词数量: ").append(lyrics.size());
        return sb.toString();
    }
}