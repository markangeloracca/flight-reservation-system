package ca.pacific.frs.ui.customer;

import ca.pacific.frs.ui.components.Toasts;
import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.control.EventBus;
import ca.pacific.frs.domain.model.Customer;
import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.model.Payment;
import ca.pacific.frs.domain.model.Reservation;
import ca.pacific.frs.ui.booking.ReservationActions;
import ca.pacific.frs.ui.booking.ReservationColumns;
import ca.pacific.frs.ui.booking.StatusViews;
import ca.pacific.frs.ui.components.Badge;
import ca.pacific.frs.ui.components.BackgroundTask;
import ca.pacific.frs.ui.components.Card;
import ca.pacific.frs.ui.components.ContentPane;
import ca.pacific.frs.ui.components.EmptyState;
import ca.pacific.frs.ui.components.Formats;
import ca.pacific.frs.ui.components.ListTableModel;
import ca.pacific.frs.ui.components.SearchField;
import ca.pacific.frs.ui.components.Tables;
import ca.pacific.frs.ui.components.Tables.Labeled;
import ca.pacific.frs.ui.components.Tone;
import ca.pacific.frs.ui.components.Toolbar;
import ca.pacific.frs.ui.shell.BasePage;
import ca.pacific.frs.ui.theme.Typography;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import java.awt.CardLayout;
import java.time.LocalDateTime;
import java.util.List;

import static ca.pacific.frs.ui.components.ListTableModel.column;

/** Agent use cases: Search customers, create and edit profiles, view their bookings and payments. */
public final class CustomersPage extends BasePage {
    private final SearchField search = new SearchField("Search by name, email or phone", t -> reload());
    private final ListTableModel<Customer> customers = new ListTableModel<>(
            column("Name", String.class, Customer::fullName),
            column("Email", String.class, Customer::email),
            column("Phone", String.class, (Customer c) -> c.phone() == null ? "—" : c.phone()));
    private final JTable table = Tables.create(customers);
    private final ContentPane listContent;
    private final JButton edit;

    private final CardLayout detailCards = new CardLayout();
    private final JPanel detail = new JPanel(detailCards);
    private final JLabel name = Typography.h2("");
    private final JLabel contact = Typography.caption("");
    private final JLabel extra = Typography.caption("");
    private final Badge account = new Badge();
    private final ListTableModel<Reservation> reservations = ReservationColumns.compact();
    private final JTable reservationTable = Tables.create(reservations);
    private final ListTableModel<Payment> payments = new ListTableModel<>(
            column("Date", LocalDateTime.class, Payment::createdAt),
            column("Type", String.class, (Payment p) -> p.type().toString()),
            column("Amount", Money.class, Payment::amount),
            column("Card", String.class, Payment::maskedCard),
            column("Status", Labeled.class, (Payment p) -> StatusViews.payment(p.type(), p.status())));
    private final JButton view;
    private final JButton change;
    private final JButton cancel;
    private LocalDateTime now = LocalDateTime.now();

