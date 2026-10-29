package ca.ucalgary.frs.domain.model;

/** A search result: a flight plus how many seats are still free in each cabin. */
public record FlightAvailability(Flight flight, int economyAvailable, int businessAvailable) {

    public int available(SeatClass seatClass) {
        return seatClass == SeatClass.BUSINESS ? businessAvailable : economyAvailable;
    }

    public int totalAvailable() {
        return economyAvailable + businessAvailable;
    }

    public int capacity() {
        return flight.aircraft().capacity();
    }

    /** Percentage of seats sold or held, 0-100. */
    public int loadFactorPercent() {
        int cap = capacity();
        return cap == 0 ? 0 : (int) Math.round(100.0 * (cap - totalAvailable()) / cap);
    }

    /** Lowest fare among cabins that still have seats, or the economy fare if sold out. */
    public Money lowestFare() {
        if (economyAvailable > 0 || businessAvailable == 0) {
            return flight.fareFor(SeatClass.ECONOMY);
        }
        return flight.fareFor(SeatClass.BUSINESS);
    }
}
