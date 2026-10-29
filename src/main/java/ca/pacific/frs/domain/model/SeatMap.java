package ca.pacific.frs.domain.model;

import java.util.Set;

/** The seats of one flight and which of them are taken. */
public record SeatMap(Flight flight, Set<String> occupiedSeatCodes) {
    public SeatMap {
        occupiedSeatCodes = Set.copyOf(occupiedSeatCodes);
    }

    public Aircraft aircraft() {
        return flight.aircraft();
    }

    public boolean isAvailable(Seat seat) {
        return !occupiedSeatCodes.contains(seat.code());
    }

    public long availableCount(SeatClass seatClass) {
        return aircraft().allSeats().stream()
                .filter(s -> s.seatClass() == seatClass && isAvailable(s))
                .count();
    }
}
