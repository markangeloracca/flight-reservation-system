package ca.ucalgary.frs.ui.booking;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.control.EventBus;
import ca.ucalgary.frs.domain.model.Airline;
import ca.ucalgary.frs.domain.model.Airport;
import ca.ucalgary.frs.domain.model.FlightAvailability;
import ca.ucalgary.frs.domain.model.Money;
import ca.ucalgary.frs.domain.model.Promotion;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.Banner;
import ca.ucalgary.frs.ui.components.Card;
import ca.ucalgary.frs.ui.components.Choice;
import ca.ucalgary.frs.ui.components.ContentPane;
import ca.ucalgary.frs.ui.components.DateRangePicker;
import ca.ucalgary.frs.ui.components.EmptyState;
import ca.ucalgary.frs.ui.components.Formats;
import ca.ucalgary.frs.ui.components.Icons;
import ca.ucalgary.frs.ui.components.ListTableModel;
import ca.ucalgary.frs.ui.components.Tables;
import ca.ucalgary.frs.ui.components.Tables.Labeled;
import ca.ucalgary.frs.ui.components.Tone;
import ca.ucalgary.frs.ui.components.Toolbar;
import ca.ucalgary.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.Timer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static ca.ucalgary.frs.ui.components.ListTableModel.column;

/** Use case: Search for flights and start a booking. */
public final class FlightSearchPage extends ca.ucalgary.frs.ui.shell.BasePage {
    private final JComboBox<Choice<Airport>> origin = new JComboBox<>();
    private final JComboBox<Choice<Airport>> destination = new JComboBox<>();
    private final JComboBox<Choice<Airline>> airline = new JComboBox<>();
    private final DateRangePicker dates = new DateRangePicker(LocalDate.now().plusDays(1), LocalDate.now().plusDays(7));
    private final JCheckBox anyDate = new JCheckBox("Any date", true);
    private final JLabel resultCount = Typography.caption(" ");
    private final Banner promo = new Banner();
    private final Timer autoSearch;
    private final ListTableModel<FlightAvailability> model;
    private final JTable table;
    private final ContentPane content;
    private final JButton book;
    private LocalDateTime lastNow = LocalDateTime.now();

    public FlightSearchPage(AppContext ctx) {
        super(ctx);
        model = new ListTableModel<>(
                column("Flight", String.class, (FlightAvailability a) -> a.flight().flightNumber()),
                column("Airline", String.class, (FlightAvailability a) -> a.flight().airline().name()),
                column("Route", String.class, (FlightAvailability a) -> a.flight().route().label()),
                column("Departs", LocalDateTime.class, (FlightAvailability a) -> a.flight().departureTime()),
                column("Arrives", String.class, (FlightAvailability a) -> a.flight().arrivalTime().format(Formats.TIME)),
                column("Duration", String.class, (FlightAvailability a) -> Formats.duration(a.flight().duration())),
                column("Status", Labeled.class, (FlightAvailability a) -> StatusViews.flight(a.flight(), lastNow)),
                column("Economy", Integer.class, FlightAvailability::economyAvailable),
                column("Business", Integer.class, FlightAvailability::businessAvailable),
                column("From", Money.class, FlightAvailability::lowestFare));
        table = Tables.create(model);
        Tables.widths(table, 70, 120, 100, 170, 70, 80, 100, 70, 70, 90);
        Tables.onActivate(table, model, this::book);

        autoSearch = new Timer(200, e -> reload());
        autoSearch.setRepeats(false);

        JButton swap = new JButton(Icons.get("swap"));
        swap.setToolTipText("Swap origin and destination");
        swap.getAccessibleContext().setAccessibleName("Swap origin and destination");
        swap.addActionListener(e -> {
            Object o = origin.getSelectedItem();
            origin.setSelectedItem(destination.getSelectedItem());
            destination.setSelectedItem(o);
        });
        JButton search = Toolbar.primary("Search", "search", this::reload);
        origin.getAccessibleContext().setAccessibleName("From");
        destination.getAccessibleContext().setAccessibleName("To");
        airline.getAccessibleContext().setAccessibleName("Airline");
        dates.getAccessibleContext().setAccessibleName("Travel dates");
        dates.setEnabled(false);
        anyDate.addActionListener(e -> {
            dates.setEnabled(!anyDate.isSelected());
            autoSearch.restart();
        });
        dates.addPropertyChangeListener(DateRangePicker.PROPERTY_RANGE, e -> autoSearch.restart());
        origin.addActionListener(e -> autoSearch.restart());
        destination.addActionListener(e -> autoSearch.restart());
        airline.addActionListener(e -> autoSearch.restart());

        Card criteria = new Card(new MigLayout("insets 0, fillx, gap 10 4",
                "[grow, fill]0[]0[grow, fill][grow 60, fill][][grow 60, fill][]", "[][]"));
        criteria.add(Typography.strong("From"));
        criteria.add(new JLabel());
        criteria.add(Typography.strong("To"));
        criteria.add(Typography.strong("Dates"));
        criteria.add(new JLabel());
        criteria.add(Typography.strong("Airline"), "wrap");
        criteria.add(origin);
        criteria.add(swap, "gapx 6 6");
        criteria.add(destination);
        criteria.add(dates);
        criteria.add(anyDate);
        criteria.add(airline);
        criteria.add(search);

        book = Toolbar.primary("Select seats", "armchair", () -> Tables.selected(table, model).ifPresent(this::book));
        book.setEnabled(false);
        table.getSelectionModel().addListSelectionListener(e -> book.setEnabled(table.getSelectedRow() >= 0));

        EmptyState empty = new EmptyState("plane", "No flights found",
                "Try other dates, choose \"Any date\", or widen the airline filter.");
        content = new ContentPane(Tables.scroll(table), empty);
        Card results = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        results.add(new Toolbar().left(Typography.h3("Available flights")).left(resultCount).right(book));
        results.add(content);

        add(promo, "hidemode 3");
        add(criteria);
        add(results, "grow, push");
        reloadOn(EventBus.Topic.FLIGHTS, EventBus.Topic.RESERVATIONS);
        loadReferenceData();
    }

