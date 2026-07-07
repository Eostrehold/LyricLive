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

        int col1 = 16;  // 标签列
        int col2 = 150; // 输入框列
        int col3 = 220; // 备注列
        int y0 = 30;
        int rh = 26;

        posXBox     = newBox(f, col2, y0, 50, 20, String.valueOf(displayConfig.getPositionX()));
        posYBox     = newBox(f, col2, y0 + rh, 50, 20, String.valueOf(displayConfig.getPositionY()));
        fontSizeBox = newBox(f, col2, y0 + 2*rh, 50, 20, String.valueOf(displayConfig.getFontSize()));
        colorBox    = newBox(f, col2, y0 + 3*rh, 60, 20, String.format("%06X", displayConfig.getFontColor()));
        opacityBox  = newBox(f, col2, y0 + 4*rh, 50, 20, String.valueOf(displayConfig.getOpacity()));
        cmdTemplateBox = newBox(f, col2, y0 + 5*rh, 160, 20, commandSender.getCommandTemplate());
        cmdTemplateBox.setMaxLength(100);

        int toggleY = y0 + 7 * rh;
        shadowToggle   = newToggle(col1,      toggleY, "阴影", displayConfig.isShadowEnabled(), this::toggleShadow);
        centeredToggle = newToggle(col1 + 90,  toggleY, "居中", displayConfig.isCentered(), this::toggleCentered);
        fadeToggle     = newToggle(col1 + 180, toggleY, "渐变", displayConfig.isFadeInOutEnabled(), this::toggleFade);

        Button doneBtn = Button.builder(Component.literal("保存并返回"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build();
        addRenderableWidget(doneBtn);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        Font f = Minecraft.getInstance().font;

        // 背景
        g.fill(0, 0, this.width, this.height, 0xC0101010);

        // 标题
        String title = "LyricLive Settings";
        g.text(f, title, this.width / 2 - f.width(title) / 2, 8, 0xFFFF55, true);

        // 控件绘制
        super.extractRenderState(g, mx, my, pt);

        // ---- 标签 ----
        int col1 = 16, col3 = 220;
        int y0 = 30, rh = 26;

        int row = 0;
        drawLabel(g, f, "X Position (0.0~1.0)", col1, y0 + row * rh);
        drawNote(g, f, "0.5=center", col3, y0 + row * rh);
        row++;

        drawLabel(g, f, "Y Position (0.0~1.0)", col1, y0 + row * rh);
        drawNote(g, f, "0.8=bottom", col3, y0 + row * rh);
        row++;

        drawLabel(g, f, "Font Size (8~64)", col1, y0 + row * rh);
        drawNote(g, f, "pixel", col3, y0 + row * rh);
        row++;

        drawLabel(g, f, "Font Color (RGB hex)", col1, y0 + row * rh);
        drawNote(g, f, "FFFFFF=white", col3, y0 + row * rh);
        row++;

        drawLabel(g, f, "Opacity (0.0~1.0)", col1, y0 + row * rh);
        drawNote(g, f, "1.0=opaque", col3, y0 + row * rh);
        row++;

        drawLabel(g, f, "Command Template", col1, y0 + row * rh);
        drawNote(g, f, "{lyric}=lyrics", col3, y0 + row * rh);
    }

    private void drawLabel(GuiGraphicsExtractor g, Font f, String text, int x, int y) {
        g.text(f, text, x, y + 5, 0xFFFFFF, true);
    }

    private void drawNote(GuiGraphicsExtractor g, Font f, String text, int x, int y) {
        g.text(f, text, x, y + 5, 0x888888, true);
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