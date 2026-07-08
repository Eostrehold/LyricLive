package com.eostrehold.lyriclive.client.gui;

import com.eostrehold.lyriclive.LyricLive;
import com.eostrehold.lyriclive.client.display.DisplayConfig;
import com.eostrehold.lyriclive.client.sender.LyricSender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SettingsScreen extends Screen {
    private static final int COL_LBL = 16;
    private static final int COL_INP = 150;
    private static final int COL_NTE = 240;
    private static final int Y0     = 32;
    private static final int RH     = 28;

    private static final int C_W    = 0xFFFFFFFF;
    private static final int C_Y    = 0xFFFFFF55;
    private static final int C_G    = 0xFFCCCCCC;

    private final DisplayConfig dc;
    private final LyricSender cs;
    private final LyricSender cmds;
    private final Screen parent;

    private EditBox posX, posY, size, color, opacity;
    private EditBox prefixBox;
    private Button shdTog, cenTog, fadeTog, cmdTog;

    public SettingsScreen(DisplayConfig dc, LyricSender cs, LyricSender cmds, Screen parent) {
        super(Component.literal("LyricLive 设置"));
        this.dc = dc; this.cs = cs; this.cmds = cmds; this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        posX    = inp(0, String.valueOf(dc.getPositionX()));
        posY    = inp(1, String.valueOf(dc.getPositionY()));
        size    = inp(2, String.valueOf(dc.getFontSize()));
        color   = inp(3, String.format("%06X", dc.getFontColor()));
        opacity = inp(4, String.valueOf(dc.getOpacity()));
        prefixBox = inp(5, cmds.getPrefix());

        int ty = Y0 + 6 * RH + 4;
        shdTog  = tog(0, ty, "文字阴影",   dc.isShadowEnabled(),     this::toggleShadow);
        cenTog  = tog(1, ty, "居中显示",   dc.isCentered(),          this::toggleCenter);
        fadeTog = tog(2, ty, "淡入淡出",   dc.isFadeInOutEnabled(),  this::toggleFade);
        cmdTog  = tog(3, ty, "前缀发送",   cmds.isEnabled(),         this::toggleCmd);

        addRenderableWidget(Button.builder(Component.literal("保存并返回"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt) {
        Font f = Minecraft.getInstance().font;
        g.fill(0, 0, this.width, this.height, 0xC0101010);

        String title = "LyricLive 设置";
        g.text(f, title, this.width / 2 - f.width(title) / 2, 8, C_Y, true);
        super.extractRenderState(g, mx, my, pt);

        for (int i = 0; i < 6; i++) {
            int y = Y0 + i * RH + 5;
            g.text(f, LBL[i], COL_LBL, y, C_W, true);
            int nx = COL_NTE;
            int nw = f.width(NTE[i]);
            g.fill(nx - 2, y - 1, nx + nw + 4, y + f.lineHeight + 2, 0xAA333333);
            g.text(f, NTE[i], nx, y, C_G, false);
        }
    }

    private EditBox inp(int row, String val) {
        Font f = Minecraft.getInstance().font;
        EditBox b = new EditBox(f, COL_INP, Y0 + row * RH, row == 5 ? 120 : 50, 18, Component.empty());
        b.setValue(val);
        addRenderableWidget(b);
        return b;
    }

    private Button tog(int col, int y, String label, boolean on, Runnable action) {
        int x = COL_LBL + col * 90;
        Button b = Button.builder(
                Component.literal(label + (on ? ": 开" : ": 关")),
                btn -> action.run()
        ).bounds(x, y, 80, 20).build();
        addRenderableWidget(b);
        return b;
    }

    // toggles
    private void toggleShadow() {
        dc.setShadowEnabled(!dc.isShadowEnabled());
        shdTog.setMessage(Component.literal("文字阴影: " + (dc.isShadowEnabled() ? "开" : "关")));
    }
    private void toggleCenter() {
        dc.setCentered(!dc.isCentered());
        cenTog.setMessage(Component.literal("居中显示: " + (dc.isCentered() ? "开" : "关")));
    }
    private void toggleFade() {
        dc.setFadeInOutEnabled(!dc.isFadeInOutEnabled());
        fadeTog.setMessage(Component.literal("淡入淡出: " + (dc.isFadeInOutEnabled() ? "开" : "关")));
    }
    private void toggleCmd() {
        cmds.setEnabled(!cmds.isEnabled());
        cmdTog.setMessage(Component.literal("前缀发送: " + (cmds.isEnabled() ? "开" : "关")));
    }

    @Override
    public void onClose() {
        save();
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    private void save() {
        try {
            dc.setPositionX(Float.parseFloat(posX.getValue()));
            dc.setPositionY(Float.parseFloat(posY.getValue()));
            dc.setFontSize(Integer.parseInt(size.getValue()));
            dc.setFontColor(Integer.parseInt(color.getValue().replace("#", ""), 16));
            dc.setOpacity(Float.parseFloat(opacity.getValue()));
            cmds.setPrefix(prefixBox.getValue());
        } catch (NumberFormatException ignored) {
            LyricLive.LOGGER.warn("设置输入值不合法，已忽略");
        }
        dc.save(Minecraft.getInstance().gameDirectory.toPath().resolve("config/lyriclive/display.json"));
    }

    private static final String[] LBL = {
        "X 位置", "Y 位置", "字体大小", "字体颜色", "不透明度", "发送前缀"
    };
    private static final String[] NTE = {
        "0.0~1.0  0.5=居中", "0.0~1.0  0.8=偏下",
        "像素 8~64", "RGB 十六进制 如 FFFFFF=白",
        "0.0~1.0  1.0=不透明", "发到聊天栏时会附加在歌词前"
    };

    @Override public boolean isPauseScreen() { return false; }
}