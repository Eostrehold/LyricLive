package com.eostrehold.lyriclive.client;

import com.eostrehold.lyriclive.LyricLive;
import com.eostrehold.lyriclive.client.core.PlaybackController;
import com.eostrehold.lyriclive.client.core.TimelineManager;
import com.eostrehold.lyriclive.client.display.DisplayConfig;
import com.eostrehold.lyriclive.client.display.LyricRenderer;
import com.eostrehold.lyriclive.client.gui.MainScreen;
import com.eostrehold.lyriclive.client.sender.ChatSender;
import com.eostrehold.lyriclive.client.sender.CommandSender;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LyricLiveClient implements ClientModInitializer {
    private static PlaybackController playbackController;
    private static TimelineManager timelineManager;
    private static DisplayConfig displayConfig;
    private static LyricRenderer lyricRenderer;
    private static ChatSender chatSender;
    private static CommandSender commandSender;

    private static MainScreen mainScreen;

    private static KeyMapping openGuiKey;
    private static KeyMapping togglePlayPauseKey;
    private static KeyMapping stopKey;
    private static KeyMapping sendLyricKey;
    private static KeyMapping toggleAutoSendKey;

    private static Path currentLyricFile;

    private static KeyMapping.Category CATEGORY;

    @Override
    public void onInitializeClient() {
        LyricLive.LOGGER.info("LyricLive 客户端初始化开始");

        playbackController = new PlaybackController();
        timelineManager = new TimelineManager(playbackController);
        displayConfig = new DisplayConfig();
        chatSender = new ChatSender();
        commandSender = new CommandSender();
        lyricRenderer = new LyricRenderer(timelineManager, displayConfig);

        mainScreen = new MainScreen(playbackController, timelineManager, lyricRenderer, chatSender, commandSender, displayConfig);

        CATEGORY = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(LyricLive.MOD_ID, "keys")
        );

        registerKeyBindings();
        registerHudRenderer();
        registerClientTick();

        LyricLive.LOGGER.info("LyricLive 客户端初始化完成");
    }

    private void registerKeyBindings() {
        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyriclive.open_gui",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                CATEGORY
        ));

        togglePlayPauseKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyriclive.toggle_play_pause",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                CATEGORY
        ));

        stopKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyriclive.stop",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                CATEGORY
        ));

        sendLyricKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyriclive.send_lyric",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
        ));

        toggleAutoSendKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyriclive.toggle_auto_send",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));

        LyricLive.LOGGER.info("快捷键注册完成");
    }

    private void registerHudRenderer() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(LyricLive.MOD_ID, "lyric_display"),
                (context, deltaTracker) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.player != null && client.screen == null) {
                        lyricRenderer.render(context);
                    }
                }
        );
        LyricLive.LOGGER.info("HUD 渲染器注册完成");
    }

    private void registerClientTick() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeyBindings(client);
            handleAutoLyricSending();
        });
        LyricLive.LOGGER.info("客户端 Tick 回调注册完成");
    }

    private void handleKeyBindings(Minecraft client) {
        while (openGuiKey.consumeClick()) {
            if (client.screen == null) {
                client.setScreen(mainScreen);
            }
        }

        while (togglePlayPauseKey.consumeClick()) {
            if (playbackController.isPlaying()) {
                playbackController.pause();
            } else {
                playbackController.play();
            }
        }

        while (stopKey.consumeClick()) {
            playbackController.stop();
        }

        while (sendLyricKey.consumeClick()) {
            String currentLyric = timelineManager.getCurrentLyricText();
            if (currentLyric != null && !currentLyric.isEmpty()) {
                chatSender.forceSendLyric(currentLyric);
            }
        }

        while (toggleAutoSendKey.consumeClick()) {
            boolean newState = !chatSender.isEnabled();
            chatSender.setEnabled(newState);
        }
    }

    private void handleAutoLyricSending() {
        if (!playbackController.isPlaying()) {
            return;
        }

        if (chatSender.isEnabled()) {
            String currentLyric = timelineManager.getCurrentLyricText();
            if (currentLyric != null && !currentLyric.isEmpty()) {
                chatSender.sendCurrentLyric(currentLyric);
            }
        }

        if (commandSender.isEnabled()) {
            String currentLyric = timelineManager.getCurrentLyricText();
            if (currentLyric != null && !currentLyric.isEmpty()) {
                commandSender.sendCurrentLyric(currentLyric);
            }
        }
    }

    public static PlaybackController getPlaybackController() {
        return playbackController;
    }

    public static TimelineManager getTimelineManager() {
        return timelineManager;
    }

    public static DisplayConfig getDisplayConfig() {
        return displayConfig;
    }

    public static LyricRenderer getLyricRenderer() {
        return lyricRenderer;
    }

    public static ChatSender getChatSender() {
        return chatSender;
    }

    public static CommandSender getCommandSender() {
        return commandSender;
    }

    public static MainScreen getMainScreen() {
        return mainScreen;
    }

    public static void loadLyricFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            timelineManager.loadLyricFile(path);
            currentLyricFile = path;
            mainScreen.setCurrentLyricFile(path);
            LyricLive.LOGGER.info("歌词文件已加载: {}", filePath);
        } catch (IOException e) {
            LyricLive.LOGGER.error("加载歌词文件失败: {}", filePath, e);
        }
    }

    public static Path getCurrentLyricFile() {
        return currentLyricFile;
    }
}