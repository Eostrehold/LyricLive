package com.eostrehold.lyriclive.client.gui;

import com.eostrehold.lyriclive.client.LyricLiveClient;
import com.eostrehold.lyriclive.client.core.PlaybackController;
import com.eostrehold.lyriclive.client.core.TimelineManager;
import com.eostrehold.lyriclive.client.display.DisplayConfig;
import com.eostrehold.lyriclive.client.gui.component.ProgressBarComponent;
import com.eostrehold.lyriclive.client.lrc.LyricTrack;
import com.eostrehold.lyriclive.client.sender.LyricSender;
import com.eostrehold.lyriclive.client.util.LyricUtils;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * 主控制界面。UI 结构由 {@code assets/lyriclive/owo_ui/main_screen.xml} 定义，
 * 本类负责事件绑定与动态内容。
 */
public class MainScreen extends BaseUIModelScreen<FlowLayout> {

    private static final Identifier MODEL_ID = Identifier.fromNamespaceAndPath("lyriclive", "main_screen");

    private final PlaybackController playbackController;
    private final TimelineManager timelineManager;
    private final DisplayConfig displayConfig;
    private final LyricSender chatSender;
    private final LyricSender commandSender;

    private List<Path> discoveredLrcFiles = new ArrayList<>();
    private Path currentLyricFile;
    private String statusMessage = "请将 .lrc 放入 lyriclive/ 后点[刷新列表]";

    private FlowLayout lyricList;
    private ButtonComponent playPauseButton;
    private ButtonComponent chatSendToggleButton;
    private LabelComponent statusLabel;
    private LabelComponent autoSendLabel;
    private LabelComponent fileLabel;
    private LabelComponent titleLabel;
    private LabelComponent artistLabel;
    private LabelComponent progressLabel;
    private LabelComponent statusMessageLabel;

    public MainScreen(PlaybackController playbackController, TimelineManager timelineManager,
                      LyricSender chatSender, LyricSender commandSender,
                      DisplayConfig displayConfig) {
        super(FlowLayout.class, MODEL_ID);
        this.playbackController = playbackController;
        this.timelineManager = timelineManager;
        this.displayConfig = displayConfig;
        this.chatSender = chatSender;
        this.commandSender = commandSender;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        // 顶部工具条
        rootComponent.childById(ButtonComponent.class, "refresh-button").onPress(button -> refreshFiles());
        rootComponent.childById(ButtonComponent.class, "settings-button").onPress(button -> openSettings());

        // 主控制
        chatSendToggleButton = rootComponent.childById(ButtonComponent.class, "chat-toggle-button");
        chatSendToggleButton.onPress(button -> toggleChatSending());
        rootComponent.childById(ButtonComponent.class, "stop-button").onPress(button -> stopPlayback());
        playPauseButton = rootComponent.childById(ButtonComponent.class, "play-pause-button");
        playPauseButton.onPress(button -> togglePlayPause());

        // 微调
        rootComponent.childById(ButtonComponent.class, "seek-10-back").onPress(button -> seek(-10_000));
        rootComponent.childById(ButtonComponent.class, "seek-1-back").onPress(button -> seek(-1_000));
        rootComponent.childById(ButtonComponent.class, "seek-center").onPress(button -> {});
        rootComponent.childById(ButtonComponent.class, "seek-1-forward").onPress(button -> seek(1_000));
        rootComponent.childById(ButtonComponent.class, "seek-10-forward").onPress(button -> seek(10_000));

        // 信息栏
        statusLabel = rootComponent.childById(LabelComponent.class, "status-label");
        autoSendLabel = rootComponent.childById(LabelComponent.class, "auto-send-label");
        fileLabel = rootComponent.childById(LabelComponent.class, "file-label");
        titleLabel = rootComponent.childById(LabelComponent.class, "title-label");
        artistLabel = rootComponent.childById(LabelComponent.class, "artist-label");
        progressLabel = rootComponent.childById(LabelComponent.class, "progress-label");
        statusMessageLabel = rootComponent.childById(LabelComponent.class, "status-message");

        // 歌词列表容器
        lyricList = rootComponent.childById(FlowLayout.class, "lyric-list");

        // 注入自定义进度条
        FlowLayout progressContainer = rootComponent.childById(FlowLayout.class, "progress-container");
        progressContainer.horizontalAlignment(io.wispforest.owo.ui.core.HorizontalAlignment.CENTER);
        progressContainer.child(new ProgressBarComponent(playbackController, timelineManager));

        // 初始歌词列表
        try {
            scanLyricDirectory();
            populateLyricList();
        } catch (IOException e) {
            statusMessage = "读取目录失败: " + e.getMessage();
        }

        refreshStaticLabels();
    }

