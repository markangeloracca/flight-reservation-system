package ca.ucalgary.frs.ui.shell;

import ca.ucalgary.frs.ui.components.Card;
import ca.ucalgary.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.JProgressBar;
import javax.swing.JWindow;
import java.awt.Color;

/** Shown while the database is opened (and seeded on first launch). */
public final class Splash extends JWindow {
    public Splash() {
        Card card = new Card(new MigLayout("insets 20 28 16 28, wrap 1, alignx center", "[center]", "[]12[]4[]16[]"));
        card.add(new Brand(56));
        card.add(Typography.h2("Flight Reservation System"));
        card.add(Typography.caption("Preparing your data…"));
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        card.add(bar, "w 240!");
        setBackground(new Color(0, 0, 0, 0));
        setContentPane(card);
        pack();
        setLocationRelativeTo(null);
    }
}
