package com.eostrehold.lyriclive.client.gui;

import com.eostrehold.lyriclive.client.core.PlaybackController;
import com.eostrehold.lyriclive.client.core.TimelineManager;
import com.eostrehold.lyriclive.client.display.DisplayConfig;
import com.eostrehold.lyriclive.client.display.LyricRenderer;
import com.eostrehold.lyriclive.client.lrc.LrcLyric;
import com.eostrehold.lyriclive.client.sender.ChatSender;
import com.eostrehold.lyriclive.client.sender.CommandSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class MainScreen extends Screen {
    private final PlaybackController playbackController;
    private final TimelineManager timelineManager;
    private final LyricRenderer lyricRenderer;
    private final ChatSender chatSender;
    private final CommandSender commandSender;
    private final DisplayConfig displayConfig;

    private Button playPauseButton;
    private Button stopButton;
    private Button chatSendButton;
    private Button commandSendButton;
    private Button settingsButton;
    private Button loadLyricButton;
    private Button reloadLyricButton;
    private Button seekBackwardTenSecondsButton;
    private Button seekBackwardOneSecondButton;
    private Button seekForwardOneSecondButton;
    private Button seekForwardTenSecondsButton;

    private Path currentLyricFile;
    private List<Path> discoveredLyricFiles = new ArrayList<>();
    private int selectedLyricFileIndex = -1;
    private String statusMessage = "请将 .lrc 文件放入游戏目录的 lyriclive 文件夹";

    public MainScreen(PlaybackController playbackController, TimelineManager timelineManager,
                      LyricRenderer lyricRenderer, ChatSender chatSender, CommandSender commandSender,
                      DisplayConfig displayConfig) {
        super(Component.literal("LyricLive"));
        this.playbackController = playbackController;
        this.timelineManager = timelineManager;
        this.lyricRenderer = lyricRenderer;
        this.chatSender = chatSender;
        this.commandSender = commandSender;
        this.displayConfig = displayConfig;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 120;
        int buttonHeight = 20;
        int startX = (this.width - buttonWidth * 4) / 2;
        int startY = this.height - 60;

        playPauseButton = Button.builder(
                Component.literal(playbackController.isPlaying() ? "暂停" : "播放"),
                button -> togglePlayPause()
        ).bounds(startX, startY, buttonWidth, buttonHeight).build();

        stopButton = Button.builder(
                Component.literal("停止"),
                button -> stopPlayback()
        ).bounds(startX + buttonWidth + 10, startY, buttonWidth, buttonHeight).build();

        chatSendButton = Button.builder(
                Component.literal(chatSender.isEnabled() ? "聊天发送: 开" : "聊天发送: 关"),
                button -> toggleChatSending()
        ).bounds(startX + 2 * (buttonWidth + 10), startY, buttonWidth, buttonHeight).build();

        commandSendButton = Button.builder(
                Component.literal(commandSender.isEnabled() ? "指令发送: 开" : "指令发送: 关"),
                button -> toggleCommandSending()
        ).bounds(startX + 3 * (buttonWidth + 10), startY, buttonWidth, buttonHeight).build();

        loadLyricButton = Button.builder(
                Component.literal("加载歌词"),
                button -> openLyricFile()
        ).bounds(this.width / 2 - 130, 30, 120, 20).build();

        reloadLyricButton = Button.builder(
                Component.literal("重新加载"),
                button -> reloadLyrics()
        ).bounds(this.width / 2 + 10, 30, 120, 20).build();

        settingsButton = Button.builder(
                Component.literal("设置"),
                button -> openSettings()
        ).bounds(this.width - 60, 10, 50, 20).build();

        int seekButtonY = startY - 25;
        seekBackwardTenSecondsButton = Button.builder(
                Component.literal("-10s"),
                button -> adjustPlaybackTime(-10_000L)
        ).bounds(startX, seekButtonY, 55, buttonHeight).build();

        seekBackwardOneSecondButton = Button.builder(
                Component.literal("-1s"),
                button -> adjustPlaybackTime(-1_000L)
        ).bounds(startX + 65, seekButtonY, 55, buttonHeight).build();

        seekForwardOneSecondButton = Button.builder(
                Component.literal("+1s"),
                button -> adjustPlaybackTime(1_000L)
        ).bounds(startX + 130, seekButtonY, 55, buttonHeight).build();

        seekForwardTenSecondsButton = Button.builder(
                Component.literal("+10s"),
                button -> adjustPlaybackTime(10_000L)
        ).bounds(startX + 195, seekButtonY, 55, buttonHeight).build();

        addRenderableWidget(playPauseButton);
        addRenderableWidget(stopButton);
        addRenderableWidget(chatSendButton);
        addRenderableWidget(commandSendButton);
        addRenderableWidget(loadLyricButton);
        addRenderableWidget(reloadLyricButton);
        addRenderableWidget(settingsButton);
        addRenderableWidget(seekBackwardTenSecondsButton);
        addRenderableWidget(seekBackwardOneSecondButton);
        addRenderableWidget(seekForwardOneSecondButton);
        addRenderableWidget(seekForwardTenSecondsButton);

        addLyricFileButtons();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Font minecraftFont = Minecraft.getInstance().font;

        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);

        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        String titleStr = this.title.getString();
        int titleWidth = minecraftFont.width(titleStr);
        guiGraphics.text(minecraftFont, titleStr, (this.width - titleWidth) / 2, 10, 0xFFFFFF, true);

        if (timelineManager.hasLyrics()) {
            List<LrcLyric> lyricContext = timelineManager.getCurrentLyricContext(2);
            LrcLyric currentLyric = timelineManager.getCurrentLyric();
            int contextStartY = 60;
            int lineHeight = 11;
            for (int contextIndex = 0; contextIndex < lyricContext.size(); contextIndex++) {
                LrcLyric contextLyric = lyricContext.get(contextIndex);
                boolean isCurrentLine = contextLyric.equals(currentLyric);
                int lyricColor = isCurrentLine ? 0xFFFFFF : 0xAAAAAA;
                String lyricText = contextLyric.getText();
                int lyricWidth = minecraftFont.width(lyricText);
                guiGraphics.text(minecraftFont, lyricText, (this.width - lyricWidth) / 2, contextStartY + contextIndex * lineHeight, lyricColor, true);
            }

            String progress = String.format("歌词进度: %d/%d",
                    timelineManager.getCurrentLyricIndex() + 1,
                    timelineManager.getLyricCount());
            guiGraphics.text(minecraftFont, progress, 10, 50, 0xFFFFFF, true);

            String timeStr = formatTime(playbackController.getCurrentTimeMillis());
            guiGraphics.text(minecraftFont, "时间: " + timeStr, 10, 70, 0xFFFFFF, true);
        } else {
            String noLyric = "未加载歌词文件";
            int noLyricWidth = minecraftFont.width(noLyric);
            guiGraphics.text(minecraftFont, noLyric, (this.width - noLyricWidth) / 2, 80, 0xAAAAAA, true);
        }

        if (currentLyricFile != null) {
            String fileText = "文件: " + currentLyricFile.getFileName();
            guiGraphics.text(minecraftFont, fileText, 10, 110, 0xFFFFFF, true);
        }

        if (statusMessage != null && !statusMessage.isEmpty()) {
            guiGraphics.text(minecraftFont, statusMessage, 10, 130, 0xAAAAAA, true);
        }

        String stateText = switch (playbackController.getState()) {
            case PLAYING -> "播放中";
            case PAUSED -> "已暂停";
            case STOPPED -> "已停止";
        };
        guiGraphics.text(minecraftFont, "状态: " + stateText, 10, 90, 0xFFFFFF, true);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void togglePlayPause() {
        if (playbackController.isPlaying()) {
            playbackController.pause();
            playPauseButton.setMessage(Component.literal("播放"));
        } else {
            playbackController.play();
            playPauseButton.setMessage(Component.literal("暂停"));
        }
    }

    private void stopPlayback() {
        playbackController.stop();
        playPauseButton.setMessage(Component.literal("播放"));
    }

    private void toggleChatSending() {
        boolean newState = !chatSender.isEnabled();
        chatSender.setEnabled(newState);
        chatSendButton.setMessage(Component.literal(newState ? "聊天发送: 开" : "聊天发送: 关"));
    }

    private void toggleCommandSending() {
        boolean newState = !commandSender.isEnabled();
        commandSender.setEnabled(newState);
        commandSendButton.setMessage(Component.literal(newState ? "指令发送: 开" : "指令发送: 关"));
    }

    private void openLyricFile() {
        try {
            refreshLyricFiles();
            if (discoveredLyricFiles.isEmpty()) {
                statusMessage = "未找到歌词：请放入 lyriclive/*.lrc";
                return;
            }
            statusMessage = "已刷新歌词列表，请点击下方文件名加载";
            Minecraft.getInstance().setScreen(this);
        } catch (IOException exception) {
            statusMessage = "加载歌词失败: " + exception.getMessage();
            exception.printStackTrace();
        }
    }

    private void addLyricFileButtons() {
        try {
            refreshLyricFiles();
        } catch (IOException exception) {
            statusMessage = "读取歌词目录失败: " + exception.getMessage();
            exception.printStackTrace();
            return;
        }

        int visibleFileCount = Math.min(discoveredLyricFiles.size(), 6);
        int fileButtonX = this.width / 2 - 130;
        int fileButtonY = 55;
        int fileButtonWidth = 260;
        for (int fileIndex = 0; fileIndex < visibleFileCount; fileIndex++) {
            Path lyricFile = discoveredLyricFiles.get(fileIndex);
            Button fileButton = Button.builder(
                    Component.literal(lyricFile.getFileName().toString()),
                    button -> loadSelectedLyricFile(lyricFile)
            ).bounds(fileButtonX, fileButtonY + fileIndex * 21, fileButtonWidth, 18).build();
            addRenderableWidget(fileButton);
        }

        if (discoveredLyricFiles.size() > visibleFileCount) {
            statusMessage = "仅显示前 " + visibleFileCount + " 个歌词文件，请按文件名排序选择";
        }
    }

    private void loadSelectedLyricFile(Path selectedLyricFile) {
        try {
            timelineManager.loadLyricFile(selectedLyricFile);
            currentLyricFile = selectedLyricFile;
            selectedLyricFileIndex = discoveredLyricFiles.indexOf(selectedLyricFile);
            statusMessage = "已加载: " + selectedLyricFile.getFileName();
        } catch (IOException exception) {
            statusMessage = "加载歌词失败: " + exception.getMessage();
            exception.printStackTrace();
        }
    }

    private void refreshLyricFiles() throws IOException {
        Path lyricDirectory = Minecraft.getInstance().gameDirectory.toPath().resolve("lyriclive");
        Files.createDirectories(lyricDirectory);

        try (Stream<Path> lyricPathStream = Files.list(lyricDirectory)) {
            discoveredLyricFiles = lyricPathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".lrc"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .toList();
        }
    }

    private void reloadLyrics() {
        if (currentLyricFile != null) {
            try {
                timelineManager.reloadLyrics(currentLyricFile);
                statusMessage = "已重新加载: " + currentLyricFile.getFileName();
            } catch (Exception e) {
                statusMessage = "重新加载失败: " + e.getMessage();
                e.printStackTrace();
            }
        }
    }

    private void adjustPlaybackTime(long deltaMillis) {
        playbackController.seek(deltaMillis);
        statusMessage = "当前时间: " + formatTime(playbackController.getCurrentTimeMillis());
    }

    private void openSettings() {
        Minecraft.getInstance().setScreen(new SettingsScreen(displayConfig, chatSender, commandSender, this));
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void setCurrentLyricFile(Path file) {
        this.currentLyricFile = file;
    }
}
