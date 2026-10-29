package ca.pacific.frs.domain.model;

import ca.pacific.frs.domain.exception.ValidationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A customer profile. Customers may exist without a sign-in account (for example, a walk-in
 * customer created by a flight agent). Immutable; use {@link #toBuilder()} to make an edited copy.
 */
public final class Customer {
    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final LocalDate dateOfBirth;
    private final String address;
    private final LocalDateTime createdAt;

    private Customer(Builder b) {
        this.id = b.id;
        this.firstName = Validate.maxLength("firstName", "First name",
                Validate.required("firstName", "First name", b.firstName), 60);
        this.lastName = Validate.maxLength("lastName", "Last name",
                Validate.required("lastName", "Last name", b.lastName), 60);
        this.email = Validate.email("email", b.email);
        this.phone = Validate.optionalPhone("phone", b.phone);
        this.address = Validate.maxLength("address", "Address", Validate.blankToNull(b.address), 200);
        if (b.dateOfBirth != null && (b.dateOfBirth.isAfter(LocalDate.now())
                || b.dateOfBirth.isBefore(LocalDate.now().minusYears(120)))) {
            throw new ValidationException("dateOfBirth", "Enter a valid date of birth.");
        }
        this.dateOfBirth = b.dateOfBirth;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().id(id).firstName(firstName).lastName(lastName).email(email).phone(phone)
                .dateOfBirth(dateOfBirth).address(address).createdAt(createdAt);
    }

    public Long id() {
        return id;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public String email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public LocalDate dateOfBirth() {
        return dateOfBirth;
    }

    public String address() {
        return address;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Customer c && id != null && id.equals(c.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return fullName() + " <" + email + ">";
    }

    /** Builder pattern: collects fields, validates once in {@link #build()}. */
    public static final class Builder {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private LocalDate dateOfBirth;
        private String address;
        private LocalDateTime createdAt;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder firstName(String v) {
            this.firstName = v;
            return this;
        }

        public Builder lastName(String v) {
            this.lastName = v;
            return this;
        }

        public Builder email(String v) {
            this.email = v;
            return this;
        }

        public Builder phone(String v) {
            this.phone = v;
            return this;
        }

        public Builder dateOfBirth(LocalDate v) {
            this.dateOfBirth = v;
            return this;
        }

        public Builder address(String v) {
            this.address = v;
            return this;
        }

        public Builder createdAt(LocalDateTime v) {
            this.createdAt = v;
            return this;
        }

        public Customer build() {
            return new Customer(this);
        }
    }
}
