package com.eostrehold.lyriclive.client.sender;

import com.eostrehold.lyriclive.LyricLive;
import net.minecraft.client.Minecraft;

/**
 * 歌词发送器，支持普通发送和前缀发送两种模式。
 * 替代 ChatSender（无前缀）和 CommandSender（带前缀）的统一实现。
 */
public class LyricSender {
    private boolean enabled = true;
    private String prefix = "";
    private String lastSentLyric = "";

    public LyricSender() {
    }

    /**
     * 发送歌词到聊天栏
     * @param lyricText 歌词文本
     * @return 是否成功发送
     */
    public boolean sendLyric(String lyricText) {
        if (!enabled) return false;
        if (lyricText == null || lyricText.isEmpty()) return false;
        if (lyricText.equals(lastSentLyric)) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.getConnection() != null) {
            client.getConnection().sendChat(prefix + lyricText);
            lastSentLyric = lyricText;
            LyricLive.LOGGER.debug("歌词已发送: {}{}", prefix, lyricText);
            return true;
        }
        return false;
    }

    /**
     * 发送当前歌词（如果歌词已变化）
     */
    public boolean sendCurrentLyric(String lyricText) {
        return sendLyric(lyricText);
    }

    /**
     * 强制发送歌词（忽略重复检查和 enabled 状态）
     */
    public boolean forceSendLyric(String lyricText) {
        if (lyricText == null || lyricText.isEmpty()) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.getConnection() != null) {
            client.getConnection().sendChat(prefix + lyricText);
            lastSentLyric = lyricText;
            LyricLive.LOGGER.debug("歌词已强制发送: {}{}", prefix, lyricText);
            return true;
        }
        return false;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LyricLive.LOGGER.info("歌词发送器{}", enabled ? "已启用" : "已禁用");
    }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        LyricLive.LOGGER.info("发送前缀已更新: {}", prefix);
    }

    public String getLastSentLyric() { return lastSentLyric; }
    public void clearLastSentLyric() { lastSentLyric = ""; }
}