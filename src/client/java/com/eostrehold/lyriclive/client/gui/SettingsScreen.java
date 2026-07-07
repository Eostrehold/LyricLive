package com.eostrehold.lyriclive.client.gui;

import com.eostrehold.lyriclive.client.display.DisplayConfig;
import com.eostrehold.lyriclive.client.sender.ChatSender;
import com.eostrehold.lyriclive.client.sender.CommandSender;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * LyricLive 设置界面，管理显示和发送配置。
 */
public class SettingsScreen extends Screen {
    private final DisplayConfig displayConfig;
    private final ChatSender chatSender;
    private final CommandSender commandSender;
    private final Screen parent;

    private EditBox positionXBox;
    private EditBox positionYBox;
    private EditBox fontSizeBox;
    private EditBox colorBox;
    private EditBox opacityBox;
    private EditBox commandTemplateBox;

    private Button shadowToggleButton;
    private Button centeredToggleButton;
    private Button fadeInOutToggleButton;
    private Button doneButton;

    public SettingsScreen(DisplayConfig displayConfig, ChatSender chatSender, CommandSender commandSender, Screen parent) {
        super(Component.literal("LyricLive 设置"));
        this.displayConfig = displayConfig;
        this.chatSender = chatSender;
        this.commandSender = commandSender;
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 30;
        int rowHeight = 30;
        int labelWidth = 100;
        int inputWidth = 80;
        int buttonWidth = 80;

        positionXBox = new EditBox(this.font, centerX - labelWidth - inputWidth, startY, inputWidth, 20, Component.literal("X 位置"));
        positionXBox.setValue(String.valueOf(displayConfig.getPositionX()));

        positionYBox = new EditBox(this.font, centerX - labelWidth - inputWidth, startY + rowHeight, inputWidth, 20, Component.literal("Y 位置"));
        positionYBox.setValue(String.valueOf(displayConfig.getPositionY()));

        fontSizeBox = new EditBox(this.font, centerX - labelWidth - inputWidth, startY + 2 * rowHeight, inputWidth, 20, Component.literal("字体大小"));
        fontSizeBox.setValue(String.valueOf(displayConfig.getFontSize()));

        colorBox = new EditBox(this.font, centerX - labelWidth - inputWidth, startY + 3 * rowHeight, inputWidth, 20, Component.literal("字体颜色"));
        colorBox.setValue(String.format("%06X", displayConfig.getFontColor()));

        opacityBox = new EditBox(this.font, centerX - labelWidth - inputWidth, startY + 4 * rowHeight, inputWidth, 20, Component.literal("透明度"));
        opacityBox.setValue(String.valueOf(displayConfig.getOpacity()));

        commandTemplateBox = new EditBox(this.font, centerX - labelWidth - inputWidth, startY + 5 * rowHeight, inputWidth * 2, 20, Component.literal("指令模板"));
        commandTemplateBox.setValue(commandSender.getCommandTemplate());
        commandTemplateBox.setMaxLength(100);

        shadowToggleButton = Button.builder(
                Component.literal("阴影: " + (displayConfig.isShadowEnabled() ? "开" : "关")),
                button -> toggleShadow()
        ).bounds(centerX + 10, startY, buttonWidth, 20).build();

        centeredToggleButton = Button.builder(
                Component.literal("居中: " + (displayConfig.isCentered() ? "开" : "关")),
                button -> toggleCentered()
        ).bounds(centerX + 10, startY + rowHeight, buttonWidth, 20).build();

        fadeInOutToggleButton = Button.builder(
                Component.literal("渐变: " + (displayConfig.isFadeInOutEnabled() ? "开" : "关")),
                button -> toggleFadeInOut()
        ).bounds(centerX + 10, startY + 2 * rowHeight, buttonWidth, 20).build();

        doneButton = Button.builder(
                Component.literal("完成"),
                button -> onClose()
        ).bounds(centerX - 50, this.height - 40, 100, 20).build();

        addRenderableWidget(positionXBox);
        addRenderableWidget(positionYBox);
        addRenderableWidget(fontSizeBox);
        addRenderableWidget(colorBox);
        addRenderableWidget(opacityBox);
        addRenderableWidget(commandTemplateBox);
        addRenderableWidget(shadowToggleButton);
        addRenderableWidget(centeredToggleButton);
        addRenderableWidget(fadeInOutToggleButton);
        addRenderableWidget(doneButton);
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        int centerX = this.width / 2;
        int startY = 30;
        int rowHeight = 30;

        guiGraphics.drawString(this.font, "X 位置 (0.0-1.0):", centerX - 100 - 80 - 5, startY + 5, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Y 位置 (0.0-1.0):", centerX - 100 - 80 - 5, startY + rowHeight + 5, 0xFFFFFF);
        guiGraphics.drawString(this.font, "字体大小 (8-64):", centerX - 100 - 80 - 5, startY + 2 * rowHeight + 5, 0xFFFFFF);
        guiGraphics.drawString(this.font, "字体颜色 (十六进制):", centerX - 100 - 80 - 5, startY + 3 * rowHeight + 5, 0xFFFFFF);
        guiGraphics.drawString(this.font, "透明度 (0.0-1.0):", centerX - 100 - 80 - 5, startY + 4 * rowHeight + 5, 0xFFFFFF);
        guiGraphics.drawString(this.font, "指令模板:", centerX - 100 - 80 - 5, startY + 5 * rowHeight + 5, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        saveSettings();
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void saveSettings() {
        try {
            float posX = Float.parseFloat(positionXBox.getValue());
            displayConfig.setPositionX(posX);

            float posY = Float.parseFloat(positionYBox.getValue());
            displayConfig.setPositionY(posY);

            int fontSize = Integer.parseInt(fontSizeBox.getValue());
            displayConfig.setFontSize(fontSize);

            String colorStr = colorBox.getValue().replace("#", "");
            if (colorStr.length() == 6) {
                int color = Integer.parseInt(colorStr, 16);
                displayConfig.setFontColor(color);
            }

            float opacity = Float.parseFloat(opacityBox.getValue());
            displayConfig.setOpacity(opacity);

            commandSender.setCommandTemplate(commandTemplateBox.getValue());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void toggleShadow() {
        boolean newValue = !displayConfig.isShadowEnabled();
        displayConfig.setShadowEnabled(newValue);
        shadowToggleButton.setMessage(Component.literal("阴影: " + (newValue ? "开" : "关")));
    }

    private void toggleCentered() {
        boolean newValue = !displayConfig.isCentered();
        displayConfig.setCentered(newValue);
        centeredToggleButton.setMessage(Component.literal("居中: " + (newValue ? "开" : "关")));
    }

    private void toggleFadeInOut() {
        boolean newValue = !displayConfig.isFadeInOutEnabled();
        displayConfig.setFadeInOutEnabled(newValue);
        fadeInOutToggleButton.setMessage(Component.literal("渐变: " + (newValue ? "开" : "关")));
    }
}