    @Override
    public String title() {
        return "Find a flight";
    }

    @Override
    public String subtitle() {
        return "Search by origin, destination, date and airline";
    }

    @Override
    public String icon() {
        return "search";
    }

    @Override
    public void focusSearch() {
        origin.requestFocusInWindow();
    }

    private void loadReferenceData() {
        record Ref(List<Airport> airports, List<Airline> airlines, Promotion promo) {
        }
        BackgroundTask.of(() -> new Ref(ctx.flightSearch().airports(), ctx.flightSearch().airlines(),
                        ctx.promotions().currentPromotion().orElse(null)))
                .onSuccess(ref -> {
                    origin.setModel(new DefaultComboBoxModel<>(Choice.list("Any origin", ref.airports(), Airport::label)
                            .toArray(Choice[]::new)));
                    destination.setModel(new DefaultComboBoxModel<>(Choice.list("Any destination", ref.airports(),
                            Airport::label).toArray(Choice[]::new)));
                    airline.setModel(new DefaultComboBoxModel<>(Choice.list("Any airline", ref.airlines(),
                            Airline::name).toArray(Choice[]::new)));
                    if (ref.promo() != null && ref.promo().discountPercent() > 0) {
                        promo.show(Tone.SUCCESS, ref.promo().title() + ": " + ref.promo().discountPercent()
                                + "% off every fare booked this month. The discount is applied at checkout.");
                    }
                    autoSearch.stop();
                    reload();
                })
                .start(this);
    }

    @SuppressWarnings("unchecked")
    private static <T> T value(JComboBox<?> box) {
        Object o = box.getSelectedItem();
        return o instanceof Choice<?> c ? (T) c.value() : null;
    }

    @Override
    protected void reload() {
        Airport from = value(origin);
        Airport to = value(destination);
        Airline al = value(airline);
        LocalDate first = anyDate.isSelected() ? null : dates.start();
        LocalDate last = anyDate.isSelected() ? null : dates.end();
        content.loading();
        BackgroundTask.of(() -> ctx.flightSearch().search(from == null ? null : from.code(), to == null ? null : to.code(),
                        first, last, al == null ? null : al.code()))
                .onSuccess(list -> {
                    lastNow = ctx.flightSearch().now();
                    model.setRows(list);
                    resultCount.setText(list.size() >= 500 ? "Showing the first 500 flights"
                            : Formats.plural(list.size(), "flight", "flights"));
                    content.showResult(!list.isEmpty());
                })
                .onFailure(err -> {
                    content.showResult(false);
                    ca.ucalgary.frs.ui.components.UiErrors.show(this, err);
                })
                .start(this);
    }

    private void book(FlightAvailability a) {
        if (a.totalAvailable() == 0) {
            ca.ucalgary.frs.ui.components.Toasts.warning(this, "This flight is full. Please choose another flight.");
            return;
        }
        BookingDialog.open(this, ctx, a.flight());
    }
}
