# Flight Reservation System

ENSF 614 term project. A standalone desktop application for searching flights, booking seats and managing
reservations. One user at a time signs in as a **customer**, **flight agent** or **system administrator**.

Java 17 · Swing with FlatLaf and MigLayout · SQLite (embedded) · Maven

## Overview

The Flight Reservation System handles the full life of an airline booking in one desktop app: finding a
flight, choosing a seat, paying, getting a confirmation, and later changing or cancelling the trip. Behind
that, it lets airline staff keep the schedule, fleet and customer records up to date.

It runs entirely on the user's computer. There is no server and no internet connection needed; all data
lives in a single SQLite database file that the app creates and fills with demo data on first launch.

### Who uses it

- **Customers** search for flights, book seats on an interactive cabin map, pay by card (simulated),
  download confirmations, and manage their own trips, profile and payment history.
- **Flight agents** do the same on behalf of any customer: create customer profiles, make and change
  bookings, and view the flight schedule.
- **System administrators** run the airline's operations data: add, update, cancel and delete flights,
  manage routes and aircraft, and clear out old flights.

### A typical booking

1. Sign in, or register as a new customer.
2. Search by origin, destination, date and (optionally) airline. Results show times, prices and seats left.
3. Pick a flight and choose seats on the seat map. The seats are held for 15 minutes.
4. Pay with a card. A monthly promotion discount is applied if one is active.
5. Get a booking confirmation with a code, which can be saved as a text or HTML file.
6. Later, change the seat or flight (paying or refunding the fare difference) or cancel (full refund 24+
   hours before departure, 50% after that).

### What makes it reliable

- Business rules are enforced in the core of the app and the database, not just the screens. For example,
  a seat can never be sold twice, and customers can only ever see their own bookings.
- Multi-step changes (such as booking plus payment) either complete fully or not at all.
- Slow work runs in the background, so the window stays responsive.
- Passwords are hashed, and full card numbers are never stored.

### How it is built

