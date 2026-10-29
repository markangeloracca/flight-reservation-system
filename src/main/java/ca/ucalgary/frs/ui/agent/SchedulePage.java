package ca.ucalgary.frs.ui.agent;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.control.EventBus;
import ca.ucalgary.frs.domain.model.FlightAvailability;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.Card;
import ca.ucalgary.frs.ui.components.ContentPane;
import ca.ucalgary.frs.ui.components.DateRangePicker;
import ca.ucalgary.frs.ui.components.EmptyState;
import ca.ucalgary.frs.ui.components.Formats;
import ca.ucalgary.frs.ui.components.ListTableModel;
import ca.ucalgary.frs.ui.components.SearchField;
import ca.ucalgary.frs.ui.components.Tables;
import ca.ucalgary.frs.ui.components.Toolbar;
import ca.ucalgary.frs.ui.shell.BasePage;
import ca.ucalgary.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.TableRowSorter;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Use case: View flight schedules (agents and administrators). Read-only. */
public final class SchedulePage extends BasePage {
    private final DateRangePicker dates = new DateRangePicker(LocalDate.now(), LocalDate.now().plusDays(6));
    private final SearchField filter = new SearchField("Filter by flight, city or aircraft", t -> applyFilter());
    private final JLabel count = Typography.caption(" ");
    private LocalDateTime now = LocalDateTime.now();
    private final ListTableModel<FlightAvailability> model = FlightColumns.model(() -> now);
    private final JTable table = Tables.create(model);
    private final ContentPane content;

    public SchedulePage(AppContext ctx) {
        super(ctx);
        FlightColumns.configure(table);
        dates.getAccessibleContext().setAccessibleName("Date range");
        dates.addPropertyChangeListener(DateRangePicker.PROPERTY_RANGE, e -> reload());
        content = new ContentPane(Tables.scroll(table),
                new EmptyState("calendar", "No flights in this period", "Choose a different date range."));
        Card card = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        card.add(new Toolbar().left(Typography.strong("Departing")).left(dates)
                .left(filter, "w 260!, gapx 12").left(count, "gapx 8"));
        card.add(content);
        add(card, "grow, push");
        reloadOn(EventBus.Topic.FLIGHTS, EventBus.Topic.RESERVATIONS);
    }

    @Override
    public String title() {
        return "Flight schedule";
    }

    @Override
    public String subtitle() {
        return "Departures, aircraft and seats sold for any date range";
    }

    @Override
    public String icon() {
        return "calendar";
    }

    @Override
    public void focusSearch() {
        filter.requestFocusInWindow();
    }

    @Override
    protected void reload() {
        LocalDate a = dates.start();
        LocalDate b = dates.end();
        content.loading();
        BackgroundTask.of(() -> ctx.flightSearch().schedule(a, b))
                .onSuccess(list -> {
                    now = ctx.flightSearch().now();
                    model.setRows(list);
                    applyFilter();
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
    }
}
