package ca.ucalgary.frs.domain.model;

/** An airline identified by its two-character IATA code. Reference data. */
public record Airline(String code, String name) {
    public Airline {
        code = Validate.required("code", "Airline code", code).toUpperCase();
    }

    @Override
    public String toString() {
        return name;
    }
}