The code follows a three-layer design (presentation, business, data) using well-known object-oriented
design patterns such as State, Strategy, Observer and Facade. The details are in [Architecture](#architecture)
below, and UML diagrams (use case, class, sequence, activity and state) are in
[`documentation/diagrams`](documentation/diagrams/README.md).

## Run it

```bash
mvn package                                   # builds and runs the tests
java -jar target/FlightReservationSystem.jar
```

The first launch creates the database and fills it with demo data: 11 airports, 4 airlines, 8 aircraft,
24 routes, and about 1,500 flights from one week ago to eight weeks ahead.

| Role | Username | Password |
|------|----------|----------|
| Customer | `customer` | `customer123` |
| Flight agent | `agent` | `agent123` |
| System administrator | `admin` | `admin123` |

The sign-in screen has one-click buttons for each demo account. New customers can also create their own account.

**Payments are simulated.** Use `4242 4242 4242 4242` with any future expiry and any 3-digit CVV.
Cards ending in `0002` are declined (insufficient funds) and cards ending in `0069` are declined (reported stolen).

**Data location:** `~/Library/Application Support/FlightReservationSystem/frs.db` (macOS),
`%APPDATA%\FlightReservationSystem\frs.db` (Windows), `~/.local/share/flight-reservation-system/frs.db` (Linux).
Delete the file to start over, or run with `-Dfrs.dataDir=/some/folder` to use another folder.

### Build the submission

```bash
scripts/package-submission.sh     # dist/FlightReservationSystem-submission.zip = runnable JAR + source.zip
scripts/package-native.sh         # optional: native installer with a bundled Java runtime (jpackage)
```

## What each role can do

| Requirement (brief §3) | Customer | Agent | Admin | Where |
|---|:-:|:-:|:-:|---|
| 3.1 Search flights by origin, destination, date, airline; see seats, times, prices | ✓ | ✓ | | Find a flight |
| 3.2 Create bookings (pick a seat on a cabin map) | ✓ | ✓ for any customer | | Find a flight → Select seats |
| 3.2 Cancel or modify reservations (seat or flight on same route) | own | any | | My trips / Reservations / Customers |
| 3.2 Generate booking confirmations (view, save as .txt or .html) | own | any | | Confirmation |
| 3.3 Create customer profiles; view and edit details | self-register, edit own | create, edit any | | My profile / Customers |
| 3.4 Add, update, cancel, delete flights; remove outdated flights | | | ✓ | Flights |
| Manage routes and aircraft | | | ✓ | Routes / Aircraft |
| View schedules | | ✓ | ✓ | Flight schedule |
| 3.5 Record payment information (simulated) and payment history | own | any | | Payment history / Customers |
| 3.6 Embedded database | SQLite | | | `data` layer |
| 3.7 Monthly promotion: picked at random on the first launch each month, shown once | ✓ | | | Pop-up after sign-in; discount applied at checkout |

Rules the system enforces:

- A seat can be held by only one live booking. A unique index in the database guarantees it, not only the UI.
- A seat is held for 15 minutes while the customer pays, then released automatically.
- Cancelling 24 hours or more before departure refunds in full; inside 24 hours refunds 50%.
- Changing to a more expensive seat or flight charges the difference; a cheaper one refunds it.
- Cancelling a flight cancels and fully refunds every booking on it.
- A flight with booking history cannot be deleted (cancel it instead); "Remove outdated" only deletes departed flights that were never booked.
- An aircraft or flight edit is rejected if a booked seat would no longer exist in the same cabin.
- Customers can only see and change their own bookings; every controller checks the signed-in role.

## Architecture

Three layers. Each depends only on the one below it, and the business layer owns the interfaces that the data
layer implements (dependency inversion), so the database could be swapped without touching business logic.

```
ca.ucalgary.frs
├── ui/            PRESENTATION (boundary classes): Swing pages, dialogs, components, theme
│   ├── shell/     main window, sign-in, navigation, page factory
│   ├── booking/   search, booking, seat map, payment, confirmation, my trips
│   ├── customer/  profile, customer management
│   ├── agent/     reservations, schedule
│   ├── admin/     flights, routes, aircraft
│   ├── components/ reusable widgets (Card, Toasts, BackgroundTask, ListTableModel, ...)
│   └── theme/     light/dark design tokens and type scale
├── control/       BUSINESS: control classes, one per major use case, plus Session and EventBus
├── domain/        BUSINESS: entities, value objects, policies, repository and gateway interfaces
│   ├── model/      User, Customer, Flight, Aircraft, Route, Reservation (+ states), Payment, ...
│   ├── policy/     FareCalculator, DiscountPolicy, RefundPolicy, ConfirmationCodeGenerator
│   ├── repository/ repository interfaces, TransactionManager
│   ├── gateway/    PaymentGateway, FileExporter
│   └── security/   PasswordHasher
├── data/          DATA: SQLite repositories, transactions, migrations, simulated payment gateway, file export
└── app/           composition root (wires everything by constructor injection), data paths, single-instance lock
```

### Use cases and their control classes

| Use case | Control class | Main boundary class |
|---|---|---|
| Log in / log out / register | `AuthController` | `LoginView`, `RegisterDialog` |
| Search flights, view schedule | `FlightSearchController` | `FlightSearchPage`, `SchedulePage` |
| Book a flight, cancel, modify, confirmation | `BookingController` | `BookingDialog`, `ModifyReservationDialog`, `ConfirmationDialog` |
| Make a payment | `PaymentController` | `PaymentDialog` |
| Manage customers / own profile | `CustomerController` | `CustomersPage`, `ProfilePage` |
| Manage flights | `FlightManagementController` | `FlightsPage`, `FlightEditorDialog` |
| Manage routes and aircraft | `RouteAndAircraftController` | `RoutesPage`, `AircraftPage` |
| Monthly promotion | `PromotionController` | `PromotionDialog` |

### Design patterns

| Pattern | Where |
|---|---|
| State | `Reservation` delegates confirm/cancel/modify/complete to `PendingPaymentState`, `ConfirmedState`, `CancelledState`, `CompletedState` |
| Strategy | `DiscountPolicy`, `RefundPolicy`, `ConfirmationRenderer` (text vs HTML), `PaymentGateway` |
| Observer | `Session` (sign-in changes), `EventBus` (data changes refresh open pages), `Theme` listeners |
| Facade | `BookingController` over reservations, payments, pricing and refunds |
| Factory Method | `UserFactory`, `ConnectionFactory`, `PageFactory` (navigation built from permissions) |
| Builder | `Flight.Builder`, `Customer.Builder`, `FlightSearchCriteria.Builder` |
| Template Method | `BackgroundTask` (fixed SwingWorker skeleton; callers supply the work and reactions) |
| Adapter | `ListTableModel` adapts domain lists to Swing's `TableModel` |
| Command | menu items and shortcuts are `Action` objects in `MainFrame` |
| Repository + Unit of Work | `*Repository` interfaces, `Database.inTransaction` binds one connection per thread |

Inheritance and polymorphism: `User` is abstract with `CustomerUser`, `FlightAgent` and `SystemAdministrator`;
permissions come from the role, so callers ask `user.can(permission)` rather than checking the subclass.

### Reliability and other non-functional choices

- Every database and payment call runs off the Swing event thread (`BackgroundTask`), so the UI never freezes.
- SQLite runs in WAL mode with foreign keys on and a busy timeout; multi-step changes run in one transaction.
- The schema is versioned (`PRAGMA user_version`) and migrated on start-up.
- On exit the app checkpoints the WAL file; a file lock prevents two copies from opening the same database.
- Passwords are salted PBKDF2 hashes. Full card numbers and CVVs are never stored, only brand and last 4 digits.
- Light and dark themes (Ctrl/⌘ + Shift + D), keyboard shortcuts (Ctrl/⌘ + 1…9 to switch pages, Ctrl/⌘ + F to search),
  accessible names on custom components, and empty, loading and error states on every list.

## Tests

```bash
mvn test
```

31 tests run against a real SQLite file, not mocks. They cover the domain rules (money, seat layout, fares,
reservation and payment state machines, refund policy, card validation) and the use cases end to end:
booking, declined payments, double-booking, hold expiry, cancellation refunds, modifications with fare
differences, access control, flight cancellation, outdated-flight removal, and the monthly promotion.
