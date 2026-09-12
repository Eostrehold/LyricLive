package com.eostrehold.lyriclive.client.gui;

import com.eostrehold.lyriclive.LyricLive;
import com.eostrehold.lyriclive.client.display.DisplayConfig;
import com.eostrehold.lyriclive.client.sender.LyricSender;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

/**
 * 设置界面。UI 结构由 {@code assets/lyriclive/owo_ui/settings_screen.xml} 定义，
 * 本类负责配置项读写与保存。
 */
public class SettingsScreen extends BaseUIModelScreen<FlowLayout> {

    private static final Identifier MODEL_ID = Identifier.fromNamespaceAndPath("lyriclive", "settings_screen");

    private final DisplayConfig displayConfig;
    private final LyricSender commandSender;
    private final Screen parent;

    private DiscreteSliderComponent posXSlider;
    private DiscreteSliderComponent posYSlider;
    private DiscreteSliderComponent fontSizeSlider;
    private DiscreteSliderComponent opacitySlider;
    private TextBoxComponent colorInput;
    private TextBoxComponent prefixInput;
    private CheckboxComponent shadowCheck;
    private CheckboxComponent centerCheck;
    private CheckboxComponent fadeCheck;
    private CheckboxComponent cmdCheck;

    public SettingsScreen(DisplayConfig displayConfig, LyricSender chatSender, LyricSender commandSender,
                          Screen parent) {
        super(FlowLayout.class, MODEL_ID);
        this.displayConfig = displayConfig;
        this.commandSender = commandSender;
        this.parent = parent;
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        // 滑块
        posXSlider = rootComponent.childById(DiscreteSliderComponent.class, "pos-x-slider");
        posXSlider.setFromDiscreteValue(displayConfig.getPositionX() * 100);
        posXSlider.onChanged().subscribe(value -> displayConfig.setPositionX((float) (value / 100.0)));

        posYSlider = rootComponent.childById(DiscreteSliderComponent.class, "pos-y-slider");
        posYSlider.setFromDiscreteValue(displayConfig.getPositionY() * 100);
        posYSlider.onChanged().subscribe(value -> displayConfig.setPositionY((float) (value / 100.0)));

        fontSizeSlider = rootComponent.childById(DiscreteSliderComponent.class, "font-size-slider");
        fontSizeSlider.setFromDiscreteValue(displayConfig.getFontSize());
        fontSizeSlider.onChanged().subscribe(value -> displayConfig.setFontSize((int) Math.round(value)));

        opacitySlider = rootComponent.childById(DiscreteSliderComponent.class, "opacity-slider");
        opacitySlider.setFromDiscreteValue(displayConfig.getOpacity() * 100);
        opacitySlider.onChanged().subscribe(value -> displayConfig.setOpacity((float) (value / 100.0)));

        // 文本框
        colorInput = rootComponent.childById(TextBoxComponent.class, "color-input");
        colorInput.text(String.format("%06X", displayConfig.getFontColor()));

        prefixInput = rootComponent.childById(TextBoxComponent.class, "prefix-input");
        prefixInput.text(commandSender.getPrefix());

        // 复选框
        shadowCheck = rootComponent.childById(CheckboxComponent.class, "shadow-check");
        shadowCheck.checked(displayConfig.isShadowEnabled());
        shadowCheck.onChanged(checked -> displayConfig.setShadowEnabled(checked));

        centerCheck = rootComponent.childById(CheckboxComponent.class, "center-check");
        centerCheck.checked(displayConfig.isCentered());
        centerCheck.onChanged(checked -> displayConfig.setCentered(checked));

        fadeCheck = rootComponent.childById(CheckboxComponent.class, "fade-check");
        fadeCheck.checked(displayConfig.isFadeInOutEnabled());
        fadeCheck.onChanged(checked -> displayConfig.setFadeInOutEnabled(checked));

        cmdCheck = rootComponent.childById(CheckboxComponent.class, "cmd-check");
        cmdCheck.checked(commandSender.isEnabled());
        cmdCheck.onChanged(checked -> commandSender.setEnabled(checked));

        // 保存并返回
        rootComponent.childById(ButtonComponent.class, "save-button").onPress(button -> onClose());
    }

    @Override
    public void onClose() {
        save();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(parent);
        }
    }

    private void save() {
        if (posXSlider != null) displayConfig.setPositionX((float) (posXSlider.discreteValue() / 100.0));
        if (posYSlider != null) displayConfig.setPositionY((float) (posYSlider.discreteValue() / 100.0));
        if (fontSizeSlider != null) displayConfig.setFontSize((int) Math.round(fontSizeSlider.discreteValue()));
        if (opacitySlider != null) displayConfig.setOpacity((float) (opacitySlider.discreteValue() / 100.0));
        if (prefixInput != null) commandSender.setPrefix(prefixInput.getValue());
        if (colorInput != null) {
            try {
                String colorStr = colorInput.getValue().replace("#", "").trim();
                if (!colorStr.isEmpty()) {
                    displayConfig.setFontColor(Integer.parseInt(colorStr, 16));
                }
            } catch (NumberFormatException ignored) {
                LyricLive.LOGGER.warn("设置颜色值不合法，保持原有颜色: {}", colorInput.getValue());
            }
        }
        LyricLiveClient.saveDisplayConfig();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
