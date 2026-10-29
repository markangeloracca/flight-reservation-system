package ca.ucalgary.frs.domain.model;

import ca.ucalgary.frs.domain.exception.ValidationException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A seat on an aircraft, such as {@code 12C}. */
public record Seat(int row, char letter, SeatClass seatClass) implements Comparable<Seat> {
    private static final Pattern CODE = Pattern.compile("^(\\d{1,2})([A-F])$");

    public String code() {
        return row + String.valueOf(letter);
    }

    /** Parses a seat code into row and letter. The class is resolved by the {@link Aircraft}. */
    public static int[] parse(String code) {
        Matcher m = CODE.matcher(code == null ? "" : code.trim().toUpperCase());
        if (!m.matches()) {
            throw new ValidationException("seat", "Invalid seat: " + code);
        }
        return new int[]{Integer.parseInt(m.group(1)), m.group(2).charAt(0)};
    }

    @Override
    public int compareTo(Seat o) {
        return row != o.row ? Integer.compare(row, o.row) : Character.compare(letter, o.letter);
    }

    @Override
    public String toString() {
        return code() + " (" + seatClass.displayName() + ")";
    }
}
