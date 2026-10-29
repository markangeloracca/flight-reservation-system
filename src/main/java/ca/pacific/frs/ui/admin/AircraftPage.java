package ca.pacific.frs.ui.admin;

import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.control.EventBus;
import ca.pacific.frs.domain.model.Aircraft;
import ca.pacific.frs.ui.components.BackgroundTask;
import ca.pacific.frs.ui.components.Card;
import ca.pacific.frs.ui.components.ContentPane;
import ca.pacific.frs.ui.components.EmptyState;
import ca.pacific.frs.ui.components.ListTableModel;
import ca.pacific.frs.ui.components.Tables;
import ca.pacific.frs.ui.components.Toasts;
import ca.pacific.frs.ui.components.Toolbar;
import ca.pacific.frs.ui.shell.BasePage;
import net.miginfocom.swing.MigLayout;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JTable;

import static ca.pacific.frs.ui.components.ListTableModel.column;

/** Administrator use case: Manage aircraft. */
public final class AircraftPage extends BasePage {
    private final ListTableModel<Aircraft> model = new ListTableModel<>(
            column("Registration", String.class, Aircraft::registration),
            column("Model", String.class, Aircraft::model),
            column("Business rows", Integer.class, Aircraft::businessRows),
            column("Economy rows", Integer.class, Aircraft::economyRows),
            column("Seats per row", Integer.class, Aircraft::seatsPerRow),
            column("Capacity", Integer.class, (Aircraft a) -> a.capacity()));
    private final JTable table = Tables.create(model);
    private final ContentPane content;
    private final JButton edit;
    private final JButton delete;

    public AircraftPage(AppContext ctx) {
        super(ctx);
        Tables.widths(table, 110, 220, 110, 110, 110, 90);
        Tables.onActivate(table, model, this::edit);
        edit = Toolbar.button("Edit", "edit", () -> Tables.selected(table, model).ifPresent(this::edit));
        delete = Toolbar.button("Delete", "trash", () -> Tables.selected(table, model).ifPresent(this::delete));
        table.getSelectionModel().addListSelectionListener(e -> updateActions());
        content = new ContentPane(Tables.scroll(table), new EmptyState("plane", "No aircraft yet",
                "Add an aircraft to assign it to flights.").action("Add aircraft", this::add));
        Card card = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        card.add(new Toolbar().right(delete).right(edit).right(Toolbar.primary("Add aircraft", "plus", this::add)));
        card.add(content);
        add(card, "grow, push");
        reloadOn(EventBus.Topic.AIRCRAFT);
        updateActions();
    }

    @Override
    public String title() {
        return "Aircraft";
    }

    @Override
    public String subtitle() {
        return "The fleet and each aircraft's seat layout";
    }

    @Override
    public String icon() {
        return "plane";
    }

    @Override
    protected void reload() {
        content.loading();
        BackgroundTask.of(() -> ctx.routesAndAircraft().aircraft())
                .onSuccess(list -> {
                    model.setRows(list);
                    content.showResult(!list.isEmpty());
                    updateActions();
                })
                .start(this);
    }

    private void updateActions() {
        boolean has = table.getSelectedRow() >= 0;
        edit.setEnabled(has);
        delete.setEnabled(has);
    }

    private void add() {
        AircraftEditorDialog.show(this, ctx, null)
                .ifPresent(a -> Toasts.success(this, "Aircraft " + a.registration() + " added."));
    }

    private void edit(Aircraft a) {
        AircraftEditorDialog.show(this, ctx, a)
                .ifPresent(saved -> Toasts.success(this, "Aircraft " + saved.registration() + " saved."));
    }

    private void delete(Aircraft a) {
        if (JOptionPane.showConfirmDialog(this, "Delete aircraft " + a.registration() + "?", "Delete aircraft",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        BackgroundTask.run(() -> ctx.routesAndAircraft().deleteAircraft(a.id()))
                .onSuccess(v -> Toasts.success(this, "Aircraft " + a.registration() + " deleted."))
                .start(this);
    }
}
