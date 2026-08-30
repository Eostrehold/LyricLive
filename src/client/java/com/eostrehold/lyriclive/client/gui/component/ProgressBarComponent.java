package com.eostrehold.lyriclive.client.gui.component;

import com.eostrehold.lyriclive.client.core.PlaybackController;
import com.eostrehold.lyriclive.client.core.TimelineManager;
import com.eostrehold.lyriclive.client.util.LyricUtils;
import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;

/**
 * 歌词播放进度条组件。固定宽度，显示播放进度并可点击跳转。
 */
public class ProgressBarComponent extends BaseUIComponent {

    private static final int C_PROGRESS_BG = 0xFF333333;
    private static final int C_PROGRESS_FG = 0xFF55FF55;
    private static final int C_PROGRESS_HOVER = 0xFF77FF77;
    private static final int C_TIME_TEXT = 0xFFFFFFFF;

    private final PlaybackController playbackController;
    private final TimelineManager timelineManager;

    public ProgressBarComponent(PlaybackController playbackController, TimelineManager timelineManager) {
        this.playbackController = playbackController;
        this.timelineManager = timelineManager;

        this.sizing(Sizing.fixed(260), Sizing.fixed(8));

        // MouseButtonEvent 坐标为相对本组件左上角的偏移。
        this.mouseDown().subscribe((click, doubled) -> {
            if (!timelineManager.hasLyrics()) return false;
            long total = lastTimestamp();
            if (total <= 0) return false;

            double ratio = Mth.clamp(click.x() / (double) this.width, 0.0, 1.0);
            long targetMs = (long) (total * ratio);
            playbackController.seekTo(targetMs);
            if (!playbackController.isPlaying()) {
                playbackController.play();
            }
            return true;
        });
    }

    @Override
    public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        long total = lastTimestamp();
        if (total <= 0) return;

        long current = playbackController.getCurrentTimeMillis();
        double ratio = Mth.clamp(current / (double) total, 0.0, 1.0);
        int fillWidth = (int) (this.width * ratio);

        boolean hovered = this.isInBoundingBox(mouseX, mouseY);
        int fillColor = hovered ? C_PROGRESS_HOVER : C_PROGRESS_FG;

        graphics.fill(this.x, this.y, this.x + this.width, this.y + this.height, C_PROGRESS_BG);
        if (fillWidth > 0) {
            graphics.fill(this.x, this.y, this.x + fillWidth, this.y + this.height, fillColor);
        }

        Font font = Minecraft.getInstance().font;
        String timeText = LyricUtils.fmtTime(current) + " / " + LyricUtils.fmtTime(total);
        graphics.text(font, timeText, this.x + this.width / 2 - font.width(timeText) / 2, this.y - 12, C_TIME_TEXT, true);
    }

    private long lastTimestamp() {
        if (!timelineManager.hasLyrics()) return 0;
        var lyrics = timelineManager.getCurrentTrack().getLyrics();
        return lyrics.isEmpty() ? 0 : lyrics.get(lyrics.size() - 1).getTimestamp();
    }
}
