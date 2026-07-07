package com.eostrehold.lyriclive.client.display;

import com.eostrehold.lyriclive.client.core.PlaybackController;
import com.eostrehold.lyriclive.client.core.TimelineManager;
import com.eostrehold.lyriclive.client.lrc.LrcLyric;
import com.eostrehold.lyriclive.client.lrc.LyricTrack;
import com.eostrehold.lyriclive.client.sender.ChatSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.function.IntSupplier;

public class LyricRenderer {
    private final TimelineManager timelineManager;
    private final PlaybackController playbackController;
    private final ChatSender chatSender;
    private final DisplayConfig config;
    private final IntSupplier manualIndexSupplier;
    private String lastLyricText;
    private long lastLyricChangeTime;

    public LyricRenderer(TimelineManager timelineManager, PlaybackController playbackController,
                         ChatSender chatSender, DisplayConfig config, IntSupplier manualIndexSupplier) {
        this.timelineManager = timelineManager;
        this.playbackController = playbackController;
        this.chatSender = chatSender;
        this.config = config;
        this.manualIndexSupplier = manualIndexSupplier;
    }

    public void render(GuiGraphicsExtractor g) {
        if (!timelineManager.hasLyrics()) return;

        Minecraft c = Minecraft.getInstance();
        int x = config.getPixelX(c.getWindow().getGuiScaledWidth());
        int y = config.getPixelY(c.getWindow().getGuiScaledHeight());

        float alpha = config.getOpacity();
        int a = (int) (alpha * 255) & 0xFF;
        int fc = (a << 24) | (config.getFontColor() & 0x00FFFFFF);
        int dc = ((a / 2) << 24) | 0x00AAAAAA;

        LyricTrack track = timelineManager.getCurrentTrack();
        int mi = manualIndexSupplier.getAsInt();

        // 信息栏
        drawHudInfo(g, c, x, y - 36, fc, track);

        // 歌词上下文
        List<LrcLyric> ctx = timelineManager.getCurrentLyricContext(2);
        if (ctx.isEmpty()) return;

        LrcLyric cur = timelineManager.getCurrentLyric();
        if (cur != null && !cur.getText().equals(lastLyricText)) {
            lastLyricText = cur.getText();
            lastLyricChangeTime = System.currentTimeMillis();
        }

        int lh = 11, ci = ctx.indexOf(cur);
        if (ci < 0) ci = 0;

        for (int i = 0; i < ctx.size(); i++) {
            LrcLyric l = ctx.get(i);
            boolean autoHl = l.equals(cur);
            boolean manualHl = mi >= 0 && l.equals(track.getLyrics().get(mi));
            String prefix = manualHl ? "\u25b6 " : "";
            int color = manualHl ? 0xFFFFFF00 : (autoHl ? fc : dc);
            String t = prefix + l.getText();
            if (config.isCentered()) {
                g.text(c.font, t, x - c.font.width(t) / 2, y + (i - ci) * lh, color, config.isShadowEnabled());
            } else {
                g.text(c.font, t, x, y + (i - ci) * lh, color, config.isShadowEnabled());
            }
        }
    }

    private void drawHudInfo(GuiGraphicsExtractor g, Minecraft c, int x, int y, int color, LyricTrack track) {
        int lh = 10, row = 0;

        String name = track.getTitle() != null ? track.getTitle() : "";
        if (!name.isEmpty()) { g.text(c.font, trunc(name, 20), x, y + row * lh, color, true); row++; }

        long curMs = playbackController.getCurrentTimeMillis();
        var lyrics = track.getLyrics();
        long totalMs = lyrics.isEmpty() ? 0 : lyrics.get(lyrics.size() - 1).getTimestamp();
        g.text(c.font, fmtTime(curMs) + " / " + fmtTime(totalMs), x, y + row * lh, 0xAAAAAA, true); row++;

        String state = switch (playbackController.getState()) {
            case PLAYING -> "\u25b6 播放中";
            case PAUSED  -> "\u23f8 已暂停";
            case STOPPED -> "\u23f9 已停止";
        };
        g.text(c.font, state, x, y + row * lh, 0x55FF55, true); row++;

        g.text(c.font, chatSender.isEnabled() ? "\u2709 自动发送: 开" : "\u2709 自动发送: 关",
                x, y + row * lh, chatSender.isEnabled() ? 0x55FF55 : 0xFF5555, true);
    }

    private static String trunc(String s, int max) { return s.length() > max ? s.substring(0, max - 1) + "…" : s; }
    private static String fmtTime(long ms) {
        if (ms <= 0) return "00:00";
        long sec = ms / 1000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    public DisplayConfig getConfig() { return config; }
    public TimelineManager getTimelineManager() { return timelineManager; }
}