    public CustomersPage(AppContext ctx) {
        super(ctx);
        Tables.widths(table, 160, 220, 120);
        Tables.onActivate(table, customers, this::editCustomer);
        edit = Toolbar.button("Edit", "edit", () -> Tables.selected(table, customers).ifPresent(this::editCustomer));
        listContent = new ContentPane(Tables.scroll(table),
                new EmptyState("users", "No customers found", "Check the spelling or create a new customer profile.")
                        .action("New customer", this::newCustomer));

        Card list = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        list.add(new Toolbar().left(search, "growx, pushx, wmin 200").right(edit)
                .right(Toolbar.primary("New customer", "plus", this::newCustomer)));
        list.add(listContent);

        Tables.widths(reservationTable, 80, 120, 150, 110, 80);
        Tables.onActivate(reservationTable, reservations, r -> {
            if (ReservationActions.canViewConfirmation(r)) {
                ReservationActions.viewConfirmation(this, ctx, r);
            }
        });
        view = Toolbar.button("Confirmation", "file-text", () -> withReservation(r -> ReservationActions.viewConfirmation(this, ctx, r)));
        change = Toolbar.button("Change", "edit", () -> withReservation(r -> ReservationActions.change(this, ctx, r)));
        cancel = Toolbar.button("Cancel", "x-circle", () -> withReservation(r -> ReservationActions.cancel(this, ctx, r)));
        reservationTable.getSelectionModel().addListSelectionListener(e -> updateActions());

        JPanel bookings = new JPanel(new MigLayout("insets 8 0 0 0, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        bookings.setOpaque(false);
        bookings.add(new Toolbar().right(view).right(change).right(cancel));
        bookings.add(Tables.scroll(reservationTable));
        JTable paymentTable = Tables.create(payments);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Bookings", bookings);
        tabs.addTab("Payments", Tables.scroll(paymentTable));

        JPanel info = new JPanel(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][][][grow, fill]"));
        info.setOpaque(false);
        JPanel title = new JPanel(new MigLayout("insets 0, gap 10", "[][]", "[center]"));
        title.setOpaque(false);
        title.add(name);
        title.add(account);
        info.add(title);
        info.add(contact);
        info.add(extra, "gapbottom 8");
        info.add(tabs);
        detail.setOpaque(false);
        detail.add(new EmptyState("user", "Select a customer", "Their bookings and payment records will appear here."), "empty");
        detail.add(info, "info");
        Card detailCard = new Card(new MigLayout("insets 0, fill", "[grow, fill]", "[grow, fill]"));
        detailCard.add(detail);

        JPanel split = new JPanel(new MigLayout("insets 0, fill, gap 16", "[45%, fill][55%, fill]", "[grow, fill]"));
        split.setOpaque(false);
        split.add(list);
        split.add(detailCard);
        add(split, "grow, push");

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDetail();
            }
        });
        reloadOn(EventBus.Topic.CUSTOMERS, EventBus.Topic.RESERVATIONS, EventBus.Topic.PAYMENTS);
        updateActions();
    }

    @Override
    public String title() {
        return "Customers";
    }

    @Override
    public String subtitle() {
        return "Create, find and update customer profiles";
    }

    @Override
    public String icon() {
        return "users";
    }

    @Override
    public void focusSearch() {
        search.requestFocusInWindow();
    }

    @Override
    protected void reload() {
        Long keep = Tables.selected(table, customers).map(Customer::id).orElse(null);
        if (customers.getRowCount() == 0) {
            listContent.loading();
        }
        String text = search.getText();
        BackgroundTask.of(() -> ctx.customers().search(text))
                .onSuccess(list -> {
                    customers.setRows(list);
                    listContent.showResult(!list.isEmpty());
                    if (keep != null) {
                        Tables.reselect(table, customers, c -> c.id().equals(keep));
                    }
                    showDetail();
                })
                .start(this);
    }

    private void showDetail() {
        Customer c = Tables.selected(table, customers).orElse(null);
        edit.setEnabled(c != null);
        if (c == null) {
            detailCards.show(detail, "empty");
            return;
        }
        detailCards.show(detail, "info");
        name.setText(c.fullName());
        contact.setText(c.email() + (c.phone() != null ? "  ·  " + c.phone() : ""));
        extra.setText((c.dateOfBirth() != null ? "Born " + Formats.date(c.dateOfBirth()) : "Date of birth not given")
                + (c.address() != null ? "  ·  " + c.address() : ""));
        record Detail(boolean hasAccount, List<Reservation> res, List<Payment> pay, LocalDateTime now) {
        }
        BackgroundTask.of(() -> new Detail(ctx.customers().hasAccount(c.id()), ctx.customers().reservations(c.id()),
                        ctx.customers().payments(c.id()), ctx.flightSearch().now()))
                .onSuccess(d -> {
                    now = d.now();
                    account.set(d.hasAccount() ? "Online account" : "Walk-in", d.hasAccount() ? Tone.ACCENT : Tone.NEUTRAL);
                    reservations.setRows(d.res());
                    payments.setRows(d.pay());
                    updateActions();
                })
                .start(this);
    }

    private void newCustomer() {
        CustomerEditorDialog.show(this, ctx, null).ifPresent(c -> {
            search.setText("");
            Toasts.success(this, "Customer " + c.fullName() + " created.");
        });
    }

    private void editCustomer(Customer c) {
        CustomerEditorDialog.show(this, ctx, c)
                .ifPresent(saved -> Toasts.success(this, "Saved " + saved.fullName() + "."));
    }

    private void withReservation(java.util.function.Consumer<Reservation> action) {
        Tables.selected(reservationTable, reservations).ifPresent(action);
    }

    private void updateActions() {
        Reservation r = Tables.selected(reservationTable, reservations).orElse(null);
        view.setEnabled(ReservationActions.canViewConfirmation(r));
        change.setEnabled(ReservationActions.canChange(r, now));
        cancel.setEnabled(ReservationActions.canCancel(r, now));
    }
}
