package ca.ucalgary.frs.ui.admin;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.control.EventBus;
import ca.ucalgary.frs.domain.model.FlightAvailability;
import ca.ucalgary.frs.domain.model.FlightStatus;
import ca.ucalgary.frs.ui.agent.FlightColumns;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.Card;
import ca.ucalgary.frs.ui.components.ContentPane;
import ca.ucalgary.frs.ui.components.DateField;
import ca.ucalgary.frs.ui.components.EmptyState;
import ca.ucalgary.frs.ui.components.Formats;
import ca.ucalgary.frs.ui.components.ListTableModel;
import ca.ucalgary.frs.ui.components.SearchField;
import ca.ucalgary.frs.ui.components.Tables;
import ca.ucalgary.frs.ui.components.Toasts;
import ca.ucalgary.frs.ui.components.Toolbar;
import ca.ucalgary.frs.ui.shell.BasePage;
import ca.ucalgary.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Administrator use cases: Add, update, cancel and delete flights; remove outdated flights. */
public final class FlightsPage extends BasePage {
    private final DateField from = DateField.date(LocalDate.now());
    private final DateField to = DateField.date(LocalDate.now().plusDays(14));
    private final SearchField filter = new SearchField("Filter by flight, city or aircraft", t -> applyFilter());
    private final JLabel count = Typography.caption(" ");
    private LocalDateTime now = LocalDateTime.now();
    private final ListTableModel<FlightAvailability> model = FlightColumns.model(() -> now);
    private final JTable table = Tables.create(model);
    private final ContentPane content;
    private final JButton edit;
    private final JButton cancelFlight;
    private final JButton delete;

