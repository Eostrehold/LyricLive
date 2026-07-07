package com.eostrehold.lyriclive.client.display;

import com.eostrehold.lyriclive.client.core.TimelineManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 歌词渲染器，负责在屏幕上绘制歌词。
 */
public class LyricRenderer {
    private final TimelineManager timelineManager;
    private final DisplayConfig config;
    private String lastLyricText;
    private long lastLyricChangeTime;

    public LyricRenderer(TimelineManager timelineManager, DisplayConfig config) {
        this.timelineManager = timelineManager;
        this.config = config;
    }

    /**
     * 渲染歌词（每帧调用）
     * @param guiGraphics GUI 图形上下文
     */
    public void render(GuiGraphics guiGraphics) {
        if (!timelineManager.hasLyrics()) {
            return;
        }

        String lyricText = timelineManager.getCurrentLyricText();
        if (lyricText == null || lyricText.isEmpty()) {
            return;
        }

        // 检测歌词变化
        if (!lyricText.equals(lastLyricText)) {
            lastLyricText = lyricText;
            lastLyricChangeTime = System.currentTimeMillis();
        }

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // 计算位置
        int x = config.getPixelX(screenWidth);
        int y = config.getPixelY(screenHeight);

        // 计算透明度（考虑淡入淡出效果）
        float alpha = config.getOpacity();
        if (config.isFadeInOutEnabled()) {
            long elapsed = System.currentTimeMillis() - lastLyricChangeTime;
            if (elapsed < config.getFadeInDuration()) {
                // 淡入阶段
                alpha *= (float) elapsed / config.getFadeInDuration();
            }
        }

        // 获取带透明度的颜色 (ARGB格式)
        int alphaValue = (int) (alpha * 255) & 0xFF;
        int fontColor = (alphaValue << 24) | (config.getFontColor() & 0x00FFFFFF);

        // 绘制歌词文本
        if (config.isCentered()) {
            guiGraphics.drawCenteredString(client.font, lyricText, x, y, fontColor);
        } else {
            guiGraphics.drawString(client.font, lyricText, x, y, fontColor, config.isShadowEnabled());
        }
    }

    /**
     * 获取显示配置
     */
    public DisplayConfig getConfig() {
        return config;
    }

    /**
     * 获取时间轴管理器
     */
    public TimelineManager getTimelineManager() {
        return timelineManager;
    }
}