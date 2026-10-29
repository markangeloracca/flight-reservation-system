package ca.ucalgary.frs.domain.model;

import ca.ucalgary.frs.domain.exception.ValidationException;

import java.util.Objects;

/** A directed origin → destination pair that flights operate on. */
public record Route(Long id, Airport origin, Airport destination, int distanceKm) {
    public Route {
        Validate.notNull("origin", "Origin", origin);
        Validate.notNull("destination", "Destination", destination);
        if (origin.code().equals(destination.code())) {
            throw new ValidationException("destination", "Origin and destination must be different airports.");
        }
        Validate.range("distanceKm", "Distance (km)", distanceKm, 50, 20_000);
    }

    public Route withId(long newId) {
        return new Route(newId, origin, destination, distanceKm);
    }

    public String label() {
        return origin.code() + " → " + destination.code();
    }

    public String longLabel() {
        return origin.city() + " → " + destination.city();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Route r && Objects.equals(id, r.id) && origin.equals(r.origin) && destination.equals(r.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, origin, destination);
    }

    @Override
    public String toString() {
        return label() + "  (" + longLabel() + ")";
    }
}
