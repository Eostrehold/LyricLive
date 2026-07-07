package com.eostrehold.lyriclive.client.display;

import com.eostrehold.lyriclive.client.core.TimelineManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class LyricRenderer {
    private final TimelineManager timelineManager;
    private final DisplayConfig config;
    private String lastLyricText;
    private long lastLyricChangeTime;

    public LyricRenderer(TimelineManager timelineManager, DisplayConfig config) {
        this.timelineManager = timelineManager;
        this.config = config;
    }

    public void render(GuiGraphicsExtractor guiGraphics) {
        if (!timelineManager.hasLyrics()) {
            return;
        }

        String lyricText = timelineManager.getCurrentLyricText();
        if (lyricText == null || lyricText.isEmpty()) {
            return;
        }

        if (!lyricText.equals(lastLyricText)) {
            lastLyricText = lyricText;
            lastLyricChangeTime = System.currentTimeMillis();
        }

        Minecraft client = Minecraft.getInstance();
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int x = config.getPixelX(screenWidth);
        int y = config.getPixelY(screenHeight);

        float alpha = config.getOpacity();
        if (config.isFadeInOutEnabled()) {
            long elapsed = System.currentTimeMillis() - lastLyricChangeTime;
            if (elapsed < config.getFadeInDuration()) {
                alpha *= (float) elapsed / config.getFadeInDuration();
            }
        }

        int alphaValue = (int) (alpha * 255) & 0xFF;
        int fontColor = (alphaValue << 24) | (config.getFontColor() & 0x00FFFFFF);

        if (config.isCentered()) {
            int textWidth = client.font.width(lyricText);
            guiGraphics.text(client.font, lyricText, x - textWidth / 2, y, fontColor, config.isShadowEnabled());
        } else {
            guiGraphics.text(client.font, lyricText, x, y, fontColor, config.isShadowEnabled());
        }
    }

    public DisplayConfig getConfig() {
        return config;
    }

    public TimelineManager getTimelineManager() {
        return timelineManager;
    }
}