package ca.pacific.frs.data.migration;

import ca.pacific.frs.data.db.Sql;
import ca.pacific.frs.domain.model.Money;
import ca.pacific.frs.domain.security.PasswordHasher;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Version 2 migration: loads demo data so the application is usable on first launch.
 * Flights are generated from one week ago to eight weeks ahead of the first launch date.
 * All people in the data set are fictional.
 */
public final class DemoDataSeeder implements Migration {
    private static final int DAYS_BACK = 7;
    private static final int DAYS_AHEAD = 56;

    private final PasswordHasher hasher;
    private final Clock clock;

    public DemoDataSeeder(PasswordHasher hasher, Clock clock) {
        this.hasher = hasher;
        this.clock = clock;
    }

    @Override
    public int version() {
        return 2;
    }

    @Override
    public String description() {
        return "demo data";
    }

    @Override
    public void apply(Connection c) throws SQLException {
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        Random random = new Random(614);

        seedAirports(c);
        seedAirlines(c);
        List<Long> aircraft = seedAircraft(c);
        List<long[]> routes = seedRoutes(c);
        seedFlights(c, routes, aircraft, now.toLocalDate(), random);
        long demoCustomer = seedPeople(c, now);
        seedPromotions(c);
        seedDemoBookings(c, demoCustomer, now);
    }

    private static void seedAirports(Connection c) throws SQLException {
        String[][] airports = {
                {"YYC", "Calgary International", "Calgary", "Canada"},
                {"YVR", "Vancouver International", "Vancouver", "Canada"},
                {"YYZ", "Toronto Pearson International", "Toronto", "Canada"},
                {"YUL", "Montréal–Trudeau International", "Montréal", "Canada"},
                {"YEG", "Edmonton International", "Edmonton", "Canada"},
                {"YOW", "Ottawa Macdonald–Cartier International", "Ottawa", "Canada"},
                {"YWG", "Winnipeg James Armstrong Richardson International", "Winnipeg", "Canada"},
                {"YHZ", "Halifax Stanfield International", "Halifax", "Canada"},
                {"SEA", "Seattle–Tacoma International", "Seattle", "United States"},
                {"LAX", "Los Angeles International", "Los Angeles", "United States"},
                {"JFK", "John F. Kennedy International", "New York", "United States"},
        };
        for (String[] a : airports) {
            Sql.update(c, "INSERT INTO airports (code, name, city, country) VALUES (?, ?, ?, ?)", (Object[]) a);
        }
    }

    private static void seedAirlines(Connection c) throws SQLException {
        String[][] airlines = {
                {"AC", "Air Canada"}, {"WS", "WestJet"}, {"PD", "Porter Airlines"}, {"F8", "Flair Airlines"},
        };
        for (String[] a : airlines) {
            Sql.update(c, "INSERT INTO airlines (code, name) VALUES (?, ?)", (Object[]) a);
        }
    }

    private static List<Long> seedAircraft(Connection c) throws SQLException {
        Object[][] fleet = {
                {"C-FRSA", "Airbus A320", 3, 22, 6},
                {"C-FRSB", "Airbus A321", 4, 26, 6},
                {"C-GRSC", "Boeing 737-800", 4, 24, 6},
                {"C-GRSD", "Boeing 737 MAX 8", 3, 25, 6},
                {"C-GRSE", "Airbus A220-300", 3, 20, 4},
                {"C-FRSF", "Embraer E195-E2", 3, 25, 4},
                {"C-GRSG", "De Havilland Dash 8-400", 0, 19, 4},
                {"C-FRSH", "Boeing 787-9", 6, 30, 6},
        };
        List<Long> ids = new ArrayList<>();
        for (Object[] a : fleet) {
            ids.add(Sql.insert(c, """
                    INSERT INTO aircraft (registration, model, business_rows, economy_rows, seats_per_row)
                    VALUES (?, ?, ?, ?, ?)
                    """, a));
        }
        return ids;
    }

