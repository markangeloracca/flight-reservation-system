# Use-Case Scenarios

| | |
|---|---|
| **Deliverable** | 7.1 C, detailed scenarios for use cases shown in the use-case diagram (three required; four provided) |
| **Related** | [Use-case diagram](use_case_diagram.md), [Class diagram](../class/domain_class_diagram.md), [Traceability matrix](../README.md#traceability-matrix) |

## Notation

| Mark | Meaning | Example |
|---|---|---|
| Single underline | Candidate **class** | <u>Flight</u> |
| Double underline (also bold) | Candidate **operation** | <b><u style="text-decoration: underline double;">search flights</u></b> |

Double underlines render in VS Code, browsers and the exported PDF. GitHub's Markdown view strips the style and shows a
single underline, so operations are also set in **bold** to stay distinguishable there.

Each scenario ends with a table mapping the marked words to the classes and methods that implement them.

---

## Scenario 1 — UC-04 Search for Flights (and select one)

**Primary actor:** Customer (also Flight Agent) · **Preconditions:** the user is signed in with a role that can search.
**Postcondition:** the user has a list of bookable flights and has opened the seat map of the chosen one.

### Main success scenario

1. The <u>Customer</u> opens the <u>Flight Search Page</u>. The system <b><u style="text-decoration: underline double;">loads the airports</u></b> and
   <b><u style="text-decoration: underline double;">loads the airlines</u></b> that fill the filter lists, and <b><u style="text-decoration: underline double;">gets the current promotion</u></b>. Because this month's
   <u>Promotion</u> has a discount, a banner tells the customer the discount will be applied at checkout.
2. The system <b><u style="text-decoration: underline double;">searches flights</u></b> with no filters and lists every bookable <u>Flight</u>.
3. The customer picks an origin <u>Airport</u> (Calgary, YYC), a destination <u>Airport</u> (Toronto, YYZ), a travel
   date, and an <u>Airline</u>.
4. After a short pause the <u>Flight Search Controller</u> <b><u style="text-decoration: underline double;">checks the permission</u></b> to search with the
   <u>Session</u>, then <b><u style="text-decoration: underline double;">builds the search criteria</u></b> as a <u>Flight Search Criteria</u> that also requires the
   flight to depart after now and be bookable (not cancelled).
5. The <u>Flight Repository</u> <b><u style="text-decoration: underline double;">finds matching flights</u></b> on that <u>Route</u> and <b><u style="text-decoration: underline double;">counts the free seats</u></b> in each
   cabin. It returns one <u>Flight Availability</u> per flight.
6. The page shows each flight's number, airline, departure and arrival times, duration, seats left in Economy and
   Business, and the <b><u style="text-decoration: underline double;">lowest available fare</u></b>.
7. The customer chooses **Select seats** on a flight that still has seats.
8. The controller <b><u style="text-decoration: underline double;">builds the seat map</u></b>: a <u>Seat Map</u> that combines the flight's <u>Aircraft</u> layout
   (<b><u style="text-decoration: underline double;">lists all seats</u></b> by row, letter and <u>Seat Class</u>) with the <b><u style="text-decoration: underline double;">occupied seats</u></b> held by live reservations.
9. The <u>Booking Dialog</u> opens and shows the cabin with occupied seats disabled. The use case ends; booking continues
   in Scenario 2.

### Alternative flows

- **4a. No flights match.** The page shows an empty state ("No flights match") and the customer changes a filter
  (back to step 3).
- **7a. The flight is full.** The page warns "This flight is full. Please choose another flight." and stays on the
  results (back to step 6).
- **4b. The session has ended or the role cannot search.** The controller raises an authorization error and the page
  shows it. Nothing is searched.

### Traceability

| Marked word | Class / method |
|---|---|
| Flight Search Page | `FlightSearchPage` «boundary» |
| Flight Search Controller | `FlightSearchController` «control» |
| loads the airports / airlines | `FlightSearchController.airports()`, `.airlines()` |
| gets the current promotion | `PromotionController.currentPromotion()` |
| searches flights | `FlightSearchController.search(origin, destination, date, airline)` |
| checks the permission | `Session.require(Permission.SEARCH_FLIGHTS)` |
| builds the search criteria | `FlightSearchCriteria.builder() … build()` |
| finds matching flights / counts the free seats | `FlightQueryRepository.search(criteria)` |
| lowest available fare | `FlightAvailability.lowestFare()` |
| builds the seat map | `FlightSearchController.seatMap(flightId)` |
| lists all seats | `Aircraft.allSeats()` |
| occupied seats | `FlightQueryRepository.occupiedSeats(flightId)` |
| Customer, Promotion, Flight, Airport, Airline, Route, Aircraft, Seat Class, Seat Map, Flight Availability, Session | entity / control classes of the same name |

---

## Scenario 2 — UC-06 Book a Flight

**Primary actor:** Customer (or Flight Agent on behalf of a customer) · **Includes:** UC-05, UC-07.
**Preconditions:** Scenario 1 has opened the booking dialog for a bookable flight.
**Postcondition:** a confirmed <u>Reservation</u> exists for the chosen seat, the payment is recorded, and the seat is
no longer available to anyone else.

### Main success scenario

1. The customer picks seat 12C on the seat map. The <u>Seat Map Panel</u> reports the chosen <u>Seat</u>.
2. The <u>Booking Controller</u> <b><u style="text-decoration: underline double;">quotes the fare</u></b>: it asks the <u>Promotion Controller</u> for the
   <b><u style="text-decoration: underline double;">current discount policy</u></b>, and the <u>Fare Calculator</u> <b><u style="text-decoration: underline double;">calculates the fare</u></b> from the flight's base fare
   times the seat's <u>Seat Class</u> multiplier, minus the <u>Discount Policy</u> discount, plus 5% tax. The result is
   a <u>Fare Quote</u>, shown as a fare breakdown.
3. The customer selects **Continue to payment**.
4. The <u>Booking Controller</u> <b><u style="text-decoration: underline double;">holds the seat</u></b>. It <b><u style="text-decoration: underline double;">checks the permission</u></b> to book and
   <b><u style="text-decoration: underline double;">checks access</u></b> to the <u>Customer</u> record through the <u>Access Guard</u>.
5. Inside one database transaction it <b><u style="text-decoration: underline double;">finds the customer</u></b>, <b><u style="text-decoration: underline double;">finds the flight</u></b>, and asks the
   <u>Aircraft</u> to <b><u style="text-decoration: underline double;">validate the seat code</u></b>. The seat is not in the flight's occupied seats.
6. The <u>Confirmation Code Generator</u> <b><u style="text-decoration: underline double;">generates a confirmation code</u></b> that no other reservation uses.
7. A new <u>Reservation</u> is <b><u style="text-decoration: underline double;">created as a hold</u></b> in the `PENDING_PAYMENT` state. The reservation first
   <b><u style="text-decoration: underline double;">checks that the flight is bookable</u></b> (not cancelled, not departed).
8. The <u>Reservation Repository</u> <b><u style="text-decoration: underline double;">inserts the reservation</u></b>. The transaction commits.
9. The controller <b><u style="text-decoration: underline double;">publishes a change event</u></b> on the <u>Event Bus</u>, so open pages refresh.
10. The <u>Payment Dialog</u> opens with the amount due and the 15-minute hold notice. The customer
    <b><u style="text-decoration: underline double;">makes a payment</u></b> (Scenario 3), which <b><u style="text-decoration: underline double;">confirms the reservation</u></b>.
11. The <u>Confirmation Dialog</u> shows the <u>Booking Confirmation</u>: the confirmation code, flight, seat, fare
    breakdown and payments. The customer may <b><u style="text-decoration: underline double;">export the confirmation</u></b> as text or HTML.

### Alternative flows

- **1a. Agent booking.** The agent first chooses the <u>Customer</u> from a list. Steps continue with that customer.
- **5a. Seat already taken.** The seat appears in the occupied seats. The controller rejects the hold: "Seat 12C is no
  longer available. Please choose another seat." The dialog reloads the seat map (back to step 1).
- **8a. Another booking took the seat in the same instant.** The database's unique index on live seats rejects the
  insert. The result is the same as 5a.
- **7a. Flight no longer bookable.** It was cancelled or has departed since the search. The hold is refused with
  "Flight … can no longer be booked."
- **10a. Customer closes the payment dialog.** The controller <b><u style="text-decoration: underline double;">releases the hold</u></b>: the reservation is cancelled and
  the seat freed. The dialog shows "Payment cancelled. Seat 12C was released."
- **10b. Customer never returns.** Housekeeping cancels any hold older than 15 minutes.

### Traceability

| Marked word | Class / method |
|---|---|
| Booking Dialog, Seat Map Panel, Payment Dialog, Confirmation Dialog | `BookingDialog`, `SeatMapPanel`, `PaymentDialog`, `ConfirmationDialog` «boundary» |
| Booking Controller | `BookingController` «control» (Facade) |
| quotes the fare | `BookingController.quote(flight, seatClass)` |
| current discount policy | `PromotionController.currentDiscountPolicy()` |
| calculates the fare | `FareCalculator.quote(flight, seatClass, discountPolicy)` |
| holds the seat | `BookingController.holdSeat(customerId, flightId, seatCode)` |
| checks the permission / checks access | `Session.require(BOOK_FLIGHTS)`, `AccessGuard.requireCustomerAccess(customerId)` |
| finds the customer / finds the flight | `CustomerRepository.findById`, `FlightQueryRepository.findById` |
| validate the seat code | `Aircraft.requireSeat(code)` |
| generates a confirmation code | `ConfirmationCodeGenerator.next()` |
| created as a hold / checks that the flight is bookable | `Reservation.hold(…)`, `Flight.isBookable(now)` |
| inserts the reservation | `ReservationRepository.insert(reservation)` |
| publishes a change event | `EventBus.publish(Topic.RESERVATIONS)` |
| makes a payment / confirms the reservation | `BookingController.pay(id, card)` → `PaymentController.payReservation`, `Reservation.confirm(now)` |
| releases the hold | `BookingController.releaseHold(reservationId)` |
| export the confirmation | `BookingController.exportConfirmation(id, path, renderer)` with `PlainTextConfirmationRenderer` or `HtmlConfirmationRenderer` |

---

## Scenario 3 — UC-07 Make a Payment

**Primary actor:** Customer (or Flight Agent) · **Secondary actor:** Payment Gateway (simulated).
**Preconditions:** a <u>Reservation</u> is in `PENDING_PAYMENT` (Scenario 2, step 10).
**Postcondition:** a completed <u>Payment</u> is recorded and the reservation is `CONFIRMED`; or a declined payment is
recorded and the seat stays held for a retry.

### Main success scenario

1. The <u>Payment Dialog</u> shows the amount due, $512.40, and the name on the booking.
2. The customer chooses **Credit card** (<u>Payment Method Type</u>) and enters the name, card number, expiry and CVV.
3. The dialog creates <u>Card Details</u>, which <b><u style="text-decoration: underline double;">validates the card</u></b>: the number passes the Luhn check, the expiry
   is not past, and the CVV has 3 or 4 digits. It <b><u style="text-decoration: underline double;">detects the card brand</u></b> (Visa).
4. The customer selects **Pay**. The <u>Booking Controller</u> <b><u style="text-decoration: underline double;">pays the reservation</u></b> by handing it to the
   <u>Payment Controller</u>.
5. The <u>Payment Controller</u> <b><u style="text-decoration: underline double;">loads the reservation</u></b> and <b><u style="text-decoration: underline double;">checks access</u></b> through the <u>Access Guard</u>.
   The reservation is awaiting payment.
6. It <b><u style="text-decoration: underline double;">calculates the amount due</u></b>: the <u>Fare Quote</u> total minus the <b><u style="text-decoration: underline double;">amount already paid</u></b>.
7. It <b><u style="text-decoration: underline double;">charges the card</u></b>. A new <u>Payment</u> is <b><u style="text-decoration: underline double;">initiated</u></b> as a `CHARGE` in `INITIATED` status, storing
   only the brand and last four digits, and the <u>Payment Repository</u> <b><u style="text-decoration: underline double;">records the payment</u></b>.
8. The <u>Payment Gateway</u> <b><u style="text-decoration: underline double;">authorizes the amount</u></b> and returns a reference. The payment is
   <b><u style="text-decoration: underline double;">authorized</u></b>.
9. The <u>Payment Gateway</u> <b><u style="text-decoration: underline double;">captures the amount</u></b>. The payment is <b><u style="text-decoration: underline double;">completed</u></b> and the repository
   <b><u style="text-decoration: underline double;">updates the payment</u></b>.
10. In one transaction the <u>Reservation</u> is <b><u style="text-decoration: underline double;">confirmed</u></b>: its <u>Reservation State</u> moves from pending
    payment to confirmed, and the <u>Reservation Repository</u> <b><u style="text-decoration: underline double;">updates the reservation</u></b>.
11. The controller <b><u style="text-decoration: underline double;">publishes a change event</u></b> on the <u>Event Bus</u>. The dialog closes and the booking
    confirmation opens.

### Alternative flows

- **3a. Invalid card details.** The dialog shows the error next to the field ("Enter a valid card number.") and nothing
  is sent (back to step 2).
- **5a. Reservation no longer awaiting payment** (released by housekeeping, or already paid). The controller refuses:
  "This reservation is not awaiting payment."
- **8a. Card declined** (for example "insufficient funds"). The payment is <b><u style="text-decoration: underline double;">declined</u></b> with the reason and the
  attempt is still recorded. The dialog shows the reason, the seat stays held, and the customer may retry with another
  card (back to step 2).
- **6a. Nothing due** (already fully paid). No charge is made; the reservation is confirmed directly (step 10).

### Traceability

| Marked word | Class / method |
|---|---|
| Payment Dialog | `PaymentDialog` «boundary» |
| Payment Controller | `PaymentController` «control» |
| Card Details, validates the card, detects the card brand | `CardDetails` record constructor, `CardDetails.luhnValid()`, `CardDetails.brand()` |
| Payment Method Type | `PaymentMethodType` (`CREDIT_CARD`, `DEBIT_CARD`) |
| pays the reservation | `BookingController.pay(id, card)` → `PaymentController.payReservation(id, card)` |
| loads the reservation / checks access | `ReservationRepository.findById`, `AccessGuard.requireReservationAccess` |
| calculates the amount due / amount already paid | `FareQuote.total()`, `PaymentController.amountPaid(id)` |
| charges the card | `PaymentController.charge(id, card, amount)` |
| initiated / authorized / completed / declined | `Payment.initiate(…)`, `Payment.authorize(ref)`, `Payment.complete()`, `Payment.decline(reason)` |
| records the payment / updates the payment | `PaymentRepository.insert`, `PaymentRepository.update` |
| Payment Gateway, authorizes / captures the amount | `PaymentGateway.authorize(card, amount)`, `.capture(ref, amount)`, implemented by `SimulatedPaymentGateway` |
| confirmed, Reservation State | `Reservation.confirm(now)` → `PendingPaymentState.confirm` → `ConfirmedState` |
| updates the reservation | `ReservationRepository.update(reservation)` |

---

## Scenario 4 — UC-01 Log In

**Primary actor:** User (Customer, Flight Agent or System Administrator).
**Preconditions:** the application is running and shows the sign-in screen.
**Postcondition:** the user is signed in and sees only the pages their role allows.

### Main success scenario

1. The <u>User</u> enters a username and password on the <u>Login View</u> and selects **Sign in**.
2. The <u>Auth Controller</u> <b><u style="text-decoration: underline double;">logs in</u></b> the user. It checks both fields are filled in.
3. The <u>User Repository</u> <b><u style="text-decoration: underline double;">finds the stored credentials</u></b> for the username: the <u>User</u> and its password
   hash.
4. The <u>Password Hasher</u> <b><u style="text-decoration: underline double;">verifies the password</u></b> against the salted hash. It matches.
5. The controller <b><u style="text-decoration: underline double;">wipes the password</u></b> from memory and the <u>Session</u> <b><u style="text-decoration: underline double;">signs in</u></b> the user.
6. The <u>Session</u> <b><u style="text-decoration: underline double;">notifies its listeners</u></b>. The <u>Main Frame</u> receives the change and
   <b><u style="text-decoration: underline double;">builds the pages</u></b> for the user's <u>Role</u> through the <u>Page Factory</u>, keeping only pages whose
   <u>Permission</u> the role grants.
7. The workspace opens with the message "Signed in as …".
8. For a customer, the <u>Promotion Controller</u> <b><u style="text-decoration: underline double;">finds an unseen promotion</u></b> for this month. The
   <u>Promotion Dialog</u> shows it once and the controller <b><u style="text-decoration: underline double;">marks it as seen</u></b>.

### Alternative flows

- **2a. A field is blank.** "Enter your username and password." (back to step 1).
- **3a. Unknown username.** The hasher still verifies the password against a dummy hash so the response takes the same
  time, then the controller reports "Invalid username or password" (back to step 1).
- **4a. Wrong password.** Same message as 3a. The password field is cleared.
- **8a. No promotion this month, or already seen.** Nothing is shown.

### Traceability

| Marked word | Class / method |
|---|---|
| Login View, Main Frame, Promotion Dialog | `LoginView`, `MainFrame`, `PromotionDialog` «boundary» |
| Auth Controller, logs in | `AuthController.login(username, password)` |
| finds the stored credentials | `UserRepository.findCredentials(username)` |
| verifies the password / wipes the password | `PasswordHasher.verify(password, hash)`, `PasswordHasher.wipe(password)` |
| Session, signs in, notifies its listeners | `Session.signIn(user)` firing `PROPERTY_USER` (Observer) |
| builds the pages, Page Factory | `PageFactory.pagesFor(user, ctx)` |
| User, Role, Permission | `User` (abstract) with `CustomerUser`, `FlightAgent`, `SystemAdministrator`; `Role`; `Permission` |
| finds an unseen promotion / marks it as seen | `PromotionController.unseenPromotionForCurrentCustomer()`, `.markSeenByCurrentCustomer()` |
