package ca.pacific.frs.ui.components;

import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * Spinner-based date or date-time input that speaks {@code java.time}. Arrow keys step the field under
 * the caret (day, month, hour, ...).
 */
public class DateField extends JSpinner {
    private final boolean withTime;

    private DateField(boolean withTime) {
        super(new SpinnerDateModel(new Date(), null, null, withTime ? Calendar.MINUTE : Calendar.DAY_OF_MONTH));
        this.withTime = withTime;
        setEditor(new JSpinner.DateEditor(this, withTime ? "yyyy-MM-dd  HH:mm" : "EEE, MMM d, yyyy"));
    }

    public static DateField date(LocalDate initial) {
        DateField f = new DateField(false);
        f.setDate(initial);
        return f;
    }

    public static DateField dateTime(LocalDateTime initial) {
        DateField f = new DateField(true);
        f.setDateTime(initial);
        return f;
    }

    public LocalDate getDate() {
        return toLocal().toLocalDate();
    }

    public void setDate(LocalDate d) {
        setValue(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    public LocalDateTime getDateTime() {
        LocalDateTime t = toLocal();
        return withTime ? t.withSecond(0).withNano(0) : t.toLocalDate().atStartOfDay();
    }

    public void setDateTime(LocalDateTime t) {
        setValue(Date.from(t.atZone(ZoneId.systemDefault()).toInstant()));
    }

    private LocalDateTime toLocal() {
        return LocalDateTime.ofInstant(((Date) getValue()).toInstant(), ZoneId.systemDefault());
    }
}