    /** Returns {routeId, distanceKm} for each route. Each city pair is seeded in both directions. */
    private static List<long[]> seedRoutes(Connection c) throws SQLException {
        Object[][] pairs = {
                {"YYC", "YVR", 686}, {"YYC", "YYZ", 2717}, {"YYC", "YEG", 248}, {"YYC", "YWG", 1190},
                {"YYC", "LAX", 1945}, {"YVR", "YYZ", 3343}, {"YVR", "SEA", 205}, {"YYZ", "YUL", 508},
                {"YYZ", "YOW", 352}, {"YYZ", "YHZ", 1286}, {"YYZ", "JFK", 589}, {"YEG", "YVR", 818},
        };
        List<long[]> routes = new ArrayList<>();
        for (Object[] p : pairs) {
            int km = (Integer) p[2];
            routes.add(new long[]{Sql.insert(c, "INSERT INTO routes (origin, destination, distance_km) VALUES (?, ?, ?)",
                    p[0], p[1], km), km});
            routes.add(new long[]{Sql.insert(c, "INSERT INTO routes (origin, destination, distance_km) VALUES (?, ?, ?)",
                    p[1], p[0], km), km});
        }
        return routes;
    }

    private static void seedFlights(Connection c, List<long[]> routes, List<Long> aircraft, LocalDate today,
                                    Random random) throws SQLException {
        String[] airlines = {"AC", "WS", "PD", "F8"};
        LocalTime[] slots = {LocalTime.of(6, 45), LocalTime.of(11, 30), LocalTime.of(17, 15), LocalTime.of(20, 40)};

        for (int r = 0; r < routes.size(); r++) {
            long routeId = routes.get(r)[0];
            int km = (int) routes.get(r)[1];
            int dailyFlights = km < 400 ? 4 : km < 1500 ? 3 : 2;
            long minutes = Math.round((km / 780.0) * 60 + 35);
            minutes = Math.max(45, (minutes / 5) * 5);
            double baseDollars = 69 + km * 0.105;

            for (int slot = 0; slot < dailyFlights; slot++) {
                String airline = airlines[(r + slot) % airlines.length];
                String number = airline + (100 + r * 10 + slot);
                long aircraftId = km < 400 ? aircraft.get(6 + (slot % 2 == 0 ? 0 : -2))
                        : km > 2500 ? aircraft.get(slot % 2 == 0 ? 7 : 1)
                        : aircraft.get(slot % 5);
                LocalTime time = slots[slot].plusMinutes((r * 7L) % 30);

                for (int day = -DAYS_BACK; day <= DAYS_AHEAD; day++) {
                    LocalDateTime dep = today.plusDays(day).atTime(time);
                    double dayFactor = 0.85 + random.nextDouble() * 0.4
                            + (dep.getDayOfWeek().getValue() >= 5 ? 0.12 : 0);
                    Money fare = Money.of(String.format("%.2f", Math.floor(baseDollars * dayFactor) + 0.99));
                    String status = random.nextInt(60) == 0 ? "DELAYED" : "SCHEDULED";
                    Sql.insert(c, """
                            INSERT INTO flights (flight_number, airline_code, route_id, aircraft_id, departure_time,
                                                 arrival_time, base_fare_cents, status)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """, number, airline, routeId, aircraftId, dep, dep.plusMinutes(minutes), fare, status);
                }
            }
        }
    }

    /** Creates the three demo accounts and a few walk-in customers. Returns the demo customer's id. */
    private long seedPeople(Connection c, LocalDateTime now) throws SQLException {
        long samId = insertCustomer(c, "Sam", "Taylor", "sam.taylor@example.com", "403-555-0142",
                LocalDate.of(1994, 5, 17), "100 Example St NW, Calgary, AB", now);
        insertCustomer(c, "Priya", "Raman", "priya.raman@example.com", "587-555-0199", LocalDate.of(1988, 11, 2),
                "42 Sample Ave, Edmonton, AB", now);
        insertCustomer(c, "Lucas", "Bergeron", "lucas.bergeron@example.com", "514-555-0107", LocalDate.of(1979, 3, 9),
                "7 Rue Fictive, Montréal, QC", now);
        insertCustomer(c, "Mei", "Chen", "mei.chen@example.com", "604-555-0163", LocalDate.of(2001, 8, 21),
                null, now);
        insertCustomer(c, "Omar", "Haddad", "omar.haddad@example.com", null, null, null, now);

        user(c, "customer", "customer123", "CUSTOMER", "Sam Taylor", "sam.taylor@example.com", samId, now);
        user(c, "agent", "agent123", "AGENT", "Jordan Reyes", "jordan.reyes@example.com", null, now);
        user(c, "admin", "admin123", "ADMIN", "Alex Morgan", "alex.morgan@example.com", null, now);
        return samId;
    }

