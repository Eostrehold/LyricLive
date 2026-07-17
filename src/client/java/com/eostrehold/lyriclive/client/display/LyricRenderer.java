package com.eostrehold.lyriclive.client.display;

import com.eostrehold.lyriclive.client.core.PlaybackController;
import com.eostrehold.lyriclive.client.core.TimelineManager;
import com.eostrehold.lyriclive.client.lrc.LrcLyric;
import com.eostrehold.lyriclive.client.lrc.LyricTrack;
import com.eostrehold.lyriclive.client.sender.LyricSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

import com.eostrehold.lyriclive.client.util.LyricUtils;

public class LyricRenderer {
    private final TimelineManager timelineManager;
    private final PlaybackController playbackController;
    private final LyricSender chatSender;
    private final DisplayConfig config;
    private final IntSupplier manualIndexSupplier;
    private final BooleanSupplier autoSendSupplier;

    // 动画状态
    private float smoothCenterIndex = -1f;
    private long lastRenderNanos;
    private float fadeInAlpha = 0f;
    // 过渡动画
    private int lastRenderedCenter = -1;
    private float lineTransitionAlpha = 1f;

    // 信息栏淡入
public LyricRenderer(TimelineManager timelineManager, PlaybackController playbackController,
                         LyricSender chatSender, DisplayConfig config, IntSupplier manualIndexSupplier,
                         BooleanSupplier autoSendSupplier) {
        this.timelineManager = timelineManager;
        this.playbackController = playbackController;
        this.chatSender = chatSender;
        this.config = config;
        this.manualIndexSupplier = manualIndexSupplier;
        this.autoSendSupplier = autoSendSupplier;
        this.lastRenderNanos = System.nanoTime();
}

    public void render(GuiGraphicsExtractor g) {
        if (!timelineManager.hasLyrics()) {
            smoothCenterIndex = -1f;
            fadeInAlpha = 0f;
            lineTransitionAlpha = 1f;
            lastRenderedCenter = -1;
            return;
        }

        Minecraft c = Minecraft.getInstance();
        int sw = c.getWindow().getGuiScaledWidth();
        int sh = c.getWindow().getGuiScaledHeight();

        long now = System.nanoTime();
        float dt = (now - lastRenderNanos) / 1_000_000_000f;
        lastRenderNanos = now;

        // 淡入
        if (config.isFadeInOutEnabled()) {
            fadeInAlpha = Math.min(1f, fadeInAlpha + dt * (1000f / Math.max(1, config.getFadeInDuration())));
        } else {
            fadeInAlpha = 1f;
        }

        // 歌词切换过渡（淡出/淡入）
        List<LrcLyric> ctx = timelineManager.getCurrentLyricContext(2);
        if (ctx.isEmpty()) return;

        LrcLyric cur = timelineManager.getCurrentLyric();
        int realCenter = ctx.indexOf(cur);
        if (realCenter < 0) realCenter = 0;

        if (realCenter != lastRenderedCenter && lastRenderedCenter >= 0) {
            lineTransitionAlpha = 0f;
        }
        lastRenderedCenter = realCenter;

        if (config.isFadeInOutEnabled()) {
            float transitionSpeed = 1000f / Math.max(1, config.getFadeOutDuration());
            lineTransitionAlpha = Math.min(1f, lineTransitionAlpha + dt * transitionSpeed);
        } else {
            lineTransitionAlpha = 1f;
        }

        float baseAlpha = config.getOpacity() * fadeInAlpha;
        if (config.isFadeInOutEnabled()) {
            baseAlpha *= lineTransitionAlpha;
        }

        int a = (int) (baseAlpha * 255) & 0xFF;
        int fc = (a << 24) | (config.getFontColor() & 0x00FFFFFF);
        int dc = ((a * 6 / 10) << 24) | 0x00AAAAAA;
        int manualColor = ((a * 9 / 10) << 24) | 0x00FFFF00;

        LyricTrack track = timelineManager.getCurrentTrack();
        int mi = manualIndexSupplier.getAsInt();

        // 字体缩放：通过矩阵栈缩放所有绘制内容
        float fontScale = config.getFontSize() / 16.0f;
        int lh = Math.max(1, Math.round(11 / fontScale));

        var pose = g.pose();
        pose.pushMatrix();
        if (Math.abs(fontScale - 1.0f) > 0.001f) {
            pose.scale(fontScale, fontScale);
        }

        int baseX = Math.round(config.getPixelX(sw) / fontScale);
        int baseY = Math.round(config.getPixelY(sh) / fontScale);

        // 信息栏
        int infoY = baseY + ctx.size() * lh + 4;
        drawHudInfo(g, c, baseX, infoY, baseAlpha, track, lh);

        // 丝滑滚动
        if (smoothCenterIndex < 0) {
            smoothCenterIndex = realCenter;
        }
        float lerpSpeed = 8f;
        smoothCenterIndex += (realCenter - smoothCenterIndex) * Math.min(1f, lerpSpeed * dt);
        if (Math.abs(smoothCenterIndex - realCenter) < 0.01f) {
            smoothCenterIndex = realCenter;
        }

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

            float distFromCenter = Math.abs(i - smoothCenterIndex);
            float distFade = Math.max(0.3f, 1f - distFromCenter * 0.25f);
            int finalAlpha = (int) ((baseAlpha * distFade) * 255) & 0xFF;
            int finalColor = (finalAlpha << 24) | (color & 0x00FFFFFF);

            if (config.isCentered()) {
                g.text(c.font, text, baseX - c.font.width(text) / 2, (int) lineY, finalColor, config.isShadowEnabled());
            } else {
                g.text(c.font, text, baseX, (int) lineY, finalColor, config.isShadowEnabled());
            }
        }

        pose.popMatrix();
    }

    private void drawHudInfo(GuiGraphicsExtractor g, Minecraft c, int x, int y, float alpha, LyricTrack track, int lh) {
        int a = (int) (alpha * 255) & 0xFF;
        if (a < 4) return;
        int white = (a << 24) | 0xFFFFFF;
        int gray = (a << 24) | 0xAAAAAA;
        int green = (a << 24) | 0x55FF55;
        int red = (a << 24) | 0xFF5555;
        int row = 0;

        String name = track.getTitle() != null ? track.getTitle() : "";
        if (!name.isEmpty()) {
            g.text(c.font, LyricUtils.trunc(name, 22), x, y + row * lh, white, true);
            row++;
        }

        long curMs = playbackController.getCurrentTimeMillis();
        var lyrics = track.getLyrics();
        long totalMs = lyrics.isEmpty() ? 0 : lyrics.get(lyrics.size() - 1).getTimestamp();
        g.text(c.font, LyricUtils.fmtTime(curMs) + " / " + LyricUtils.fmtTime(totalMs), x, y + row * lh, gray, true);
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

        boolean auto = autoSendSupplier.getAsBoolean();
        g.text(c.font, auto ? "AUTO ON" : "AUTO OFF", x, y + row * lh, auto ? green : red, true);
    }

    public DisplayConfig getConfig() { return config; }
    public TimelineManager getTimelineManager() { return timelineManager; }
}