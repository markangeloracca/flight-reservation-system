package ca.ucalgary.frs.ui.theme;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Installs FlatLaf with the project's design tokens and switches between light and dark mode at runtime
 * without restarting. The choice is remembered between launches.
 */
public final class Theme {
    public static final String PROPERTY_DARK = "dark";
    private static final String PREF_KEY = "theme.dark";
    private static final PropertyChangeSupport CHANGES = new PropertyChangeSupport(Theme.class);
    private static boolean dark;

    private static final Map<String, String> COMMON = Map.ofEntries(
            Map.entry("Component.arc", "10"),
            Map.entry("Button.arc", "10"),
            Map.entry("TextComponent.arc", "8"),
            Map.entry("CheckBox.arc", "6"),
            Map.entry("ProgressBar.arc", "6"),
            Map.entry("Component.focusWidth", "2"),
            Map.entry("Component.innerFocusWidth", "0"),
            Map.entry("ScrollBar.width", "10"),
            Map.entry("ScrollBar.thumbArc", "999"),
            Map.entry("ScrollBar.thumbInsets", "2,2,2,2"),
            Map.entry("ScrollBar.showButtons", "false"),
            Map.entry("Table.rowHeight", "34"),
            Map.entry("Table.showHorizontalLines", "false"),
            Map.entry("Table.showVerticalLines", "false"),
            Map.entry("TableHeader.height", "32"),
            Map.entry("TabbedPane.tabHeight", "36"),
            Map.entry("TabbedPane.showTabSeparators", "false"),
            Map.entry("TitlePane.unifiedBackground", "true"),
            Map.entry("SplitPaneDivider.style", "plain")
    );

    private static final Map<String, String> LIGHT = Map.ofEntries(
            Map.entry("@accentColor", "#2675BF"),
            Map.entry("Frs.accentColor", "#2675BF"),
            Map.entry("Frs.accentSoftColor", "#E3EEF9"),
            Map.entry("Frs.onAccentColor", "#FFFFFF"),
            Map.entry("Frs.successColor", "#1A7F37"),
            Map.entry("Frs.successSoftColor", "#DFF3E4"),
            Map.entry("Frs.warningColor", "#8A5A00"),
            Map.entry("Frs.warningSoftColor", "#FBEFD5"),
            Map.entry("Frs.dangerColor", "#C4232E"),
            Map.entry("Frs.dangerSoftColor", "#FBE3E4"),
            Map.entry("Frs.layer0Color", "#F3F5F8"),
            Map.entry("Frs.layer1Color", "#FFFFFF"),
            Map.entry("Frs.layer2Color", "#F0F3F7"),
            Map.entry("Frs.borderColor", "#DCE2E9"),
            Map.entry("Frs.textColor", "#1F2328"),
            Map.entry("Frs.mutedColor", "#57606A"),
            Map.entry("Frs.hoverColor", "#EAF1FA"),
            Map.entry("Frs.shadowColor", "#0F172A"),
            Map.entry("Table.alternateRowColor", "#F8FAFC"),
            Map.entry("Table.background", "#FFFFFF"),
            Map.entry("Panel.background", "#F3F5F8")
    );

    private static final Map<String, String> DARK = Map.ofEntries(
            Map.entry("@accentColor", "#4C9BE8"),
            Map.entry("Frs.accentColor", "#4C9BE8"),
            Map.entry("Frs.accentSoftColor", "#1C3550"),
            Map.entry("Frs.onAccentColor", "#0B1622"),
            Map.entry("Frs.successColor", "#4AC26B"),
            Map.entry("Frs.successSoftColor", "#173A24"),
            Map.entry("Frs.warningColor", "#E3B341"),
            Map.entry("Frs.warningSoftColor", "#3A2E12"),
            Map.entry("Frs.dangerColor", "#FF6B66"),
            Map.entry("Frs.dangerSoftColor", "#45201F"),
            Map.entry("Frs.layer0Color", "#15181C"),
            Map.entry("Frs.layer1Color", "#1D2126"),
            Map.entry("Frs.layer2Color", "#262B31"),
            Map.entry("Frs.borderColor", "#343A42"),
            Map.entry("Frs.textColor", "#E6EDF3"),
            Map.entry("Frs.mutedColor", "#9DA5AE"),
            Map.entry("Frs.hoverColor", "#233142"),
            Map.entry("Frs.shadowColor", "#000000"),
            Map.entry("Table.alternateRowColor", "#21262C"),
            Map.entry("Table.background", "#1D2126"),
            Map.entry("Panel.background", "#15181C")
    );

    private Theme() {
    }

    /** Installs the remembered theme. Call once on the EDT before creating any component. */
    public static void install() {
        apply(preferences().getBoolean(PREF_KEY, false));
    }

    public static boolean isDark() {
        return dark;
    }

    /** Switches light/dark with a short cross-fade, updating every open window. */
    public static void toggle() {
        FlatAnimatedLafChange.showSnapshot();
        apply(!dark);
        FlatLaf.updateUI();
        FlatAnimatedLafChange.hideSnapshotWithAnimation();
        preferences().putBoolean(PREF_KEY, dark);
        CHANGES.firePropertyChange(PROPERTY_DARK, !dark, dark);
    }

    private static void apply(boolean useDark) {
        Map<String, String> extras = new HashMap<>(COMMON);
        extras.putAll(useDark ? DARK : LIGHT);
        FlatLaf.setGlobalExtraDefaults(extras);
        if (useDark) {
            FlatMacDarkLaf.setup();
        } else {
            FlatMacLightLaf.setup();
        }
        dark = useDark;
    }

    public static void addListener(PropertyChangeListener l) {
        CHANGES.addPropertyChangeListener(l);
    }

    public static void removeListener(PropertyChangeListener l) {
        CHANGES.removePropertyChangeListener(l);
    }

    private static Preferences preferences() {
        return Preferences.userNodeForPackage(Theme.class);
    }
}
