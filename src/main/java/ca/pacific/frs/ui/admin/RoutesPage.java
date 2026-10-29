package ca.pacific.frs.ui.admin;

import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.control.EventBus;
import ca.pacific.frs.domain.model.Route;
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

/** Administrator use case: Manage routes. */
public final class RoutesPage extends BasePage {
    private final ListTableModel<Route> model = new ListTableModel<>(
            column("Route", String.class, Route::label),
            column("Origin", String.class, (Route r) -> r.origin().name()),
            column("Destination", String.class, (Route r) -> r.destination().name()),
            column("Distance (km)", Integer.class, Route::distanceKm));
    private final JTable table = Tables.create(model);
    private final ContentPane content;
    private final JButton edit;
    private final JButton delete;

    public RoutesPage(AppContext ctx) {
        super(ctx);
        Tables.widths(table, 110, 280, 280, 110);
        Tables.onActivate(table, model, this::edit);
        edit = Toolbar.button("Edit", "edit", () -> Tables.selected(table, model).ifPresent(this::edit));
        delete = Toolbar.button("Delete", "trash", () -> Tables.selected(table, model).ifPresent(this::delete));
        table.getSelectionModel().addListSelectionListener(e -> updateActions());
        content = new ContentPane(Tables.scroll(table), new EmptyState("route", "No routes yet",
                "Add a route to start scheduling flights on it.").action("Add route", this::add));
        Card card = new Card(new MigLayout("insets 0, fill, wrap 1", "[grow, fill]", "[][grow, fill]"));
        card.add(new Toolbar().right(delete).right(edit).right(Toolbar.primary("Add route", "plus", this::add)));
        card.add(content);
        add(card, "grow, push");
        reloadOn(EventBus.Topic.ROUTES);
        updateActions();
    }

    @Override
    public String title() {
        return "Routes";
    }

    @Override
    public String subtitle() {
        return "City pairs that flights can be scheduled on";
    }

    @Override
    public String icon() {
        return "route";
    }

    @Override
    protected void reload() {
        content.loading();
        BackgroundTask.of(() -> ctx.routesAndAircraft().routes())
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
        RouteEditorDialog.open(this, ctx, null, r -> Toasts.success(this, "Route " + r.label() + " added."));
    }

    private void edit(Route r) {
        RouteEditorDialog.open(this, ctx, r, saved -> Toasts.success(this, "Route " + saved.label() + " saved."));
    }

    private void delete(Route r) {
        if (JOptionPane.showConfirmDialog(this, "Delete route " + r.label() + "?", "Delete route",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        BackgroundTask.run(() -> ctx.routesAndAircraft().deleteRoute(r.id()))
                .onSuccess(v -> Toasts.success(this, "Route " + r.label() + " deleted."))
                .start(this);
    }
}
