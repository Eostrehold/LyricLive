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

    private static final int C_WHITE  = 0xFFFFFFFF;
    private static final int C_YELLOW = 0xFFFFFF55;
    private static final int C_GRAY   = 0xFFCCCCCC;

    private final DisplayConfig displayConfig;
    private final ChatSender chatSender;
    private final CommandSender commandSender;
    private final Screen parent;

    private EditBox posXBox, posYBox, fontSizeBox, colorBox, opacityBox;
    private Button shadowToggle, centeredToggle, fadeToggle;
    private Button cmdSendToggle;

    public SettingsScreen(DisplayConfig displayConfig, ChatSender chatSender,
                          CommandSender commandSender, Screen parent) {
        super(Component.literal("LyricLive 设置"));
        this.displayConfig = displayConfig;
        this.chatSender = chatSender;
        this.commandSender = commandSender;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        posXBox        = box(0, String.valueOf(displayConfig.getPositionX()));
        posYBox        = box(1, String.valueOf(displayConfig.getPositionY()));
        fontSizeBox    = box(2, String.valueOf(displayConfig.getFontSize()));
        colorBox       = box(3, String.format("%06X", displayConfig.getFontColor()));
        opacityBox     = box(4, String.valueOf(displayConfig.getOpacity()));

        // 开关行（第 5 行不占输入框，留给 cmd 开关）
        int toggleRowY = ROW_Y0 + 5 * ROW_H + 6;
        shadowToggle   = toggleAt(0, toggleRowY, "文字阴影",   displayConfig.isShadowEnabled(),          this::toggleShadow);
        centeredToggle = toggleAt(1, toggleRowY, "居中显示",   displayConfig.isCentered(),               this::toggleCentered);
        fadeToggle     = toggleAt(2, toggleRowY, "淡入淡出",   displayConfig.isFadeInOutEnabled(),       this::toggleFade);
        cmdSendToggle  = toggleAt(3, toggleRowY, "指令发送",   commandSender.isEnabled(),               this::toggleCmdSend);

        addRenderableWidget(Button.builder(Component.literal("保存并返回"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    // ---- render ----
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        Font f = Minecraft.getInstance().font;

        g.fill(0, 0, this.width, this.height, 0xC0101010);

        String title = "LyricLive 设置";
        g.text(f, title, this.width / 2 - f.width(title) / 2, 8, C_YELLOW, true);

        super.extractRenderState(g, mx, my, pt);

        for (int i = 0; i < 5; i++) {
            int y = ROW_Y0 + i * ROW_H + 5;
            g.text(f, LABELS[i], COL_LABEL, y, C_WHITE, true);
            int nx = COL_NOTE;
            int nw = f.width(NOTES[i]);
            g.fill(nx - 2, y - 1, nx + nw + 4, y + f.lineHeight + 2, 0xAA333333);
            g.text(f, NOTES[i], nx, y, C_GRAY, false);
        }
    }

    // ---- widgets ----

    private EditBox box(int row, String val) {
        Font f = Minecraft.getInstance().font;
        EditBox b = new EditBox(f, COL_INPUT, ROW_Y0 + row * ROW_H, 50, 18, Component.empty());
        b.setValue(val);
        addRenderableWidget(b);
        return b;
    }

    private Button toggleAt(int col, int y, String label, boolean on, Runnable action) {
        int x = COL_LABEL + col * 90;
        Button b = Button.builder(
                Component.literal(label + (on ? ": 开" : ": 关")),
                btn -> action.run()
        ).bounds(x, y, 80, 20).build();
        addRenderableWidget(b);
        return b;
    }

    // ---- toggles ----

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
    private void toggleCmdSend() {
        commandSender.setEnabled(!commandSender.isEnabled());
        cmdSendToggle.setMessage(Component.literal("指令发送: " + (commandSender.isEnabled() ? "开" : "关")));
    }

    // ---- save ----

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
        } catch (NumberFormatException ignored) {}
    }

    // ---- labels ----

    private static final String[] LABELS = {
        "X 位置", "Y 位置", "字体大小", "字体颜色", "不透明度"
    };
    private static final String[] NOTES = {
        "0.0~1.0  0.5 = 居中", "0.0~1.0  0.8 = 偏下",
        "像素  8~64", "十六进制  如 FFFFFF = 白",
        "0.0~1.0  1.0 = 不透明"
    };

    @Override public boolean isPauseScreen() { return false; }
}