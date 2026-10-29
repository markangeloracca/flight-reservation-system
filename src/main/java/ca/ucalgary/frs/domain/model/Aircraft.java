package ca.ucalgary.frs.domain.model;

import ca.ucalgary.frs.domain.exception.ValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * An aircraft and its cabin layout. Business rows come first (row 1..businessRows), then economy.
 */
public record Aircraft(Long id, String registration, String model, int businessRows, int economyRows,
                       int seatsPerRow) {

    public static final int MAX_ROWS = 60;

    public Aircraft {
        registration = Validate.maxLength("registration", "Registration",
                Validate.required("registration", "Registration", registration).toUpperCase(), 10);
        model = Validate.maxLength("model", "Model", Validate.required("model", "Model", model), 60);
        Validate.range("businessRows", "Business rows", businessRows, 0, 20);
        Validate.range("economyRows", "Economy rows", economyRows, 1, 50);
        if (seatsPerRow != 4 && seatsPerRow != 6) {
            throw new ValidationException("seatsPerRow", "Seats per row must be 4 or 6.");
        }
        if (businessRows + economyRows > MAX_ROWS) {
            throw new ValidationException("economyRows", "An aircraft can have at most " + MAX_ROWS + " rows.");
        }
    }

    public Aircraft withId(long newId) {
        return new Aircraft(newId, registration, model, businessRows, economyRows, seatsPerRow);
    }

    public int totalRows() {
        return businessRows + economyRows;
    }

    public int capacity(SeatClass seatClass) {
        return (seatClass == SeatClass.BUSINESS ? businessRows : economyRows) * seatsPerRow;
    }

    public int capacity() {
        return totalRows() * seatsPerRow;
    }

    /** Seat letters for one row: A-D for 4-abreast, A-F for 6-abreast. */
    public char[] letters() {
        return seatsPerRow == 4 ? new char[]{'A', 'B', 'C', 'D'} : new char[]{'A', 'B', 'C', 'D', 'E', 'F'};
    }

    /** Index after which the aisle sits (between C|D for 6-abreast, B|C for 4-abreast). */
    public int aisleAfterIndex() {
        return seatsPerRow / 2 - 1;
    }

    public SeatClass classOfRow(int row) {
        return row <= businessRows ? SeatClass.BUSINESS : SeatClass.ECONOMY;
    }

    public List<Seat> allSeats() {
        List<Seat> seats = new ArrayList<>(capacity());
        for (int row = 1; row <= totalRows(); row++) {
            for (char letter : letters()) {
                seats.add(new Seat(row, letter, classOfRow(row)));
            }
        }
        return Collections.unmodifiableList(seats);
    }

    /** Resolves a seat code such as {@code "3B"} on this aircraft, if it exists. */
    public Optional<Seat> seat(String code) {
        int[] parts;
        try {
            parts = Seat.parse(code);
        } catch (ValidationException e) {
            return Optional.empty();
        }
        int row = parts[0];
        char letter = (char) parts[1];
        if (row < 1 || row > totalRows() || new String(letters()).indexOf(letter) < 0) {
            return Optional.empty();
        }
        return Optional.of(new Seat(row, letter, classOfRow(row)));
    }

    public Seat requireSeat(String code) {
        return seat(code).orElseThrow(() -> new ValidationException("seat", "Seat " + code + " does not exist on this aircraft."));
    }

    @Override
    public String toString() {
        return registration + " · " + model;
    }
}
