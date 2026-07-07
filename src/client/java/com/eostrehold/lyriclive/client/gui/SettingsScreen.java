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
    private static final int BTN_W = 80;
    private static final int BTN_H = 20;
    private static final int EDIT_W = 60;
    private static final int EDIT_H = 20;
    private static final int LABEL_X = 12;
    private static final int EDIT_X  = 130;
    private static final int NOTE_X  = 200;
    private static final int START_Y = 30;
    private static final int ROW_H   = 24;

    private final DisplayConfig displayConfig;
    private final ChatSender chatSender;
    private final CommandSender commandSender;
    private final Screen parent;

    private EditBox posXBox, posYBox, fontSizeBox, colorBox, opacityBox;
    private EditBox cmdTemplateBox;
    private Button shadowToggle, centeredToggle, fadeToggle;
    private Button doneButton;

    public SettingsScreen(DisplayConfig displayConfig, ChatSender chatSender,
                          CommandSender commandSender, Screen parent) {
        super(Component.literal("LyricLive · 设置"));
        this.displayConfig = displayConfig;
        this.chatSender = chatSender;
        this.commandSender = commandSender;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        Font f = Minecraft.getInstance().font;

        int y = START_Y;

        posXBox     = addEditBox(f, EDIT_X, y, EDIT_W, EDIT_H, String.valueOf(displayConfig.getPositionX()));
        y += ROW_H;
        posYBox     = addEditBox(f, EDIT_X, y, EDIT_W, EDIT_H, String.valueOf(displayConfig.getPositionY()));
        y += ROW_H;
        fontSizeBox = addEditBox(f, EDIT_X, y, EDIT_W, EDIT_H, String.valueOf(displayConfig.getFontSize()));
        y += ROW_H;
        colorBox    = addEditBox(f, EDIT_X, y, EDIT_W, EDIT_H, String.format("%06X", displayConfig.getFontColor()));
        y += ROW_H;
        opacityBox  = addEditBox(f, EDIT_X, y, EDIT_W, EDIT_H, String.valueOf(displayConfig.getOpacity()));
        y += ROW_H;
        cmdTemplateBox = addEditBox(f, EDIT_X, y, 150, EDIT_H, commandSender.getCommandTemplate());
        cmdTemplateBox.setMaxLength(100);
        y += ROW_H + 4;

        shadowToggle  = addToggle(LABEL_X,            y, "文字阴影", displayConfig.isShadowEnabled(), this::toggleShadow);
        centeredToggle = addToggle(LABEL_X + BTN_W + 8, y, "居中显示", displayConfig.isCentered(),     this::toggleCentered);
        fadeToggle    = addToggle(LABEL_X + 2 * (BTN_W + 8), y, "淡入淡出", displayConfig.isFadeInOutEnabled(), this::toggleFade);

        doneButton = Button.builder(Component.literal("保存并返回"), btn -> onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, BTN_H).build();
        addRenderableWidget(doneButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        Font f = Minecraft.getInstance().font;
        g.fill(0, 0, this.width, this.height, 0x90000000);
        super.extractRenderState(g, mx, my, pt);
        g.text(f, "LyricLive · 设置", (this.width - f.width("LyricLive · 设置")) / 2, 8, 0xFFFF55, true);

        int y = START_Y;
        g.text(f, "HUD 歌词显示位置 (0.0~1.0)", LABEL_X, y + 5, 0xAAAAAA, true);
        g.text(f, "X 位置", LABEL_X,     y + 5, 0xFFFFFF, true);
        drawNote(g, f, "屏幕宽度比例，0.5 = 居中", NOTE_X, y + 5);
        y += ROW_H;

        g.text(f, "Y 位置", LABEL_X,     y + 5, 0xFFFFFF, true);
        drawNote(g, f, "屏幕高度比例，0.8 = 偏下方", NOTE_X, y + 5);
        y += ROW_H;

        g.text(f, "字体大小", LABEL_X,    y + 5, 0xFFFFFF, true);
        drawNote(g, f, "像素大小 (8 ~ 64)", NOTE_X, y + 5);
        y += ROW_H;

        g.text(f, "字体颜色", LABEL_X,    y + 5, 0xFFFFFF, true);
        drawNote(g, f, "十六进制 RGB，如 FFFFFF = 白色", NOTE_X, y + 5);
        y += ROW_H;

        g.text(f, "不透明度", LABEL_X,    y + 5, 0xFFFFFF, true);
        drawNote(g, f, "1.0 = 完全不透明", NOTE_X, y + 5);
        y += ROW_H;

        g.text(f, "指令模板", LABEL_X,    y + 5, 0xFFFFFF, true);
        drawNote(g, f, "{lyric} 会被替换为歌词文本", NOTE_X, y + 5);
    }

    private void drawNote(GuiGraphicsExtractor g, Font f, String text, int x, int y) {
        g.text(f, text, x, y, 0x888888, false);
    }

    private EditBox addEditBox(Font f, int x, int y, int w, int h, String initial) {
        EditBox box = new EditBox(f, x, y, w, h, Component.empty());
        box.setValue(initial);
        addRenderableWidget(box);
        return box;
    }

    private Button addToggle(int x, int y, String text, boolean value, Runnable action) {
        Button b = Button.builder(Component.literal(text + ": " + (value ? "开" : "关")), btn -> action.run())
                .bounds(x, y, BTN_W, BTN_H).build();
        addRenderableWidget(b);
        return b;
    }

    private void toggleShadow() {
        displayConfig.setShadowEnabled(!displayConfig.isShadowEnabled());
        shadowToggle.setMessage(Component.literal("文字阴影: " + (displayConfig.isShadowEnabled() ? "开" : "关")));
    }

    private void toggleCentered() {
        displayConfig.setCentered(!displayConfig.isCentered());
        centeredToggle.setMessage(Component.literal("居中显示: " + (displayConfig.isCentered() ? "开" : "关")));
    }

    private void toggleFade() {
        displayConfig.setFadeInOutEnabled(!displayConfig.isFadeInOutEnabled());
        fadeToggle.setMessage(Component.literal("淡入淡出: " + (displayConfig.isFadeInOutEnabled() ? "开" : "关")));
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
            e.printStackTrace();
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}