package ca.pacific.frs.ui.components;

import ca.pacific.frs.domain.exception.BusinessRuleException;
import ca.pacific.frs.domain.exception.PersistenceException;

import java.awt.Component;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Turns exceptions into messages people can act on. Technical details go to the log, not the screen. */
public final class UiErrors {
    private static final Logger LOG = Logger.getLogger(UiErrors.class.getName());

    private UiErrors() {
    }

    public static String message(Throwable error) {
        if (error instanceof BusinessRuleException) {
            return error.getMessage();
        }
        if (error instanceof PersistenceException) {
            LOG.log(Level.WARNING, "Storage error", error);
            return "Your change could not be saved because the database is unavailable. Please try again.";
        }
        LOG.log(Level.SEVERE, "Unexpected error", error);
        return "Something went wrong. Please try again.";
    }

    public static void show(Component anchor, Throwable error) {
        Tone tone = error instanceof BusinessRuleException ? Tone.WARNING : Tone.DANGER;
        Toasts.show(anchor, tone, message(error), 6000);
    }
}
