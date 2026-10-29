package ca.pacific.frs.ui.components;

import ca.pacific.frs.ui.theme.Palette;

import java.awt.Color;

/** Semantic colour pairs used by badges, banners and toasts. */
public enum Tone {
    NEUTRAL, ACCENT, SUCCESS, WARNING, DANGER;

    public Color foreground() {
        return switch (this) {
            case NEUTRAL -> Palette.muted();
            case ACCENT -> Palette.accent();
            case SUCCESS -> Palette.success();
            case WARNING -> Palette.warning();
            case DANGER -> Palette.danger();
        };
    }

    public Color background() {
        return switch (this) {
            case NEUTRAL -> Palette.layer2();
            case ACCENT -> Palette.accentSoft();
            case SUCCESS -> Palette.successSoft();
            case WARNING -> Palette.warningSoft();
            case DANGER -> Palette.dangerSoft();
        };
    }

    public String icon() {
        return switch (this) {
            case SUCCESS -> "check-circle";
            case WARNING, DANGER -> "alert-circle";
            default -> "info";
        };
    }
}
