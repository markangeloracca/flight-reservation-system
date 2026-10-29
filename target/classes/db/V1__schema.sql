-- Flight Reservation System schema, version 1.
-- Money is stored as integer cents. Timestamps are ISO-8601 text: yyyy-MM-ddTHH:mm:ss.

CREATE TABLE airports (
    code      TEXT PRIMARY KEY CHECK (length(code) = 3),
    name      TEXT NOT NULL,
    city      TEXT NOT NULL,
    country   TEXT NOT NULL
);

CREATE TABLE airlines (
    code TEXT PRIMARY KEY CHECK (length(code) = 2),
    name TEXT NOT NULL
);

CREATE TABLE aircraft (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    registration   TEXT NOT NULL UNIQUE,
    model          TEXT NOT NULL,
    business_rows  INTEGER NOT NULL CHECK (business_rows >= 0),
    economy_rows   INTEGER NOT NULL CHECK (economy_rows >= 1),
    seats_per_row  INTEGER NOT NULL CHECK (seats_per_row IN (4, 6))
);

CREATE TABLE routes (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    origin       TEXT NOT NULL REFERENCES airports(code),
    destination  TEXT NOT NULL REFERENCES airports(code),
    distance_km  INTEGER NOT NULL CHECK (distance_km > 0),
    CHECK (origin <> destination),
    UNIQUE (origin, destination)
);

CREATE TABLE flights (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    flight_number    TEXT NOT NULL,
    airline_code     TEXT NOT NULL REFERENCES airlines(code),
    route_id         INTEGER NOT NULL REFERENCES routes(id),
    aircraft_id      INTEGER NOT NULL REFERENCES aircraft(id),
    departure_time   TEXT NOT NULL,
    arrival_time     TEXT NOT NULL,
    base_fare_cents  INTEGER NOT NULL CHECK (base_fare_cents > 0),
    status           TEXT NOT NULL CHECK (status IN ('SCHEDULED', 'DELAYED', 'CANCELLED')),
    CHECK (arrival_time > departure_time),
    UNIQUE (flight_number, departure_time)
);
CREATE INDEX idx_flights_route_departure ON flights(route_id, departure_time);
CREATE INDEX idx_flights_departure ON flights(departure_time);

CREATE TABLE customers (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name     TEXT NOT NULL,
    last_name      TEXT NOT NULL,
    email          TEXT NOT NULL UNIQUE COLLATE NOCASE,
    phone          TEXT,
    date_of_birth  TEXT,
    address        TEXT,
    created_at     TEXT NOT NULL
);

CREATE TABLE users (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    username       TEXT NOT NULL UNIQUE COLLATE NOCASE,
    password_hash  TEXT NOT NULL,
    role           TEXT NOT NULL CHECK (role IN ('CUSTOMER', 'AGENT', 'ADMIN')),
    full_name      TEXT NOT NULL,
    email          TEXT,
    customer_id    INTEGER UNIQUE REFERENCES customers(id),
    created_at     TEXT NOT NULL,
    CHECK ((role = 'CUSTOMER') = (customer_id IS NOT NULL))
);

CREATE TABLE reservations (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    confirmation_code   TEXT NOT NULL UNIQUE,
    customer_id         INTEGER NOT NULL REFERENCES customers(id),
    flight_id           INTEGER NOT NULL REFERENCES flights(id),
    seat_code           TEXT NOT NULL,
    seat_class          TEXT NOT NULL CHECK (seat_class IN ('ECONOMY', 'BUSINESS')),
    fare_cents          INTEGER NOT NULL,
    discount_cents      INTEGER NOT NULL DEFAULT 0,
    tax_cents           INTEGER NOT NULL,
    total_cents         INTEGER NOT NULL,
    promotion_title     TEXT,
    status              TEXT NOT NULL CHECK (status IN ('PENDING_PAYMENT', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    booked_by_user_id   INTEGER REFERENCES users(id),
    created_at          TEXT NOT NULL,
    updated_at          TEXT NOT NULL
);
CREATE INDEX idx_reservations_customer ON reservations(customer_id);
CREATE INDEX idx_reservations_flight ON reservations(flight_id);
-- A seat can be held by at most one live reservation. The database enforces this, not only the UI.
CREATE UNIQUE INDEX ux_reservations_live_seat
    ON reservations(flight_id, seat_code)
    WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED');

CREATE TABLE payments (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    reservation_id   INTEGER NOT NULL REFERENCES reservations(id),
    type             TEXT NOT NULL CHECK (type IN ('CHARGE', 'REFUND')),
    amount_cents     INTEGER NOT NULL CHECK (amount_cents > 0),
    method           TEXT NOT NULL CHECK (method IN ('CREDIT_CARD', 'DEBIT_CARD')),
    card_brand       TEXT,
    card_last4       TEXT,
    cardholder_name  TEXT,
    status           TEXT NOT NULL CHECK (status IN ('INITIATED', 'AUTHORIZED', 'DECLINED', 'COMPLETED', 'REFUNDED')),
    transaction_ref  TEXT,
    failure_reason   TEXT,
    created_at       TEXT NOT NULL
);
CREATE INDEX idx_payments_reservation ON payments(reservation_id);

CREATE TABLE promotions (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    title             TEXT NOT NULL,
    description       TEXT NOT NULL,
    discount_percent  INTEGER NOT NULL CHECK (discount_percent BETWEEN 0 AND 90),
    active            INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE promotion_views (
    customer_id  INTEGER NOT NULL REFERENCES customers(id),
    month        TEXT NOT NULL,
    PRIMARY KEY (customer_id, month)
);

CREATE TABLE app_settings (
    key    TEXT PRIMARY KEY,
    value  TEXT NOT NULL
);
