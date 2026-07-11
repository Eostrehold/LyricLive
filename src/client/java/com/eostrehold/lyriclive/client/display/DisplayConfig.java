package com.eostrehold.lyriclive.client.display;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.eostrehold.lyriclive.LyricLive;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
/**
 * 歌词显示配置，管理显示样式和位置。
 */
public class DisplayConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // 位置配置
    private float positionX = 0.5f;  // X 位置（0.0 - 1.0，相对于屏幕宽度）
    private float positionY = 0.8f;  // Y 位置（0.0 - 1.0，相对于屏幕高度）

    // 样式配置
    private int fontSize = 16;           // 字体大小
    private int fontColor = 0xFFFFFF;    // 字体颜色（白色）
    private int shadowColor = 0x3F3F3F;  // 阴影颜色
    private float opacity = 1.0f;        // 透明度（0.0 - 1.0）
    private boolean shadowEnabled = true; // 是否启用阴影
    private boolean centered = true;      // 是否居中显示

    // 动画配置
    private boolean fadeInOutEnabled = true;  // 是否启用淡入淡出效果
    private int fadeInDuration = 200;         // 淡入持续时间（毫秒）
    private int fadeOutDuration = 200;        // 淡出持续时间（毫秒）

    public DisplayConfig() {
    }

    // Position getters/setters
    public float getPositionX() {
        return positionX;
    }

    public void setPositionX(float positionX) {
        this.positionX = Math.max(0.0f, Math.min(1.0f, positionX));
    }

    public float getPositionY() {
        return positionY;
    }

    public void setPositionY(float positionY) {
        this.positionY = Math.max(0.0f, Math.min(1.0f, positionY));
    }

    // Style getters/setters
    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = Math.max(8, Math.min(64, fontSize));
    }

    public int getFontColor() {
        return fontColor;
    }

    public void setFontColor(int fontColor) {
        this.fontColor = fontColor;
    }

    public int getShadowColor() {
        return shadowColor;
    }

    public void setShadowColor(int shadowColor) {
        this.shadowColor = shadowColor;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
    }

    public boolean isShadowEnabled() {
        return shadowEnabled;
    }

    public void setShadowEnabled(boolean shadowEnabled) {
        this.shadowEnabled = shadowEnabled;
    }

    public boolean isCentered() {
        return centered;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    // Animation getters/setters
    public boolean isFadeInOutEnabled() {
        return fadeInOutEnabled;
    }

    public void setFadeInOutEnabled(boolean fadeInOutEnabled) {
        this.fadeInOutEnabled = fadeInOutEnabled;
    }

    public int getFadeInDuration() {
        return fadeInDuration;
    }

    public void setFadeInDuration(int fadeInDuration) {
        this.fadeInDuration = Math.max(0, fadeInDuration);
    }

    public int getFadeOutDuration() {
        return fadeOutDuration;
    }

    public void setFadeOutDuration(int fadeOutDuration) {
        this.fadeOutDuration = Math.max(0, fadeOutDuration);
    }

    /**
     * 计算像素位置 X
     * @param screenWidth 屏幕宽度
     */
    public int getPixelX(int screenWidth) {
        return (int) (screenWidth * positionX);
    }

    /**
     * 计算像素位置 Y
     * @param screenHeight 屏幕高度
     */
    public int getPixelY(int screenHeight) {
        return (int) (screenHeight * positionY);
    }

    /**
     * 获取 ARGB 格式的字体颜色（包含透明度）
     */
    public int getFontColorARGB() {
        int alpha = (int) (opacity * 255) & 0xFF;
        return (alpha << 24) | (fontColor & 0x00FFFFFF);
    }

    /**
     * 获取带透明度的阴影颜色
     */
    public int getShadowColorARGB() {
        int alpha = (int) (opacity * 255) & 0xFF;
        return (alpha << 24) | (shadowColor & 0x00FFFFFF);
    }

    @Override
    public String toString() {
        return String.format("DisplayConfig[pos=(%.2f, %.2f), fontSize=%d, color=#%06X, opacity=%.2f, centered=%b]",
                positionX, positionY, fontSize, fontColor, opacity, centered);
    }
    /**
     * 保存配置到 JSON 文件
     * @param path 配置文件路径
     */
    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            String json = GSON.toJson(this);
            Files.writeString(path, json);
            LyricLive.LOGGER.info("显示配置已保存: {}", path);
        } catch (IOException e) {
            LyricLive.LOGGER.error("保存显示配置失败", e);
        }
    }

    /**
     * 从 JSON 文件加载配置
     * @param path 配置文件路径
     * @return 加载后的配置对象，失败则返回默认配置
     */
    public static DisplayConfig load(Path path) {
        try {
            if (Files.exists(path)) {
                String json = Files.readString(path);
                DisplayConfig config = GSON.fromJson(json, DisplayConfig.class);
                if (config != null) {
                    LyricLive.LOGGER.info("显示配置已加载: {}", path);
                    return config;
                }
            }
        } catch (IOException e) {
            LyricLive.LOGGER.warn("加载显示配置失败，使用默认值", e);
        }
        return new DisplayConfig();
    }
}