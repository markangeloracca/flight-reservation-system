package ca.ucalgary.frs.domain.model;

import ca.ucalgary.frs.domain.exception.ValidationException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A scheduled flight on a route, operated by an airline with a specific aircraft.
 * Times are local airport times. Immutable; edit through {@link #toBuilder()}.
 */
public final class Flight {
    private static final Pattern FLIGHT_NUMBER = Pattern.compile("^[A-Z0-9]{2}\\d{1,4}$");

    private final Long id;
    private final String flightNumber;
    private final Airline airline;
    private final Route route;
    private final Aircraft aircraft;
    private final LocalDateTime departureTime;
    private final LocalDateTime arrivalTime;
    private final Money baseFare;
    private final FlightStatus status;

    private Flight(Builder b) {
        this.id = b.id;
        String number = Validate.required("flightNumber", "Flight number", b.flightNumber).toUpperCase().replace(" ", "");
        if (!FLIGHT_NUMBER.matcher(number).matches()) {
            throw new ValidationException("flightNumber", "Flight number must look like AC123 (airline code + 1-4 digits).");
        }
        this.airline = Validate.notNull("airline", "Airline", b.airline);
        if (!number.startsWith(airline.code())) {
            throw new ValidationException("flightNumber", "Flight number must start with the airline code " + airline.code() + ".");
        }
        this.flightNumber = number;
        this.route = Validate.notNull("route", "Route", b.route);
        this.aircraft = Validate.notNull("aircraft", "Aircraft", b.aircraft);
        this.departureTime = Validate.notNull("departureTime", "Departure time", b.departureTime);
        this.arrivalTime = Validate.notNull("arrivalTime", "Arrival time", b.arrivalTime);
        if (!arrivalTime.isAfter(departureTime)) {
            throw new ValidationException("arrivalTime", "Arrival must be after departure.");
        }
        if (Duration.between(departureTime, arrivalTime).toHours() > 24) {
            throw new ValidationException("arrivalTime", "A flight cannot last more than 24 hours.");
        }
        this.baseFare = Validate.notNull("baseFare", "Base fare", b.baseFare);
        if (!baseFare.isPositive()) {
            throw new ValidationException("baseFare", "Base fare must be greater than zero.");
        }
        this.status = b.status == null ? FlightStatus.SCHEDULED : b.status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().id(id).flightNumber(flightNumber).airline(airline).route(route).aircraft(aircraft)
                .departureTime(departureTime).arrivalTime(arrivalTime).baseFare(baseFare).status(status);
    }

    public Duration duration() {
        return Duration.between(departureTime, arrivalTime);
    }

    public boolean hasDeparted(LocalDateTime now) {
        return !departureTime.isAfter(now);
    }

    /** A flight can be booked if it is not cancelled and has not departed. */
    public boolean isBookable(LocalDateTime now) {
        return status != FlightStatus.CANCELLED && !hasDeparted(now);
    }

    /** Fare for one seat in the given class, before discounts and tax. */
    public Money fareFor(SeatClass seatClass) {
        return baseFare.times(seatClass.fareMultiplier());
    }

    public Long id() {
        return id;
    }

    public String flightNumber() {
        return flightNumber;
    }

    public Airline airline() {
        return airline;
    }

    public Route route() {
        return route;
    }

    public Aircraft aircraft() {
        return aircraft;
    }

    public LocalDateTime departureTime() {
        return departureTime;
    }

    public LocalDateTime arrivalTime() {
        return arrivalTime;
    }

    public Money baseFare() {
        return baseFare;
    }

    public FlightStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Flight f && id != null && id.equals(f.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return flightNumber + " " + route.label() + " " + departureTime;
    }

    public static final class Builder {
        private Long id;
        private String flightNumber;
        private Airline airline;
        private Route route;
        private Aircraft aircraft;
        private LocalDateTime departureTime;
        private LocalDateTime arrivalTime;
        private Money baseFare;
        private FlightStatus status;

        public Builder id(Long v) {
            this.id = v;
            return this;
        }

        public Builder flightNumber(String v) {
            this.flightNumber = v;
            return this;
        }

        public Builder airline(Airline v) {
            this.airline = v;
            return this;
        }

        public Builder route(Route v) {
            this.route = v;
            return this;
        }

        public Builder aircraft(Aircraft v) {
            this.aircraft = v;
            return this;
        }

        public Builder departureTime(LocalDateTime v) {
            this.departureTime = v;
            return this;
        }

        public Builder arrivalTime(LocalDateTime v) {
            this.arrivalTime = v;
            return this;
        }

        public Builder baseFare(Money v) {
            this.baseFare = v;
            return this;
        }

        public Builder status(FlightStatus v) {
            this.status = v;
            return this;
        }

        public Flight build() {
            return new Flight(this);
        }
    }
}
