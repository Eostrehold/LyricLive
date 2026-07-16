package com.eostrehold.lyriclive.client.gui;

import com.eostrehold.lyriclive.client.core.PlaybackController;
import com.eostrehold.lyriclive.client.core.TimelineManager;
import com.eostrehold.lyriclive.client.display.DisplayConfig;
import com.eostrehold.lyriclive.client.lrc.LyricTrack;
import com.eostrehold.lyriclive.client.sender.LyricSender;
import com.eostrehold.lyriclive.client.LyricLiveClient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class MainScreen extends Screen {
    private static final int BTN_W = 120;
    private static final int BTN_H = 20;
    private static final int C_WHITE  = 0xFFFFFFFF;
    private static final int C_YELLOW = 0xFFFFFF55;
    private static final int C_GRAY   = 0xFFAAAAAA;
    private static final int C_GREEN  = 0xFF55FF55;
    private static final int C_RED    = 0xFFFF5555;

    private final PlaybackController playbackController;
    private final TimelineManager timelineManager;
    private final DisplayConfig displayConfig;
    private final LyricSender chatSender;
    private final LyricSender commandSender;

    private Button playPauseButton;
    private Button stopButton;
    private Button chatSendToggleButton;
    private Button settingsButton;
    private Button loadRefreshButton;

    private List<Path> discoveredLrcFiles = new ArrayList<>();
    private Path currentLyricFile;
    private String statusMessage = "请将 .lrc 放入 lyriclive/ 后点[刷新列表]";

    // 进度条配置
    private static final int PROGRESS_BAR_H = 8;
    private static final int PROGRESS_BAR_W = 260;
    private static final int PROGRESS_BAR_MARGIN_BOTTOM = 10;
    private static final int C_PROGRESS_BG = 0xFF333333;
    private static final int C_PROGRESS_FG = 0xFF55FF55;
    private static final int C_PROGRESS_HOVER = 0xFF77FF77;

    public MainScreen(PlaybackController playbackController, TimelineManager timelineManager,
                      LyricSender chatSender, LyricSender commandSender,
                      DisplayConfig displayConfig) {
        super(Component.literal("LyricLive"));
        this.playbackController = playbackController;
        this.timelineManager = timelineManager;
        this.displayConfig = displayConfig;
        this.chatSender = chatSender;
        this.commandSender = commandSender;
    }

    @Override
    protected void init() {
        super.init();

        // top bar
        loadRefreshButton = newButton("刷新列表", this.width / 2 - 65, 4, 130, this::refreshFiles);
        settingsButton = newButton("设置", this.width - 55, 4, 50, this::openSettings);

        // fine seek
        int fineY = this.height - 65;
        int fineX = (this.width - 5 * (BTN_W + 5)) / 2;
        newButton("-10s", fineX, fineY, BTN_W, () -> seek(-10_000));
        fineX += BTN_W + 5;
        newButton("-1s",  fineX, fineY, BTN_W, () -> seek(-1_000));
        fineX += BTN_W + 5;
        newButton("◇",    fineX, fineY, BTN_W, () -> {});
        fineX += BTN_W + 5;
        newButton("+1s",  fineX, fineY, BTN_W, () -> seek(1_000));
        fineX += BTN_W + 5;
        newButton("+10s", fineX, fineY, BTN_W, () -> seek(10_000));

        // main control
        int ctrlY = fineY - BTN_H - 3;
        int ctrlX = (this.width - 5 * (BTN_W + 5)) / 2;
        chatSendToggleButton = newButton(chatSendLabel(), ctrlX, ctrlY, BTN_W, this::toggleChatSending);
        ctrlX += BTN_W + 5;
stopButton = newButton("停止",                ctrlX, ctrlY, BTN_W, this::stopPlayback);
        ctrlX += BTN_W + 5;
playPauseButton = newButton(playLabel(),      ctrlX, ctrlY, BTN_W, this::togglePlayPause);
        addLyricFileButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        Font f = Minecraft.getInstance().font;
        g.fill(0, 0, this.width, this.height, 0x80000000);
        super.extractRenderState(g, mx, my, pt);

        drawCentered(g, f, "LyricLive", this.width / 2, 6, C_YELLOW);

        // 右侧信息栏
        int ix = this.width - 145, iy = 24;
        drawLeft(g, f, "状态: " + stateLabel(), ix, iy, C_WHITE);
        drawLeft(g, f, "自动发送: " + (LyricLiveClient.isAutoSendEnabled() ? "开" : "关"), ix, iy + 11, LyricLiveClient.isAutoSendEnabled() ? C_GREEN : C_RED);

        if (timelineManager.hasLyrics()) {
            LyricTrack track = timelineManager.getCurrentTrack();
            String name = currentLyricFile != null ? currentLyricFile.getFileName().toString() : "";
            if (!name.isEmpty()) drawLeft(g, f, trunc("文件: " + name, 18), ix, iy + 24, C_GRAY);
            if (track.getTitle() != null)  drawLeft(g, f, trunc("歌曲: " + track.getTitle(), 18), ix, iy + 36, C_GRAY);
            if (track.getArtist() != null) drawLeft(g, f, trunc("演唱: " + track.getArtist(), 18), ix, iy + 48, C_GRAY);

            long cur = playbackController.getCurrentTimeMillis();
            long total = lastTimestamp();
            drawLeft(g, f, "进度: " + fmtTime(cur) + " / " + fmtTime(total), ix, iy + 62, C_WHITE);
        }

        // 歌词进度条
        if (timelineManager.hasLyrics()) {
            drawProgressBar(g, mx, my);
        }

        // 状态提示
        if (statusMessage != null && !statusMessage.isEmpty()) {
            drawLeft(g, f, statusMessage, 10, this.height - 14, C_GRAY);
        }
    }

    private Button newButton(String text, int x, int y, int w, Runnable action) {
        Button b = Button.builder(Component.literal(text), btn -> action.run()).bounds(x, y, w, BTN_H).build();
        addRenderableWidget(b);
        return b;
    }

    private String stateLabel() {
        return switch (playbackController.getState()) {
            case PLAYING -> "播放中";
            case PAUSED  -> "已暂停";
            case STOPPED -> "已停止";
        };
    }
    private String playLabel() { return playbackController.isPlaying() ? "暂停" : "播放"; }
    private String chatSendLabel() { return LyricLiveClient.isAutoSendEnabled() ? "自动发: 开" : "自动发: 关"; }

    private void togglePlayPause() {
        if (playbackController.isPlaying()) playbackController.pause();
        else playbackController.play();
        playPauseButton.setMessage(Component.literal(playLabel()));
    }
    private void stopPlayback() {
        playbackController.stop();
        playPauseButton.setMessage(Component.literal(playLabel()));
    }
    private void toggleChatSending() {
        LyricLiveClient.setAutoSendEnabled(!LyricLiveClient.isAutoSendEnabled());
        chatSendToggleButton.setMessage(Component.literal(chatSendLabel()));
    }
    private void seek(long deltaMs) { playbackController.seek(deltaMs); }

    private void openSettings() {
        assert this.minecraft != null;
        this.minecraft.setScreen(new SettingsScreen(displayConfig, chatSender, commandSender, this));
    }

    private void refreshFiles() {
        try {
            scanLyricDirectory();
            rebuildFileButtons();
            statusMessage = discoveredLrcFiles.isEmpty() ? "lyriclive/ 下未找到 .lrc 文件" : "已刷新歌词列表";
        } catch (IOException e) {
            statusMessage = "读取目录失败: " + e.getMessage();
        }
    }

    private void scanLyricDirectory() throws IOException {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("lyriclive");
        Files.createDirectories(dir);
        try (Stream<Path> s = Files.list(dir)) {
            discoveredLrcFiles = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".lrc"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList();
        }
    }

    private void addLyricFileButtons() {
        try { scanLyricDirectory(); } catch (IOException ignored) { return; }
        int count = Math.min(discoveredLrcFiles.size(), 6);
        int rx = this.width / 2 - 130;
        for (int i = 0; i < count; i++) {
            Path p = discoveredLrcFiles.get(i);
            newButton(p.getFileName().toString(), rx, 50 + i * 21, 260, () -> loadFile(p));
        }
    }

    private void rebuildFileButtons() { this.clearWidgets(); this.init(); }

    private void loadFile(Path path) {
        try {
            timelineManager.loadLyricFile(path);
            currentLyricFile = path;
            statusMessage = "已加载: " + path.getFileName();
        } catch (IOException e) {
            statusMessage = "加载失败: " + e.getMessage();
        }
    }

    private static String trunc(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }
    private static String fmtTime(long ms) {
        if (ms <= 0) return "00:00";
        long sec = ms / 1000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }
    private long lastTimestamp() {
        if (!timelineManager.hasLyrics()) return 0;
        var list = timelineManager.getCurrentTrack().getLyrics();
        return list.isEmpty() ? 0 : list.get(list.size() - 1).getTimestamp();
    }
    private void drawLeft(GuiGraphicsExtractor g, Font f, String s, int x, int y, int c) {
        g.text(f, s, x, y, c, true);
    }
    private void drawCentered(GuiGraphicsExtractor g, Font f, String s, int x, int y, int c) {
        g.text(f, s, x - f.width(s) / 2, y, c, true);
    }

    public void setCurrentLyricFile(Path file) { this.currentLyricFile = file; }
    @Override public boolean isPauseScreen() { return false; }

    private void drawProgressBar(GuiGraphicsExtractor g, int mx, int my) {
        Font f = Minecraft.getInstance().font;
        long cur = playbackController.getCurrentTimeMillis();
        long total = lastTimestamp();
        if (total <= 0) return;

        int barX = (this.width - PROGRESS_BAR_W) / 2;
        int barY = this.height - PROGRESS_BAR_MARGIN_BOTTOM - PROGRESS_BAR_H;

        boolean hover = mx >= barX && mx <= barX + PROGRESS_BAR_W
                     && my >= barY && my <= barY + PROGRESS_BAR_H;
        int fgColor = hover ? C_PROGRESS_HOVER : C_PROGRESS_FG;

        // 背景
        g.fill(barX, barY, barX + PROGRESS_BAR_W, barY + PROGRESS_BAR_H, C_PROGRESS_BG);
        // 进度
        int fillW = (int) (PROGRESS_BAR_W * Math.min(1.0, (double) cur / total));
        g.fill(barX, barY, barX + fillW, barY + PROGRESS_BAR_H, fgColor);

        // 时间文本
        String timeText = fmtTime(cur) + " / " + fmtTime(total);
        g.text(f, timeText, barX + PROGRESS_BAR_W / 2 - f.width(timeText) / 2, barY - 12, C_WHITE, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.buttonInfo().button();

        if (button != 0 || !timelineManager.hasLyrics()) {
            return super.mouseClicked(event, isDoubleClick);
        }

        int barX = (this.width - PROGRESS_BAR_W) / 2;
        int barY = this.height - PROGRESS_BAR_MARGIN_BOTTOM - PROGRESS_BAR_H;

        if (mouseX >= barX && mouseX <= barX + PROGRESS_BAR_W
                && mouseY >= barY && mouseY <= barY + PROGRESS_BAR_H) {
            long total = lastTimestamp();
            if (total > 0) {
                double ratio = (mouseX - barX) / (double) PROGRESS_BAR_W;
                long targetMs = (long) (total * Math.max(0.0, Math.min(1.0, ratio)));
                playbackController.seekTo(targetMs);
                if (!playbackController.isPlaying()) {
                    playbackController.play();
                }
            }
            return true;
        }

        return super.mouseClicked(event, isDoubleClick);
    }
}