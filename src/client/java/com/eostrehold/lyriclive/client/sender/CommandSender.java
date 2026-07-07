package com.eostrehold.lyriclive.client.sender;

import com.eostrehold.lyriclive.LyricLive;
import net.minecraft.client.Minecraft;

public class CommandSender {
    private boolean enabled = false;
    private String prefix = "♪ ";
    private String lastSentLyric = "";

    public CommandSender() {
    }

    public boolean sendLyric(String lyricText) {
        if (!enabled) return false;
        if (lyricText == null || lyricText.isEmpty()) return false;
        if (lyricText.equals(lastSentLyric)) return false;

        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.getConnection() != null) {
            client.getConnection().sendChat(prefix + lyricText);
            lastSentLyric = lyricText;
            LyricLive.LOGGER.debug("前缀歌词已发送: {}{}", prefix, lyricText);
            return true;
        }
        return false;
    }

    public boolean sendCurrentLyric(String lyricText) { return sendLyric(lyricText); }

    public boolean forceSendLyric(String lyricText) {
        if (lyricText == null || lyricText.isEmpty()) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && client.getConnection() != null) {
            client.getConnection().sendChat(prefix + lyricText);
            lastSentLyric = lyricText;
            LyricLive.LOGGER.debug("前缀歌词已强制发送: {}{}", prefix, lyricText);
            return true;
        }
        return false;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        LyricLive.LOGGER.info("前缀发送{}", enabled ? "已启用" : "已禁用");
    }

    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        LyricLive.LOGGER.info("发送前缀已更新: {}", prefix);
    }

    public String getLastSentLyric() { return lastSentLyric; }
    public void clearLastSentLyric() { lastSentLyric = ""; }
}