package ca.ucalgary.frs.ui.components;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Display formats used across the UI. */
public final class Formats {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);
    public static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("EEE, MMM d · HH:mm", Locale.ENGLISH);
    public static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ENGLISH);

    private Formats() {
    }

    public static String date(LocalDate d) {
        return d == null ? "" : d.format(DATE);
    }

    public static String dateTime(LocalDateTime t) {
        return t == null ? "" : t.format(DATE_TIME);
    }

    public static String duration(Duration d) {
        return d.toHours() + "h " + String.format("%02dm", d.toMinutesPart());
    }

    public static String plural(int n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }
}