    @Override
    public void tick() {
        super.tick();
        refreshDynamicLabels();
    }

    private void refreshStaticLabels() {
        if (playPauseButton != null) {
            playPauseButton.setMessage(Component.literal(playLabel()));
            chatSendToggleButton.setMessage(Component.literal(chatSendLabel()));
        }
        if (statusMessageLabel != null) {
            statusMessageLabel.text(Component.literal(statusMessage));
        }
    }

    private void refreshDynamicLabels() {
        if (statusLabel == null) return;

        statusLabel.text(Component.literal("状态: " + stateLabel()));

        boolean autoSend = LyricLiveClient.isAutoSendEnabled();
        autoSendLabel.text(Component.literal("自动发送: " + (autoSend ? "开" : "关")))
                .color(autoSend ? Color.ofRgb(0x55FF55) : Color.ofRgb(0xFF5555));

        LyricTrack track = timelineManager.hasLyrics() ? timelineManager.getCurrentTrack() : null;
        String name = currentLyricFile != null ? currentLyricFile.getFileName().toString() : "";
        fileLabel.text(Component.literal(name.isEmpty() ? "文件: -" : "文件: " + LyricUtils.trunc(name, 18)));
        titleLabel.text(Component.literal(track != null && track.getTitle() != null
                ? "歌曲: " + LyricUtils.trunc(track.getTitle(), 18) : "歌曲: -"));
        artistLabel.text(Component.literal(track != null && track.getArtist() != null
                ? "演唱: " + LyricUtils.trunc(track.getArtist(), 18) : "演唱: -"));

        long current = playbackController.getCurrentTimeMillis();
        long total = timelineManager.hasLyrics() ? lastTimestamp() : 0;
        progressLabel.text(Component.literal(
                "进度: " + LyricUtils.fmtTime(current) + " / " + LyricUtils.fmtTime(total)));
    }

    private String stateLabel() {
        return switch (playbackController.getState()) {
            case PLAYING -> "播放中";
            case PAUSED -> "已暂停";
            case STOPPED -> "已停止";
        };
    }

    private String playLabel() {
        return playbackController.isPlaying() ? "暂停" : "播放";
    }

    private String chatSendLabel() {
        return LyricLiveClient.isAutoSendEnabled() ? "自动发送: 开" : "自动发送: 关";
    }

    private void togglePlayPause() {
        if (playbackController.isPlaying()) {
            playbackController.pause();
        } else {
            playbackController.play();
        }
        refreshStaticLabels();
    }

    private void stopPlayback() {
        playbackController.stop();
        refreshStaticLabels();
    }

    private void toggleChatSending() {
        LyricLiveClient.setAutoSendEnabled(!LyricLiveClient.isAutoSendEnabled());
        refreshStaticLabels();
    }

    private void seek(long deltaMs) {
        playbackController.seek(deltaMs);
    }

    private void openSettings() {
        assert this.minecraft != null;
        this.minecraft.gui.setScreen(new SettingsScreen(displayConfig, chatSender, commandSender, this));
    }

    private void refreshFiles() {
        try {
            scanLyricDirectory();
            populateLyricList();
            statusMessage = discoveredLrcFiles.isEmpty() ? "lyriclive/ 下未找到 .lrc 文件" : "已刷新歌词列表";
        } catch (IOException e) {
            statusMessage = "读取目录失败: " + e.getMessage();
        }
        refreshStaticLabels();
    }

    private void scanLyricDirectory() throws IOException {
        Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("lyriclive");
        Files.createDirectories(dir);
        try (Stream<Path> stream = Files.list(dir)) {
            discoveredLrcFiles = stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".lrc"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
                    .toList();
        }
    }

    private void populateLyricList() {
        lyricList.clearChildren();
        for (Path path : discoveredLrcFiles) {
            String fileName = path.getFileName().toString();
            lyricList.child(UIComponents.button(Component.literal(fileName), button -> loadFile(path)));
        }
    }

    private void loadFile(Path path) {
        try {
            timelineManager.loadLyricFile(path);
            currentLyricFile = path;
            statusMessage = "已加载: " + path.getFileName();
        } catch (IOException e) {
            statusMessage = "加载失败: " + e.getMessage();
        }
        refreshStaticLabels();
    }

    private long lastTimestamp() {
        if (!timelineManager.hasLyrics()) return 0;
        var lyrics = timelineManager.getCurrentTrack().getLyrics();
        return lyrics.isEmpty() ? 0 : lyrics.get(lyrics.size() - 1).getTimestamp();
    }

    public void setCurrentLyricFile(Path file) {
        this.currentLyricFile = file;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