    private static long insertCustomer(Connection c, String first, String last, String email, String phone,
                                       LocalDate dob, String address, LocalDateTime now) throws SQLException {
        return Sql.insert(c, """
                INSERT INTO customers (first_name, last_name, email, phone, date_of_birth, address, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, first, last, email, phone, dob, address, now);
    }

    private void user(Connection c, String username, String password, String role, String name, String email,
                      Long customerId, LocalDateTime now) throws SQLException {
        Sql.insert(c, """
                INSERT INTO users (username, password_hash, role, full_name, email, customer_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, username, hasher.hash(password.toCharArray()), role, name, email, customerId, now);
    }

    private static void seedPromotions(Connection c) throws SQLException {
        Object[][] promos = {
                {"Rocky Mountain Getaway", "Escape to the mountains: save on every fare booked this month.", 15},
                {"Coast to Coast", "From the Pacific to the Atlantic, explore Canada for less.", 10},
                {"Weekend Wanderer", "Make the most of your weekends with a discount on all flights.", 12},
                {"Early Bird Special", "Plan ahead and save on any flight booked this month.", 20},
                {"Sunshine Escape", "Trade the snow for sunshine with reduced fares.", 18},
                {"City Hopper", "Short hops, big savings. Discounted fares on every route.", 8},
        };
        for (Object[] p : promos) {
            Sql.insert(c, "INSERT INTO promotions (title, description, discount_percent, active) VALUES (?, ?, ?, 1)", p);
        }
    }

    /** Gives the demo customer a completed past trip and a confirmed upcoming trip, with payments. */
    private static void seedDemoBookings(Connection c, long customerId, LocalDateTime now) throws SQLException {
        long pastFlight = firstFlight(c, "YYC", "YVR", now.toLocalDate().minusDays(4));
        long nextFlight = firstFlight(c, "YYC", "YYZ", now.toLocalDate().plusDays(12));
        long userId = Sql.one(c, "SELECT id FROM users WHERE username = 'customer'", rs -> rs.getLong(1)).orElseThrow();

        booking(c, "FRS7QK", customerId, pastFlight, "8C", userId, now.minusDays(20), "CONFIRMED");
        booking(c, "M4TRVL", customerId, nextFlight, "12A", userId, now.minusDays(3), "CONFIRMED");
    }

    private static long firstFlight(Connection c, String origin, String destination, LocalDate date) throws SQLException {
        return Sql.one(c, """
                SELECT f.id FROM flights f JOIN routes r ON r.id = f.route_id
                WHERE r.origin = ? AND r.destination = ? AND substr(f.departure_time, 1, 10) = ?
                ORDER BY f.departure_time LIMIT 1
                """, rs -> rs.getLong(1), origin, destination, date).orElseThrow();
    }

    private static void booking(Connection c, String code, long customerId, long flightId, String seat, long userId,
                                LocalDateTime bookedAt, String status) throws SQLException {
        long fareCents = Sql.one(c, "SELECT base_fare_cents FROM flights WHERE id = ?", rs -> rs.getLong(1), flightId)
                .orElseThrow();
        Money fare = Money.ofCents(fareCents);
        Money tax = fare.percent(5);
        Money total = fare.plus(tax);
        long reservationId = Sql.insert(c, """
                INSERT INTO reservations (confirmation_code, customer_id, flight_id, seat_code, seat_class, fare_cents,
                    discount_cents, tax_cents, total_cents, promotion_title, status, booked_by_user_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, 'ECONOMY', ?, 0, ?, ?, NULL, ?, ?, ?, ?)
                """, code, customerId, flightId, seat, fare, tax, total, status, userId, bookedAt, bookedAt);
        Sql.insert(c, """
                INSERT INTO payments (reservation_id, type, amount_cents, method, card_brand, card_last4,
                    cardholder_name, status, transaction_ref, failure_reason, created_at)
                VALUES (?, 'CHARGE', ?, 'CREDIT_CARD', 'Visa', '4242', 'Sam Taylor', 'COMPLETED', ?, NULL, ?)
                """, reservationId, total, "TXN-DEMO" + code, bookedAt);
    }
}
