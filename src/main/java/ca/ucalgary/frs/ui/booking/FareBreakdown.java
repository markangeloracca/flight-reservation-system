package ca.ucalgary.frs.ui.booking;

import ca.ucalgary.frs.domain.model.FareQuote;
import ca.ucalgary.frs.domain.model.Money;
import ca.ucalgary.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/** Fare, promotion, tax and total for the selected seat. */
public class FareBreakdown extends JPanel {
    private final JLabel fare = new JLabel("—");
    private final JLabel discountLabel = Typography.caption("Promotion");
    private final JLabel discount = new JLabel();
    private final JLabel tax = new JLabel("—");
    private final JLabel total = Typography.h2("—");
    private final JLabel extraLabel = Typography.caption("");
    private final JLabel extra = Typography.strong("");

    public FareBreakdown() {
        super(new MigLayout("insets 0, fillx, wrap 2, gapy 6", "[grow][right]"));
        setOpaque(false);
        add(Typography.caption("Fare"));
        add(fare);
        add(discountLabel, "hidemode 3");
        add(discount, "hidemode 3");
        add(Typography.caption("Tax (GST 5%)"));
        add(tax);
        add(new JSeparator(), "span, growx, gapy 4");
        add(Typography.strong("Total"));
        add(total);
        add(extraLabel, "hidemode 3");
        add(extra, "hidemode 3");
        discount.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "foreground: $Frs.successColor");
        show(null);
    }

    public void show(FareQuote q) {
        boolean has = q != null;
        fare.setText(has ? q.fare().toString() : "—");
        tax.setText(has ? q.tax().toString() : "—");
        total.setText(has ? q.total().toString() : "—");
        boolean promo = has && q.hasDiscount();
        discountLabel.setVisible(promo);
        discount.setVisible(promo);
        if (promo) {
            discountLabel.setText(q.promotionTitle());
            discount.setText("−" + q.discount());
        }
        showExtra(null, null);
    }

    /** An extra line under the total, such as "You pay" or "Refund" for a change. */
    public void showExtra(String label, Money amount) {
        boolean visible = label != null;
        extraLabel.setVisible(visible);
        extra.setVisible(visible);
        if (visible) {
            extraLabel.setText(label);
            extra.setText(amount.toString());
        }
        revalidate();
    }
}
