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

        int col1 = 16;
        int col2 = 150;
        int y0 = 32;
        int rh = 30;

        posXBox     = newBox(f, col2, y0, 50, 18, String.valueOf(displayConfig.getPositionX()));
        posYBox     = newBox(f, col2, y0 + rh, 50, 18, String.valueOf(displayConfig.getPositionY()));
        fontSizeBox = newBox(f, col2, y0 + 2*rh, 50, 18, String.valueOf(displayConfig.getFontSize()));
        colorBox    = newBox(f, col2, y0 + 3*rh, 60, 18, String.format("%06X", displayConfig.getFontColor()));
        opacityBox  = newBox(f, col2, y0 + 4*rh, 50, 18, String.valueOf(displayConfig.getOpacity()));
        cmdTemplateBox = newBox(f, col2, y0 + 5*rh, 160, 18, commandSender.getCommandTemplate());
        cmdTemplateBox.setMaxLength(100);

        int toggleY = y0 + 7 * rh + 4;
        shadowToggle   = newToggle(col1,      toggleY, "阴影", displayConfig.isShadowEnabled(), this::toggleShadow);
        centeredToggle = newToggle(col1 + 100, toggleY, "居中", displayConfig.isCentered(), this::toggleCentered);
        fadeToggle     = newToggle(col1 + 200, toggleY, "渐变", displayConfig.isFadeInOutEnabled(), this::toggleFade);

        Button doneBtn = Button.builder(Component.literal("保存并返回"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build();
        addRenderableWidget(doneBtn);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        Font f = Minecraft.getInstance().font;
        int sw = this.width, sh = this.height;

        // 全屏半透明黑底
        g.fill(0, 0, sw, sh, 0xC0101010);

        // 标题
        String title = "LyricLive Settings";
        g.text(f, title, sw / 2 - f.width(title) / 2, 8, 0xFFFF55, true);

        // 控件（输入框、按钮）
        super.extractRenderState(g, mx, my, pt);

        // ---- 标签行 ----
        int col1 = 16, col2 = 150, y0 = 32, rh = 30;

        int row = 0;
        drawRow(g, f, "X Position (0.0~1.0)",      "屏幕宽度比例",    col1, y0 + row * rh);
        drawRow(g, f, "Y Position (0.0~1.0)",      "屏幕高度比例",    col1, y0 + ++row * rh);
        drawRow(g, f, "Font Size (8~64)",          "像素大小",        col1, y0 + ++row * rh);
        drawRow(g, f, "Font Color (RGB hex)",      "FFFFFF=白色",     col1, y0 + ++row * rh);
        drawRow(g, f, "Opacity (0.0~1.0)",         "1.0=完全不透明",  col1, y0 + ++row * rh);
        drawRow(g, f, "Command Template",          "{lyric}=歌词文本", col1, y0 + ++row * rh);
    }

    /**
     * 绘制一行：左侧白字标签 + 右侧带深色背景的说明文字
     */
    private void drawRow(GuiGraphicsExtractor g, Font f, String label, String note, int x, int y) {
        // 左侧标签（白字）
        g.text(f, label, x, y + 6, 0xFFFFFF, true);

        // 右侧说明文字（带深色背景）
        int noteX = 220;
        int noteY = y + 6;
        int noteW = f.width(note) + 8;
        int noteH = f.lineHeight + 4;
        g.fill(noteX - 2, noteY - 1, noteX + noteW - 4, noteY + noteH - 2, 0xAA222222);
        g.text(f, note, noteX + 2, noteY + 1, 0xBBBBBB, false);
    }

    private EditBox newBox(Font f, int x, int y, int w, int h, String val) {
        EditBox box = new EditBox(f, x, y, w, h, Component.empty());
        box.setValue(val);
        addRenderableWidget(box);
        return box;
    }

    private Button newToggle(int x, int y, String label, boolean value, Runnable action) {
        Button b = Button.builder(Component.literal(label + ": " + (value ? "ON" : "OFF")), btn -> action.run())
                .bounds(x, y, 80, 20).build();
        addRenderableWidget(b);
        return b;
    }

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
        } catch (NumberFormatException e) {
            // ignore invalid input
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}