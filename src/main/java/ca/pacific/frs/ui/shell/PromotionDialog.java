package ca.pacific.frs.ui.shell;

import ca.pacific.frs.ui.components.WrapLabel;
import ca.pacific.frs.domain.model.Promotion;
import ca.pacific.frs.ui.components.AppDialog;
import ca.pacific.frs.ui.components.Badge;
import ca.pacific.frs.ui.components.Icons;
import ca.pacific.frs.ui.components.Tone;
import ca.pacific.frs.ui.theme.Typography;
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
        body.add(new WrapLabel(p.description(), 380), "w 380!, gaptop 4");
        body.add(new WrapLabel("The discount is applied automatically to every booking "
                + "you make before the end of the month.", 380), "w 380!, gaptop 8");
        setBody(body);
        addPrimaryButton("Start exploring", this::dispose);
    }

    static void show(Component owner, Promotion p, YearMonth month) {
        new PromotionDialog(owner, p, month).present();
    }
}
