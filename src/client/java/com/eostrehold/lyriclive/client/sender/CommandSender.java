package com.eostrehold.lyriclive.client.sender;

import com.eostrehold.lyriclive.LyricLive;
import net.minecraft.client.Minecraft;

/**
 * 指令发送器，将歌词作为游戏指令发送。
 */
public class CommandSender {
    private boolean enabled = false;
    private String commandTemplate = "/say {lyric}"; // 指令模板，{lyric} 会被替换为歌词文本
    private String lastSentLyric = "";

    public CommandSender() {
    }

    /**
     * 发送歌词作为指令
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
        if (client.player != null && client.getConnection() != null) {
            String command = commandTemplate.replace("{lyric}", lyricText);
            // 移除开头的 / 如果有
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            client.getConnection().sendCommand(command);
            lastSentLyric = lyricText;
            LyricLive.LOGGER.debug("歌词指令已发送: {}", command);
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
        if (client.player != null && client.getConnection() != null) {
            String command = commandTemplate.replace("{lyric}", lyricText);
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            client.getConnection().sendCommand(command);
            lastSentLyric = lyricText;
            LyricLive.LOGGER.debug("歌词指令已强制发送: {}", command);
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
        LyricLive.LOGGER.info("指令发送器{}", enabled ? "已启用" : "已禁用");
    }

    /**
     * 获取指令模板
     */
    public String getCommandTemplate() {
        return commandTemplate;
    }

    /**
     * 设置指令模板
     * @param template 指令模板，{lyric} 会被替换为歌词文本
     */
    public void setCommandTemplate(String template) {
        this.commandTemplate = template;
        LyricLive.LOGGER.info("指令模板已更新: {}", template);
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