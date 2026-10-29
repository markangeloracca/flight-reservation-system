package ca.ucalgary.frs.ui.components;

import ca.ucalgary.frs.ui.theme.Palette;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import java.awt.Color;
import java.util.function.Supplier;

/**
 * Vector icons from {@code /icons/*.svg}, recoloured at paint time so they follow the current theme
 * and scale crisply at any DPI.
 */
public final class Icons {
    public static final int SMALL = 16;
    public static final int MEDIUM = 18;
    public static final int LARGE = 24;

    private Icons() {
    }

    /** Icon in the normal text colour. */
    public static FlatSVGIcon get(String name, int size) {
        return tinted(name, size, Palette::text);
    }

    public static FlatSVGIcon get(String name) {
        return get(name, SMALL);
    }

    public static FlatSVGIcon muted(String name, int size) {
        return tinted(name, size, Palette::muted);
    }

    public static FlatSVGIcon accent(String name, int size) {
        return tinted(name, size, Palette::accent);
    }

    /** Icon whose colour comes from a supplier evaluated on every paint. */
    public static FlatSVGIcon tinted(String name, int size, Supplier<Color> color) {
        FlatSVGIcon icon = new FlatSVGIcon("icons/" + name + ".svg", size, size);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color.get()));
        return icon;
    }
}
