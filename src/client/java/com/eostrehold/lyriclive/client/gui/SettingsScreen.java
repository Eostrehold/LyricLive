package com.eostrehold.lyriclive.client.gui;

import com.eostrehold.lyriclive.client.display.DisplayConfig;
import com.eostrehold.lyriclive.client.sender.ChatSender;
import com.eostrehold.lyriclive.client.sender.CommandSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {
    private static final int COL_LABEL = 16;
    private static final int COL_INPUT = 150;
    private static final int COL_NOTE  = 240;
    private static final int ROW_Y0   = 32;
    private static final int ROW_H    = 28;

    private final DisplayConfig displayConfig;
    private final ChatSender chatSender;
    private final CommandSender commandSender;
    private final Screen parent;

    private EditBox posXBox, posYBox, fontSizeBox, colorBox, opacityBox;
    private EditBox cmdTemplateBox;
    private Button shadowToggle, centeredToggle, fadeToggle;

    public SettingsScreen(DisplayConfig displayConfig, ChatSender chatSender,
                          CommandSender commandSender, Screen parent) {
        super(Component.literal("LyricLive Settings"));
        this.displayConfig = displayConfig;
        this.chatSender = chatSender;
        this.commandSender = commandSender;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        Font f = Minecraft.getInstance().font;

        // --- input fields ---
        posXBox        = box(f, 0, String.valueOf(displayConfig.getPositionX()));
        posYBox        = box(f, 1, String.valueOf(displayConfig.getPositionY()));
        fontSizeBox    = box(f, 2, String.valueOf(displayConfig.getFontSize()));
        colorBox       = box(f, 3, String.format("%06X", displayConfig.getFontColor()));
        opacityBox     = box(f, 4, String.valueOf(displayConfig.getOpacity()));
        cmdTemplateBox = box(f, 5, commandSender.getCommandTemplate());
        cmdTemplateBox.setMaxLength(100);

        // --- toggles ---
        int ty = ROW_Y0 + 7 * ROW_H + 6;
        shadowToggle   = tog(f, ty, "阴影", displayConfig.isShadowEnabled(), this::toggleShadow);
        centeredToggle = tog(f, ty, "居中", displayConfig.isCentered(),      this::toggleCentered);
        fadeToggle     = tog(f, ty, "渐变", displayConfig.isFadeInOutEnabled(), this::toggleFade);

        // done
        addRenderableWidget(Button.builder(Component.literal("保存并返回"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    // ---- render: 背景 + 控件 + 标签 ----
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        Font f = Minecraft.getInstance().font;

        // 背景
        g.fill(0, 0, this.width, this.height, 0xC0101010);

        // 标题
        String title = "LyricLive Settings";
        g.text(f, title, this.width / 2 - f.width(title) / 2, 8, C_YELLOW, true);

        // 控件
        super.extractRenderState(g, mx, my, pt);

        // 标签
        for (int i = 0; i < 6; i++) {
            int y = ROW_Y0 + i * ROW_H + 5;
            g.text(f, LABELS[i], COL_LABEL, y, C_WHITE, true);
            int nx = COL_NOTE;
            int nw = f.width(NOTES[i]);
            g.fill(nx - 2, y - 1, nx + nw + 4, y + f.lineHeight + 2, 0xAA333333);
            g.text(f, NOTES[i], nx, y, C_GRAY, false);
        }
    }

    // ---- helpers ----

    private EditBox box(Font f, int row, String val) {
        EditBox b = new EditBox(f, COL_INPUT, ROW_Y0 + row * ROW_H, 50, 18, Component.empty());
        b.setValue(val);
        addRenderableWidget(b);
        return b;
    }

    private Button tog(Font f, int baseY, String label, boolean on, Runnable action) {
        int idx = 0;
        if (label.equals("居中")) idx = 1;
        if (label.equals("渐变")) idx = 2;
        int x = COL_LABEL + idx * 90;
        Button b = Button.builder(Component.literal(label + (on ? ": ON" : ": OFF")), btn -> action.run())
                .bounds(x, baseY, 80, 20).build();
        addRenderableWidget(b);
        return b;
    }

    private static final String[] LABELS = {
        "X Position", "Y Position", "Font Size", "Font Color", "Opacity", "Cmd Template"
    };
    private static final String[] NOTES = {
        "0.0~1.0  0.5=center", "0.0~1.0  0.8=bottom",
        "8~64 px", "hex  FFFFFF=white", "0.0~1.0  1.0=opaque",
        "{lyric} = lyric text"
    };
    private static final int C_WHITE  = 0xFFFFFFFF;
    private static final int C_YELLOW = 0xFFFFFF55;
    private static final int C_GRAY   = 0xFFCCCCCC;

    private void toggleShadow() {
        displayConfig.setShadowEnabled(!displayConfig.isShadowEnabled());
        shadowToggle.setMessage(Component.literal("阴影: " + (displayConfig.isShadowEnabled() ? "ON" : "OFF")));
    }
    private void toggleCentered() {
        displayConfig.setCentered(!displayConfig.isCentered());
        centeredToggle.setMessage(Component.literal("居中: " + (displayConfig.isCentered() ? "ON" : "OFF")));
    }
    private void toggleFade() {
        displayConfig.setFadeInOutEnabled(!displayConfig.isFadeInOutEnabled());
        fadeToggle.setMessage(Component.literal("渐变: " + (displayConfig.isFadeInOutEnabled() ? "ON" : "OFF")));
    }

    @Override
    public void onClose() {
        saveSettings();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    private void saveSettings() {
        try {
            displayConfig.setPositionX(Float.parseFloat(posXBox.getValue()));
            displayConfig.setPositionY(Float.parseFloat(posYBox.getValue()));
            displayConfig.setFontSize(Integer.parseInt(fontSizeBox.getValue()));
            displayConfig.setFontColor(Integer.parseInt(colorBox.getValue().replace("#", ""), 16));
            displayConfig.setOpacity(Float.parseFloat(opacityBox.getValue()));
            commandSender.setCommandTemplate(cmdTemplateBox.getValue());
        } catch (NumberFormatException e) { /* ignore */ }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}