package ca.ucalgary.frs.ui.booking;

import ca.ucalgary.frs.domain.model.Flight;
import ca.ucalgary.frs.ui.components.Card;
import ca.ucalgary.frs.ui.components.Formats;
import ca.ucalgary.frs.ui.components.Icons;
import ca.ucalgary.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;

/** Card summarising one flight: route, times, duration, airline and aircraft. */
public class FlightSummary extends Card {
    public FlightSummary(Flight f) {
        super(new MigLayout("insets 0, fillx, wrap 3", "[left][grow, center][right]", "[]2[]10[]"));
        add(Typography.caption(f.departureTime().format(Formats.DATE)), "span, wrap");
        add(Typography.display(f.route().origin().code()));
        JLabel plane = new JLabel(Formats.duration(f.duration()), Icons.muted("plane", Icons.SMALL), JLabel.CENTER);
        plane.setHorizontalTextPosition(JLabel.CENTER);
        plane.setVerticalTextPosition(JLabel.BOTTOM);
        Typography.mutedText(plane);
        add(plane, "growx");
        add(Typography.display(f.route().destination().code()));
        add(Typography.strong(f.departureTime().format(Formats.TIME) + "  " + f.route().origin().city()));
        add(new JLabel(""));
        add(Typography.strong(f.route().destination().city() + "  " + f.arrivalTime().format(Formats.TIME)));
        add(Typography.caption(f.flightNumber() + " · " + f.airline().name() + " · " + f.aircraft().model()),
                "span, growx");
    }
}
