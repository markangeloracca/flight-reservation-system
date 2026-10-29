package ca.pacific.frs.domain.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Search filters for flights. Any null field means "any". Built with {@link #builder()}.
 */
public record FlightSearchCriteria(String originCode, String destinationCode, LocalDate dateFrom,
                                   LocalDate dateTo, String airlineCode,
                                   LocalDateTime departingAfter, boolean bookableOnly) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String originCode;
        private String destinationCode;
        private LocalDate dateFrom;
        private LocalDate dateTo;
        private String airlineCode;
        private LocalDateTime departingAfter;
        private boolean bookableOnly = true;

        public Builder origin(String code) {
            this.originCode = code;
            return this;
        }

        public Builder destination(String code) {
            this.destinationCode = code;
            return this;
        }

        /** Departure dates, inclusive; either end may be null for open-ended. */
        public Builder dates(LocalDate from, LocalDate to) {
            this.dateFrom = from;
            this.dateTo = to;
            return this;
        }

        public Builder airline(String code) {
            this.airlineCode = code;
            return this;
        }

        public Builder departingAfter(LocalDateTime t) {
            this.departingAfter = t;
            return this;
        }

        public Builder bookableOnly(boolean v) {
            this.bookableOnly = v;
            return this;
        }

        public FlightSearchCriteria build() {
            return new FlightSearchCriteria(originCode, destinationCode, dateFrom, dateTo, airlineCode, departingAfter, bookableOnly);
        }
    }
}
