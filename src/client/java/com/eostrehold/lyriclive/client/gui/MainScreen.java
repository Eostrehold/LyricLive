package com.eostrehold.lyriclive.client.gui;

import com.eostrehold.lyriclive.client.core.PlaybackController;
import com.eostrehold.lyriclive.client.core.TimelineManager;
import com.eostrehold.lyriclive.client.display.DisplayConfig;
import com.eostrehold.lyriclive.client.display.LyricRenderer;
import com.eostrehold.lyriclive.client.sender.ChatSender;
import com.eostrehold.lyriclive.client.sender.CommandSender;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;

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

    private Path currentLyricFile;

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

        addRenderableWidget(playPauseButton);
        addRenderableWidget(stopButton);
        addRenderableWidget(chatSendButton);
        addRenderableWidget(commandSendButton);
        addRenderableWidget(loadLyricButton);
        addRenderableWidget(reloadLyricButton);
        addRenderableWidget(settingsButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractRenderState(guiGraphics, mouseX, mouseY);

        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);

        String titleStr = this.title.getString();
        int titleWidth = this.font.width(titleStr);
        guiGraphics.text(this.font, titleStr, (this.width - titleWidth) / 2, 10, 0xFFFFFF, true);

        if (timelineManager.hasLyrics()) {
            String currentLyric = timelineManager.getCurrentLyricText();
            if (currentLyric != null && !currentLyric.isEmpty()) {
                int lyricWidth = this.font.width(currentLyric);
                guiGraphics.text(this.font, currentLyric, (this.width - lyricWidth) / 2, 80, 0xFFFFFF, true);
            }

            String progress = String.format("歌词进度: %d/%d",
                    timelineManager.getCurrentLyricIndex() + 1,
                    timelineManager.getLyricCount());
            guiGraphics.text(this.font, progress, 10, 50, 0xFFFFFF, true);

            String timeStr = formatTime(playbackController.getCurrentTimeMillis());
            guiGraphics.text(this.font, "时间: " + timeStr, 10, 70, 0xFFFFFF, true);
        } else {
            String noLyric = "未加载歌词文件";
            int noLyricWidth = this.font.width(noLyric);
            guiGraphics.text(this.font, noLyric, (this.width - noLyricWidth) / 2, 80, 0xAAAAAA, true);
        }

        String stateText = switch (playbackController.getState()) {
            case PLAYING -> "播放中";
            case PAUSED -> "已暂停";
            case STOPPED -> "已停止";
        };
        guiGraphics.text(this.font, "状态: " + stateText, 10, 90, 0xFFFFFF, true);
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
        // TODO: 实现文件选择对话框
    }

    private void reloadLyrics() {
        if (currentLyricFile != null) {
            try {
                timelineManager.reloadLyrics(currentLyricFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void openSettings() {
        // TODO: 打开设置界面
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