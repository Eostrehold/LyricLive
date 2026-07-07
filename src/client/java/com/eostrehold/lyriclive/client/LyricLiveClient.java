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

    private static int manualLyricIndex = -1;

    @Override
    public void onInitializeClient() {
        playbackController = new PlaybackController();
        timelineManager = new TimelineManager(playbackController);
        displayConfig = new DisplayConfig();
        chatSender = new ChatSender();
        commandSender = new CommandSender();
        lyricRenderer = new LyricRenderer(timelineManager, playbackController, chatSender, displayConfig, () -> manualLyricIndex);

        mainScreen = new MainScreen(playbackController, timelineManager, lyricRenderer, chatSender, commandSender, displayConfig);

        CATEGORY = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(LyricLive.MOD_ID, "keys"));

        registerKeyBindings();
        registerHudRenderer();
        registerClientTick();
    }

    private void registerKeyBindings() {
        openGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyriclive.open_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_L, CATEGORY));
        togglePlayPauseKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyriclive.toggle_play_pause", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, CATEGORY));
        stopKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyriclive.stop", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_O, CATEGORY));
        sendLyricKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyriclive.send_lyric", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
        toggleAutoSendKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.lyriclive.toggle_auto_send", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));
    }

    private void registerHudRenderer() {
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(LyricLive.MOD_ID, "lyric_display"),
                (context, dt) -> {
                    if (Minecraft.getInstance().player != null && Minecraft.getInstance().screen == null) {
                        lyricRenderer.render(context);
                    }
                });
    }

    private void registerClientTick() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            handleKeyBindings(client);
            handleAutoLyricSending();
        });
    }

    private void handleKeyBindings(Minecraft client) {
        while (openGuiKey.consumeClick()) {
            if (client.screen == null) client.setScreen(mainScreen);
        }
        while (togglePlayPauseKey.consumeClick()) {
            if (playbackController.isPlaying()) playbackController.pause();
            else playbackController.play();
        }
        while (stopKey.consumeClick()) {
            playbackController.stop();
            manualLyricIndex = -1;
        }
        while (sendLyricKey.consumeClick()) {
            manualSendCurrentLyric();
        }
        while (toggleAutoSendKey.consumeClick()) {
            chatSender.setEnabled(!chatSender.isEnabled());
        }
    }

    private void manualSendCurrentLyric() {
        if (!timelineManager.hasLyrics()) return;
        var lyrics = timelineManager.getCurrentTrack().getLyrics();
        if (lyrics.isEmpty()) return;

        if (manualLyricIndex < 0) {
            manualLyricIndex = timelineManager.getCurrentLyricIndex();
        }
        if (manualLyricIndex < 0) manualLyricIndex = 0;

        String text = lyrics.get(manualLyricIndex).getText();
        if (text != null && !text.isEmpty()) {
            chatSender.forceSendLyric(text);
        }

        manualLyricIndex++;
        if (manualLyricIndex >= lyrics.size()) {
            manualLyricIndex = lyrics.size() - 1;
        }
    }

    private void handleAutoLyricSending() {
        if (!playbackController.isPlaying()) return;

        if (chatSender.isEnabled()) {
            String cur = timelineManager.getCurrentLyricText();
            if (cur != null && !cur.isEmpty()) chatSender.sendCurrentLyric(cur);
        }
        if (commandSender.isEnabled()) {
            String cur = timelineManager.getCurrentLyricText();
            if (cur != null && !cur.isEmpty()) commandSender.sendCurrentLyric(cur);
        }
    }

    public static int getManualLyricIndex() { return manualLyricIndex; }
    public static PlaybackController getPlaybackController() { return playbackController; }
    public static TimelineManager getTimelineManager() { return timelineManager; }
    public static DisplayConfig getDisplayConfig() { return displayConfig; }
    public static LyricRenderer getLyricRenderer() { return lyricRenderer; }
    public static ChatSender getChatSender() { return chatSender; }
    public static CommandSender getCommandSender() { return commandSender; }
    public static MainScreen getMainScreen() { return mainScreen; }

    public static void loadLyricFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            timelineManager.loadLyricFile(path);
            currentLyricFile = path;
            manualLyricIndex = -1;
            mainScreen.setCurrentLyricFile(path);
        } catch (IOException e) {
            LyricLive.LOGGER.error("加载歌词文件失败: {}", filePath, e);
        }
    }

    public static Path getCurrentLyricFile() { return currentLyricFile; }
}