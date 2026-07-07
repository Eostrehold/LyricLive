package com.eostrehold.lyriclive.client.sender;

import com.eostrehold.lyriclive.LyricLive;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 聊天发送器，将歌词发送到游戏聊天栏。
 */
public class ChatSender {
    private boolean enabled = true;
    private String lastSentLyric = "";

    public ChatSender() {
    }

    /**
     * 发送歌词到聊天栏
     * @param lyricText 歌词文本
     * @return 是否成功发送
     */
    public boolean sendLyric(String lyricText) {
        if (!enabled) {
            return false;
        }

        if (lyricText == null || lyricText.isEmpty()) {
            return false;
        }

        // 避免重复发送相同的歌词
        if (lyricText.equals(lastSentLyric)) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(lyricText));
            lastSentLyric = lyricText;
            LyricLive.LOGGER.debug("歌词已发送: {}", lyricText);
            return true;
        }

        return false;
    }

    /**
     * 发送当前歌词（如果歌词已变化）
     * @param lyricText 当前歌词
     * @return 是否成功发送
     */
    public boolean sendCurrentLyric(String lyricText) {
        return sendLyric(lyricText);
    }

    /**
     * 强制发送歌词（忽略重复检查）
     * @param lyricText 歌词文本
     * @return 是否成功发送
     */
    public boolean forceSendLyric(String lyricText) {
        if (!enabled) {
            return false;
        }

        if (lyricText == null || lyricText.isEmpty()) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(lyricText));
            lastSentLyric = lyricText;
            LyricLive.LOGGER.debug("歌词已强制发送: {}", lyricText);
            return true;
        }

        return false;
    }

    /**
     * 检查是否启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置启用状态
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LyricLive.LOGGER.info("聊天发送器{}", enabled ? "已启用" : "已禁用");
    }

    /**
     * 获取上次发送的歌词
     */
    public String getLastSentLyric() {
        return lastSentLyric;
    }

    /**
     * 清除上次发送记录
     */
    public void clearLastSentLyric() {
        lastSentLyric = "";
    }
}