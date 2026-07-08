package com.eostrehold.lyriclive.client.display;

import com.eostrehold.lyriclive.client.core.PlaybackController;
import com.eostrehold.lyriclive.client.core.TimelineManager;
import com.eostrehold.lyriclive.client.lrc.LrcLyric;
import com.eostrehold.lyriclive.client.lrc.LyricTrack;
import com.eostrehold.lyriclive.client.sender.LyricSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.function.IntSupplier;

public class LyricRenderer {
    private final TimelineManager timelineManager;
    private final PlaybackController playbackController;
    private final LyricSender chatSender;
    private final DisplayConfig config;
    private final IntSupplier manualIndexSupplier;

    // 动画状态
    private float smoothCenterIndex = -1f;
    private long lastRenderNanos;
    private float fadeInAlpha = 0f;

    // 信息栏淡入
public LyricRenderer(TimelineManager timelineManager, PlaybackController playbackController,
                         LyricSender chatSender, DisplayConfig config, IntSupplier manualIndexSupplier) {
        this.timelineManager = timelineManager;
        this.playbackController = playbackController;
        this.chatSender = chatSender;
        this.config = config;
        this.manualIndexSupplier = manualIndexSupplier;
        this.lastRenderNanos = System.nanoTime();
}

    public void render(GuiGraphicsExtractor g) {
        if (!timelineManager.hasLyrics()) {
            smoothCenterIndex = -1f;
            fadeInAlpha = 0f;
            return;
        }

        Minecraft c = Minecraft.getInstance();
        int sw = c.getWindow().getGuiScaledWidth();
        int sh = c.getWindow().getGuiScaledHeight();
        int baseX = config.getPixelX(sw);
        int baseY = config.getPixelY(sh);

        long now = System.nanoTime();
        float dt = (now - lastRenderNanos) / 1_000_000_000f;
        lastRenderNanos = now;

        // 淡入
        if (config.isFadeInOutEnabled()) {
            fadeInAlpha = Math.min(1f, fadeInAlpha + dt * (1000f / Math.max(1, config.getFadeInDuration())));
        } else {
            fadeInAlpha = 1f;
        }

        float alpha = config.getOpacity() * fadeInAlpha;
        int a = (int) (alpha * 255) & 0xFF;
        int fc = (a << 24) | (config.getFontColor() & 0x00FFFFFF);
        int dc = ((a * 6 / 10) << 24) | 0x00AAAAAA;
        int manualColor = ((a * 9 / 10) << 24) | 0x00FFFF00;

        LyricTrack track = timelineManager.getCurrentTrack();
        int mi = manualIndexSupplier.getAsInt();

        // ---- 信息栏 (右下角，歌词下方) ----
        drawHudInfo(g, c, baseX, baseY + 5 * 11 + 4, alpha, track);

        // ---- 歌词上下文 (auto ±2 lines) ----
        List<LrcLyric> ctx = timelineManager.getCurrentLyricContext(2);
        if (ctx.isEmpty()) return;

        LrcLyric cur = timelineManager.getCurrentLyric();
        int realCenter = ctx.indexOf(cur);
        if (realCenter < 0) realCenter = 0;

        // 丝滑滚动：lerp smoothCenterIndex -> realCenter
        if (smoothCenterIndex < 0) {
            smoothCenterIndex = realCenter;
        }
        float lerpSpeed = 8f;
        smoothCenterIndex += (realCenter - smoothCenterIndex) * Math.min(1f, lerpSpeed * dt);
        if (Math.abs(smoothCenterIndex - realCenter) < 0.01f) {
            smoothCenterIndex = realCenter;
        }

        int lh = 11;
        float centerOffset = (smoothCenterIndex - realCenter) * lh;

        for (int i = 0; i < ctx.size(); i++) {
            LrcLyric l = ctx.get(i);
            boolean autoHl = l.equals(cur);
            boolean manualHl = mi >= 0 && l.equals(track.getLyrics().get(mi));

            int color;
            String prefix;
            if (manualHl) {
                prefix = "> ";
                color = manualColor;
            } else if (autoHl) {
                prefix = "";
                color = fc;
            } else {
                prefix = "";
                color = dc;
            }

            String text = prefix + l.getText();
            float lineY = baseY + (i - smoothCenterIndex) * lh;

            // 透明度随距离衰减
            float distFromCenter = Math.abs(i - smoothCenterIndex);
            float distFade = Math.max(0.3f, 1f - distFromCenter * 0.25f);
            int finalAlpha = (int) ((alpha * distFade) * 255) & 0xFF;
            int finalColor = (finalAlpha << 24) | (color & 0x00FFFFFF);

            if (config.isCentered()) {
                g.text(c.font, text, baseX - c.font.width(text) / 2, (int) lineY, finalColor, config.isShadowEnabled());
            } else {
                g.text(c.font, text, baseX, (int) lineY, finalColor, config.isShadowEnabled());
            }
        }
    }

    private void drawHudInfo(GuiGraphicsExtractor g, Minecraft c, int x, int y, float alpha, LyricTrack track) {
        int a = (int) (alpha * 255) & 0xFF;
        if (a < 4) return;
        int white = (a << 24) | 0xFFFFFF;
        int gray = (a << 24) | 0xAAAAAA;
        int green = (a << 24) | 0x55FF55;
        int red = (a << 24) | 0xFF5555;
        int lh = 10;
        int row = 0;

        String name = track.getTitle() != null ? track.getTitle() : "";
        if (!name.isEmpty()) {
            g.text(c.font, trunc(name, 22), x, y + row * lh, white, true);
            row++;
        }

        long curMs = playbackController.getCurrentTimeMillis();
        var lyrics = track.getLyrics();
        long totalMs = lyrics.isEmpty() ? 0 : lyrics.get(lyrics.size() - 1).getTimestamp();
        g.text(c.font, fmtTime(curMs) + " / " + fmtTime(totalMs), x, y + row * lh, gray, true);
        row++;

        String state = switch (playbackController.getState()) {
            case PLAYING -> "PLAY";
            case PAUSED -> "PAUSE";
            case STOPPED -> "STOP";
        };
        int stateColor = switch (playbackController.getState()) {
            case PLAYING -> green;
            case PAUSED -> (a << 24) | 0xFFFF55;
            case STOPPED -> red;
        };
        g.text(c.font, state, x, y + row * lh, stateColor, true);
        row++;

        boolean auto = chatSender.isEnabled();
        g.text(c.font, auto ? "AUTO ON" : "AUTO OFF", x, y + row * lh, auto ? green : red, true);
    }

    private static String trunc(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + ".." : s;
    }

    private static String fmtTime(long ms) {
        if (ms <= 0) return "00:00";
        long sec = ms / 1000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    public DisplayConfig getConfig() { return config; }
    public TimelineManager getTimelineManager() { return timelineManager; }
}