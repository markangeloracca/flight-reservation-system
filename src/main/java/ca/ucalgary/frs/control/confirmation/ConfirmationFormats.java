package ca.ucalgary.frs.control.confirmation;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Shared formatting for confirmation documents. */
final class ConfirmationFormats {
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.ENGLISH);
    static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);

    private ConfirmationFormats() {
    }

    static String duration(Duration d) {
        return d.toHours() + "h " + String.format("%02d", d.toMinutesPart()) + "m";
    }
}
