package ca.pacific.frs.data.sqlite;

import ca.pacific.frs.data.db.Sql;
import ca.pacific.frs.domain.model.Aircraft;
import ca.pacific.frs.domain.model.Airline;
import ca.pacific.frs.domain.model.Airport;
import ca.pacific.frs.domain.model.Customer;
import ca.pacific.frs.domain.model.FareQuote;
import ca.pacific.frs.domain.model.Flight;
import ca.pacific.frs.domain.model.FlightAvailability;
import ca.pacific.frs.domain.model.FlightStatus;
import ca.pacific.frs.domain.model.Reservation;
import ca.pacific.frs.domain.model.ReservationStatus;
import ca.pacific.frs.domain.model.Route;
import ca.pacific.frs.domain.model.Seat;
import ca.pacific.frs.domain.model.SeatClass;

import java.sql.ResultSet;
import java.sql.SQLException;

/** Shared SELECT fragments and row mappers for joined flight and reservation queries. */
final class Mappers {
    private Mappers() {
    }

    static final String FLIGHT_COLUMNS = """
            f.id AS f_id, f.flight_number, f.departure_time, f.arrival_time, f.base_fare_cents, f.status AS f_status,
            al.code AS al_code, al.name AS al_name,
            r.id AS r_id, r.distance_km,
            o.code AS o_code, o.name AS o_name, o.city AS o_city, o.country AS o_country,
            d.code AS d_code, d.name AS d_name, d.city AS d_city, d.country AS d_country,
            ac.id AS ac_id, ac.registration, ac.model, ac.business_rows, ac.economy_rows, ac.seats_per_row
            """;

    static final String FLIGHT_JOINS = """
            JOIN airlines al ON al.code = f.airline_code
            JOIN routes r ON r.id = f.route_id
            JOIN airports o ON o.code = r.origin
            JOIN airports d ON d.code = r.destination
            JOIN aircraft ac ON ac.id = f.aircraft_id
            """;

    static final String AVAILABILITY_COLUMNS = """
            (SELECT COUNT(*) FROM reservations x WHERE x.flight_id = f.id
                AND x.status IN ('PENDING_PAYMENT', 'CONFIRMED') AND x.seat_class = 'ECONOMY') AS eco_taken,
            (SELECT COUNT(*) FROM reservations x WHERE x.flight_id = f.id
                AND x.status IN ('PENDING_PAYMENT', 'CONFIRMED') AND x.seat_class = 'BUSINESS') AS biz_taken
            """;

    static final String CUSTOMER_COLUMNS = """
            c.id AS c_id, c.first_name, c.last_name, c.email AS c_email, c.phone AS c_phone,
            c.date_of_birth, c.address, c.created_at AS c_created_at
            """;

    static Airport airport(ResultSet rs, String prefix) throws SQLException {
        return new Airport(rs.getString(prefix + "code"), rs.getString(prefix + "name"),
                rs.getString(prefix + "city"), rs.getString(prefix + "country"));
    }

    static Aircraft aircraft(ResultSet rs, String idColumn) throws SQLException {
        return new Aircraft(rs.getLong(idColumn), rs.getString("registration"), rs.getString("model"),
                rs.getInt("business_rows"), rs.getInt("economy_rows"), rs.getInt("seats_per_row"));
    }

    static Flight flight(ResultSet rs) throws SQLException {
        Route route = new Route(rs.getLong("r_id"), airport(rs, "o_"), airport(rs, "d_"), rs.getInt("distance_km"));
        return Flight.builder()
                .id(rs.getLong("f_id"))
                .flightNumber(rs.getString("flight_number"))
                .airline(new Airline(rs.getString("al_code"), rs.getString("al_name")))
                .route(route)
                .aircraft(aircraft(rs, "ac_id"))
                .departureTime(Sql.timestamp(rs, "departure_time"))
                .arrivalTime(Sql.timestamp(rs, "arrival_time"))
                .baseFare(Sql.money(rs, "base_fare_cents"))
                .status(FlightStatus.valueOf(rs.getString("f_status")))
                .build();
    }

    static FlightAvailability availability(ResultSet rs) throws SQLException {
        Flight flight = flight(rs);
        Aircraft ac = flight.aircraft();
        int eco = Math.max(0, ac.capacity(SeatClass.ECONOMY) - rs.getInt("eco_taken"));
        int biz = Math.max(0, ac.capacity(SeatClass.BUSINESS) - rs.getInt("biz_taken"));
        return new FlightAvailability(flight, eco, biz);
    }

    static Customer customer(ResultSet rs, String idColumn, String emailColumn, String phoneColumn,
                             String createdColumn) throws SQLException {
        return Customer.builder()
                .id(rs.getLong(idColumn))
                .firstName(rs.getString("first_name"))
                .lastName(rs.getString("last_name"))
                .email(rs.getString(emailColumn))
                .phone(rs.getString(phoneColumn))
                .dateOfBirth(Sql.date(rs, "date_of_birth"))
                .address(rs.getString("address"))
                .createdAt(Sql.timestamp(rs, createdColumn))
                .build();
    }

    static Reservation reservation(ResultSet rs) throws SQLException {
        Flight flight = flight(rs);
        SeatClass seatClass = SeatClass.valueOf(rs.getString("seat_class"));
        int[] seatParts = Seat.parse(rs.getString("seat_code"));
        Seat seat = new Seat(seatParts[0], (char) seatParts[1], seatClass);
        FareQuote fare = new FareQuote(seatClass, Sql.money(rs, "fare_cents"), Sql.money(rs, "discount_cents"),
                Sql.money(rs, "tax_cents"), Sql.money(rs, "total_cents"), rs.getString("promotion_title"));
        Customer customer = customer(rs, "c_id", "c_email", "c_phone", "c_created_at");
        return new Reservation(rs.getLong("res_id"), rs.getString("confirmation_code"), customer, flight, seat, fare,
                ReservationStatus.valueOf(rs.getString("res_status")),
                Sql.timestamp(rs, "res_created_at"), Sql.timestamp(rs, "res_updated_at"),
                Sql.nullableLong(rs, "booked_by_user_id"));
    }
}