    public FlightsPage(AppContext ctx) {
        super(ctx);
        FlightColumns.configure(table);
        Tables.onActivate(table, model, this::edit);
        from.addChangeListener(e -> reload());
        to.addChangeListener(e -> reload());
        from.getAccessibleContext().setAccessibleName("From date");
        to.getAccessibleContext().setAccessibleName("To date");

        edit = Toolbar.button("Edit", "edit", () -> selected().ifPresent(this::edit));
        cancelFlight = Toolbar.button("Cancel flight", "x-circle", () -> selected().ifPresent(this::cancelFlight));
        delete = Toolbar.button("Delete", "trash", () -> selected().ifPresent(this::delete));
        JButton purge = Toolbar.button("Remove outdated", "eraser", this::removeOutdated);
        purge.setToolTipText("Delete departed flights that were never booked");
        table.getSelectionModel().addListSelectionListener(e -> updateActions());

        content = new ContentPane(Tables.scroll(table),
                new EmptyState("plane-takeoff", "No flights in this period", "Choose another date range or add a flight.")
                        .action("Add flight", this::add));
        Card card = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][][grow, fill]"));
        card.add(new Toolbar().left(Typography.strong("From")).left(from).left(Typography.strong("to")).left(to)
                .left(filter, "w 240!, gapx 12").left(count, "gapx 8"));
        card.add(new Toolbar().right(purge).right(delete).right(cancelFlight).right(edit)
                .right(Toolbar.primary("Add flight", "plus", this::add)), "gaptop 8");
        card.add(content);
        add(card, "grow, push");
        reloadOn(EventBus.Topic.FLIGHTS, EventBus.Topic.RESERVATIONS, EventBus.Topic.ROUTES, EventBus.Topic.AIRCRAFT);
        updateActions();
    }

    @Override
    public String title() {
        return "Flights";
    }

    @Override
    public String subtitle() {
        return "Add, update and retire flights and their schedules";
    }

    @Override
    public String icon() {
        return "plane-takeoff";
    }

    @Override
    public void focusSearch() {
        filter.requestFocusInWindow();
    }

    @Override
    protected void reload() {
        LocalDate a = from.getDate();
        LocalDate b = to.getDate();
        if (b.isBefore(a)) {
            return;
        }
        Long keep = selected().map(f -> f.flight().id()).orElse(null);
        content.loading();
        BackgroundTask.of(() -> ctx.flightManagement().flights(a, b))
                .onSuccess(list -> {
                    now = ctx.flightSearch().now();
                    model.setRows(list);
                    applyFilter();
                    if (keep != null) {
                        Tables.reselect(table, model, f -> f.flight().id().equals(keep));
                    }
                })
                .start(this);
    }

    private void applyFilter() {
        String text = filter.getText().toLowerCase();
        @SuppressWarnings("unchecked")
        TableRowSorter<ListTableModel<FlightAvailability>> sorter =
                (TableRowSorter<ListTableModel<FlightAvailability>>) table.getRowSorter();
        sorter.setRowFilter(text.isBlank() ? null : new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends ListTableModel<FlightAvailability>, ? extends Integer> entry) {
                return FlightColumns.searchText(model.row(entry.getIdentifier())).contains(text);
            }
        });
        count.setText(Formats.plural(table.getRowCount(), "flight", "flights"));
        content.showResult(table.getRowCount() > 0);
        updateActions();
    }

    private java.util.Optional<FlightAvailability> selected() {
        return Tables.selected(table, model);
    }

    private void updateActions() {
        FlightAvailability a = selected().orElse(null);
        boolean live = a != null && a.flight().status() != FlightStatus.CANCELLED;
        edit.setEnabled(live);
        cancelFlight.setEnabled(live && !a.flight().hasDeparted(now));
        delete.setEnabled(a != null);
    }

    private void add() {
        FlightEditorDialog.open(this, ctx, null, f -> Toasts.success(this, "Flight " + f.flightNumber() + " added."));
    }

    private void edit(FlightAvailability a) {
        if (a.flight().status() == FlightStatus.CANCELLED) {
            Toasts.warning(this, "A cancelled flight cannot be edited.");
            return;
        }
        FlightEditorDialog.open(this, ctx, a.flight(), f -> Toasts.success(this, "Flight " + f.flightNumber() + " saved."));
    }

    private void cancelFlight(FlightAvailability a) {
        int booked = a.capacity() - a.totalAvailable();
        int choice = JOptionPane.showConfirmDialog(this, "<html><b>Cancel " + a.flight().flightNumber() + " on "
                        + Formats.dateTime(a.flight().departureTime()) + "?</b><br><br>"
                        + Formats.plural(booked, "booking", "bookings") + " will be cancelled and refunded in full.<br>"
                        + "This cannot be undone.</html>",
                "Cancel flight", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        BackgroundTask.of(() -> ctx.flightManagement().cancelFlight(a.flight().id()))
                .onSuccess(n -> Toasts.success(this, a.flight().flightNumber() + " cancelled. "
                        + Formats.plural(n, "passenger was", "passengers were") + " refunded."))
                .start(this);
    }

    private void delete(FlightAvailability a) {
        int choice = JOptionPane.showConfirmDialog(this, "Delete " + a.flight().flightNumber() + " on "
                        + Formats.dateTime(a.flight().departureTime()) + "? This cannot be undone.",
                "Delete flight", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        BackgroundTask.run(() -> ctx.flightManagement().deleteFlight(a.flight().id()))
                .onSuccess(v -> Toasts.success(this, "Flight deleted."))
                .start(this);
    }

    private void removeOutdated() {
        int choice = JOptionPane.showConfirmDialog(this, "<html>Delete every flight that has already departed and was "
                        + "never booked?<br>Flights with booking history are kept for records.</html>",
                "Remove outdated flights", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        BackgroundTask.of(() -> ctx.flightManagement().removeOutdatedFlights())
                .onSuccess(n -> Toasts.success(this, n == 0 ? "No outdated flights to remove."
                        : Formats.plural(n, "outdated flight", "outdated flights") + " removed."))
                .start(this);
    }
}
