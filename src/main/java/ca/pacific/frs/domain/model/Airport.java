package ca.pacific.frs.domain.model;

/** An airport identified by its IATA code. Reference data. */
public record Airport(String code, String name, String city, String country) {
    public Airport {
        code = Validate.required("code", "Airport code", code).toUpperCase();
    }

    /** e.g. {@code Calgary (YYC)}. */
    public String label() {
        return city + " (" + code + ")";
    }

    @Override
    public String toString() {
        return label();
    }
}
