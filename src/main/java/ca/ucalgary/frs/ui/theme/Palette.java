package ca.ucalgary.frs.ui.theme;

import javax.swing.UIManager;
import java.awt.Color;

/**
 * Semantic colour tokens. Values are installed into {@link UIManager} by {@link Theme} for the active
 * light or dark mode and read at paint time, so custom-painted components follow theme switches.
 */
public final class Palette {
    private Palette() {
    }

    public static Color accent() {
        return get("Frs.accentColor");
    }

    public static Color accentSoft() {
        return get("Frs.accentSoftColor");
    }

    public static Color onAccent() {
        return get("Frs.onAccentColor");
    }

    public static Color success() {
        return get("Frs.successColor");
    }

    public static Color successSoft() {
        return get("Frs.successSoftColor");
    }

    public static Color warning() {
        return get("Frs.warningColor");
    }

    public static Color warningSoft() {
        return get("Frs.warningSoftColor");
    }

    public static Color danger() {
        return get("Frs.dangerColor");
    }

    public static Color dangerSoft() {
        return get("Frs.dangerSoftColor");
    }

    /** Window background (layer 0). */
    public static Color layer0() {
        return get("Frs.layer0Color");
    }

    /** Cards and panels (layer 1). */
    public static Color layer1() {
        return get("Frs.layer1Color");
    }

    /** Inputs, hover fills and nested surfaces (layer 2). */
    public static Color layer2() {
        return get("Frs.layer2Color");
    }

    public static Color border() {
        return get("Frs.borderColor");
    }

    public static Color text() {
        return get("Frs.textColor");
    }

    public static Color muted() {
        return get("Frs.mutedColor");
    }

    public static Color hover() {
        return get("Frs.hoverColor");
    }

    public static Color shadow() {
        return get("Frs.shadowColor");
    }

    public static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static Color get(String key) {
        Color c = UIManager.getColor(key);
        return c != null ? c : Color.MAGENTA;
    }
}
