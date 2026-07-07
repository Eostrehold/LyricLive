package com.eostrehold.lyriclive.client.display;

import com.eostrehold.lyriclive.client.core.TimelineManager;
import com.eostrehold.lyriclive.client.lrc.LrcLyric;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

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

        List<LrcLyric> lyricContext = timelineManager.getCurrentLyricContext(2);
        if (lyricContext.isEmpty()) {
            return;
        }

        LrcLyric currentLyric = timelineManager.getCurrentLyric();
        String currentLyricText = currentLyric == null ? "" : currentLyric.getText();

        if (!currentLyricText.equals(lastLyricText)) {
            lastLyricText = currentLyricText;
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

        int lineHeight = 11;
        int centerIndex = lyricContext.indexOf(currentLyric);
        if (centerIndex < 0) {
            centerIndex = 0;
        }

        for (int contextIndex = 0; contextIndex < lyricContext.size(); contextIndex++) {
            LrcLyric contextLyric = lyricContext.get(contextIndex);
            boolean isCurrentLine = contextLyric.equals(currentLyric);
            int lineColor = isCurrentLine ? fontColor : ((alphaValue / 2) << 24) | 0x00AAAAAA;
            int lineY = y + (contextIndex - centerIndex) * lineHeight;
            String lyricText = contextLyric.getText();

            if (config.isCentered()) {
                int textWidth = client.font.width(lyricText);
                guiGraphics.text(client.font, lyricText, x - textWidth / 2, lineY, lineColor, config.isShadowEnabled());
            } else {
                guiGraphics.text(client.font, lyricText, x, lineY, lineColor, config.isShadowEnabled());
            }
        }
    }

    public DisplayConfig getConfig() {
        return config;
    }

    public TimelineManager getTimelineManager() {
        return timelineManager;
    }
}
