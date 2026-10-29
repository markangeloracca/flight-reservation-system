package ca.ucalgary.frs.ui.booking;

import ca.ucalgary.frs.app.AppContext;
import ca.ucalgary.frs.domain.model.Reservation;
import ca.ucalgary.frs.domain.model.ReservationStatus;
import ca.ucalgary.frs.ui.components.BackgroundTask;
import ca.ucalgary.frs.ui.components.Toasts;

import javax.swing.JOptionPane;
import java.awt.Component;
import java.time.LocalDateTime;

/** Shared commands for a selected reservation (used by the customer's and the agent's lists). */
public final class ReservationActions {
    private ReservationActions() {
    }

    public static boolean canViewConfirmation(Reservation r) {
        return r != null && r.status() != ReservationStatus.PENDING_PAYMENT;
    }

    public static boolean canChange(Reservation r, LocalDateTime now) {
        return r != null && r.status() == ReservationStatus.CONFIRMED && !r.flight().hasDeparted(now);
    }

    public static boolean canCancel(Reservation r, LocalDateTime now) {
        return r != null && r.holdsSeat() && !r.flight().hasDeparted(now);
    }

    public static void viewConfirmation(Component owner, AppContext ctx, Reservation r) {
        ConfirmationDialog.open(owner, ctx, r.id(), false);
    }

    public static void change(Component owner, AppContext ctx, Reservation r) {
        ModifyReservationDialog.open(owner, ctx, r);
    }

    /** Shows the refund the policy allows, asks for confirmation, then cancels. */
    public static void cancel(Component owner, AppContext ctx, Reservation r) {
        BackgroundTask.of(() -> ctx.booking().quoteCancellation(r.id()))
                .onSuccess(q -> {
                    String message = "<html><b>Cancel booking " + r.confirmationCode() + "?</b><br><br>"
                            + r.flight().flightNumber() + " · " + r.flight().route().label() + " · seat " + r.seat().code()
                            + "<br>Amount paid: " + q.amountPaid() + "<br>Refund: <b>" + q.refund() + "</b><br><br>"
                            + q.policy() + "<br>This cannot be undone.</html>";
                    int choice = JOptionPane.showConfirmDialog(owner, message, "Cancel booking",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (choice != JOptionPane.YES_OPTION) {
                        return;
                    }
                    BackgroundTask.of(() -> ctx.booking().cancel(r.id()))
                            .onSuccess(done -> Toasts.success(owner, "Booking " + r.confirmationCode() + " cancelled."
                                    + (done.refund().isPositive() ? " Refund of " + done.refund() + " issued." : "")))
                            .start(owner);
                })
                .start(owner);
    }
}
