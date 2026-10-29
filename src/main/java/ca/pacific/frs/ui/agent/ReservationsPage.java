package ca.pacific.frs.ui.agent;

import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.control.EventBus;
import ca.pacific.frs.domain.model.Reservation;
import ca.pacific.frs.domain.model.ReservationStatus;
import ca.pacific.frs.ui.booking.ReservationActions;
import ca.pacific.frs.ui.booking.ReservationColumns;
import ca.pacific.frs.ui.components.BackgroundTask;
import ca.pacific.frs.ui.components.Card;
import ca.pacific.frs.ui.components.Choice;
import ca.pacific.frs.ui.components.ContentPane;
import ca.pacific.frs.ui.components.EmptyState;
import ca.pacific.frs.ui.components.Formats;
import ca.pacific.frs.ui.components.ListTableModel;
import ca.pacific.frs.ui.components.SearchField;
import ca.pacific.frs.ui.components.Tables;
import ca.pacific.frs.ui.components.Toolbar;
import ca.pacific.frs.ui.shell.BasePage;
import ca.pacific.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import java.time.LocalDateTime;
import java.util.Arrays;

/** Agent use cases: Find any reservation, view its confirmation, change or cancel it. */
public final class ReservationsPage extends BasePage {
    private final SearchField search = new SearchField("Reference, customer, email or flight number", t -> reload());
    private final JComboBox<Choice<ReservationStatus>> status = new JComboBox<>(
            Choice.list("All statuses", Arrays.asList(ReservationStatus.values()), ReservationStatus::displayName)
                    .toArray(Choice[]::new));
    private final ListTableModel<Reservation> model = ReservationColumns.model(true);
    private final JTable table = Tables.create(model);
    private final ContentPane content;
    private final JLabel count = Typography.caption(" ");
    private final JButton view;
    private final JButton change;
    private final JButton cancel;
    private LocalDateTime now = LocalDateTime.now();

    @SuppressWarnings("unchecked")
    public ReservationsPage(AppContext ctx) {
        super(ctx);
        ReservationColumns.widths(table, true);
        Tables.onActivate(table, model, r -> {
            if (ReservationActions.canViewConfirmation(r)) {
                ReservationActions.viewConfirmation(this, ctx, r);
            }
        });
        status.getAccessibleContext().setAccessibleName("Status filter");
        status.addActionListener(e -> reload());
        view = Toolbar.button("Confirmation", "file-text", () -> selected(r -> ReservationActions.viewConfirmation(this, ctx, r)));
        change = Toolbar.button("Change", "edit", () -> selected(r -> ReservationActions.change(this, ctx, r)));
        cancel = Toolbar.button("Cancel", "x-circle", () -> selected(r -> ReservationActions.cancel(this, ctx, r)));
        table.getSelectionModel().addListSelectionListener(e -> updateActions());

        content = new ContentPane(Tables.scroll(table),
                new EmptyState("inbox", "No reservations match", "Try a booking reference such as M4TRVL, or clear the filters."));
        Card card = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        card.add(new Toolbar().left(search, "w 320!").left(status).left(count, "gapx 8")
                .right(view).right(change).right(cancel));
        card.add(content);
        add(card, "grow, push");
        reloadOn(EventBus.Topic.RESERVATIONS, EventBus.Topic.CUSTOMERS);
        updateActions();
    }

    @Override
    public String title() {
        return "Reservations";
    }

    @Override
    public String subtitle() {
        return "Look up, change and cancel bookings for any customer";
    }

    @Override
    public String icon() {
        return "ticket";
    }

    @Override
    public void focusSearch() {
        search.requestFocusInWindow();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void reload() {
        String text = search.getText();
        ReservationStatus st = ((Choice<ReservationStatus>) status.getSelectedItem()).value();
        Long keep = Tables.selected(table, model).map(Reservation::id).orElse(null);
        if (model.getRowCount() == 0) {
            content.loading();
        }
        BackgroundTask.of(() -> ctx.booking().search(text, st))
                .onSuccess(list -> {
                    now = ctx.flightSearch().now();
                    model.setRows(list);
                    count.setText(Formats.plural(list.size(), "reservation", "reservations"));
                    content.showResult(!list.isEmpty());
                    if (keep != null) {
                        Tables.reselect(table, model, r -> r.id().equals(keep));
                    }
                    updateActions();
                })
                .start(this);
    }

    private void selected(java.util.function.Consumer<Reservation> action) {
        Tables.selected(table, model).ifPresent(action);
    }

    private void updateActions() {
        Reservation r = Tables.selected(table, model).orElse(null);
        view.setEnabled(ReservationActions.canViewConfirmation(r));
        change.setEnabled(ReservationActions.canChange(r, now));
        cancel.setEnabled(ReservationActions.canCancel(r, now));
    }
}
