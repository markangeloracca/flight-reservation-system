package ca.ucalgary.frs.ui.shell;

import ca.ucalgary.frs.domain.model.Promotion;
import ca.ucalgary.frs.ui.components.AppDialog;
import ca.ucalgary.frs.ui.components.Badge;
import ca.ucalgary.frs.ui.components.Icons;
import ca.ucalgary.frs.ui.components.Tone;
import ca.ucalgary.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

/** Use case: Monthly promotional news, shown to each customer once per month. */
final class PromotionDialog extends AppDialog {
    private PromotionDialog(Component owner, Promotion p, YearMonth month) {
        super(owner, "This month's offer", month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " "
                + month.getYear());
        JPanel body = new JPanel(new MigLayout("insets 8 0 8 0, wrap 1, fillx", "[grow, fill]"));
        JLabel icon = new JLabel(Icons.accent("gift", 40));
        icon.setHorizontalAlignment(JLabel.LEFT);
        body.add(icon, "alignx left, gapbottom 8");
        body.add(Typography.h1(p.title()));
        if (p.discountPercent() > 0) {
            body.add(new Badge(p.discountPercent() + "% OFF", Tone.SUCCESS), "growx 0, gapbottom 6");
        }
        body.add(new ca.ucalgary.frs.ui.components.WrapLabel(p.description(), 380), "w 380!, gaptop 4");
        body.add(new ca.ucalgary.frs.ui.components.WrapLabel("The discount is applied automatically to every booking "
                + "you make before the end of the month.", 380), "w 380!, gaptop 8");
        setBody(body);
        addPrimaryButton("Start exploring", this::dispose);
    }

    static void show(Component owner, Promotion p, YearMonth month) {
        new PromotionDialog(owner, p, month).present();
    }
}
