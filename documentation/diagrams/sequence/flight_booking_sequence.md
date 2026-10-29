# Sequence Diagram 3 — Book a Flight (`BookingController`)

| | |
|---|---|
| **Deliverable** | 7.1 D, sequence diagram 3 |
| **Use case** | UC-06 Book a flight ([Scenario 2](../usecase/use_case_scenarios.md#scenario-2--uc-06-book-a-flight)) |
| **Control class** | `BookingController` (Facade over reservations, pricing, payments and refunds) |
| **Boundary classes** | `BookingDialog`, `PaymentDialog`, `ConfirmationDialog` |
| **Related diagrams** | [Booking activity](../activity/flight_booking_activity.md), [Reservation state machine](../state/reservation_state_machine.md), [Payment sequence](payment_sequence.md) |

## Diagram

```mermaid
sequenceDiagram
    autonumber
    actor U as Customer
    participant BD as «boundary»<br/>BookingDialog
    participant BC as «control»<br/>BookingController
    participant PC as «control»<br/>PromotionController
    participant FC as «policy»<br/>FareCalculator
    participant SS as «control»<br/>Session / AccessGuard
    participant CR as «data access»<br/>SqliteCustomerRepository
    participant FR as «data access»<br/>SqliteFlightRepository
    participant CG as «policy»<br/>ConfirmationCodeGenerator
    participant R as «entity»<br/>Reservation
    participant RR as «data access»<br/>SqliteReservationRepository
    participant EB as «control»<br/>EventBus
    participant PD as «boundary»<br/>PaymentDialog
    participant CD as «boundary»<br/>ConfirmationDialog

    U->>BD: pick seat 12C on the seat map
    activate BD
    BD->>BC: quote(flight, seatClass)
    activate BC
    BC->>PC: currentDiscountPolicy()
    PC-->>BC: DiscountPolicy (percentage or NONE)
    BC->>FC: quote(flight, seatClass, discountPolicy)
    FC->>FC: fare = flight.fareFor(seatClass)<br/>discount, tax 5%, total
    FC-->>BC: FareQuote
    BC-->>BD: FareQuote
    deactivate BC
    BD-->>U: fare breakdown

    U->>BD: Continue to payment
    BD->>BC: holdSeat(customerId, flightId, "12C")
    activate BC
    BC->>SS: require(BOOK_FLIGHTS), requireCustomerAccess(customerId)
    SS-->>BC: User

    rect rgba(120, 144, 156, 0.12)
        Note over BC,RR: Database.inTransaction: one SQLite transaction
        BC->>CR: findById(customerId)
        CR-->>BC: Customer
        BC->>FR: findById(flightId)
        FR-->>BC: Flight
        BC->>BC: flight.aircraft().requireSeat("12C")
        BC->>FR: occupiedSeats(flightId)
        FR-->>BC: set of seat codes
        alt seat 12C already occupied
            BC-->>BD: throw BusinessRuleException "Seat 12C is no longer available"
            BD-->>U: show error, reload seat map
        else seat free
            loop until the code is unused (at most 20 tries)
                BC->>CG: next()
                CG-->>BC: code e.g. "K7M2QX"
                BC->>RR: confirmationCodeExists(code)
                RR-->>BC: false
            end
            BC->>R: hold(code, customer, flight, seat, quote(...), now, userId)
            activate R
            R->>R: check flight.isBookable(now)
            R-->>BC: Reservation in PENDING_PAYMENT
            deactivate R
            BC->>RR: insert(reservation)
            Note right of RR: unique index on (flight, seat)<br/>for live reservations
            RR-->>BC: Reservation with id
        end
    end

    BC->>EB: publish(RESERVATIONS), publish(FLIGHTS)
    BC-->>BD: held Reservation
    deactivate BC

    BD->>PD: show(amount, "seat held for 15 minutes", submitter)
    activate PD
    U->>PD: enter card details, click Pay
    PD->>BC: pay(reservationId, card)
    activate BC
    Note over BC,PD: see Sequence Diagram 4 (PaymentController)
    BC-->>PD: Reservation in CONFIRMED
    deactivate BC
    PD-->>BD: Optional of Reservation
    deactivate PD

    alt paid
        BD->>CD: open(owner, ctx, reservationId)
        activate CD
        CD->>BC: confirmation(reservationId)
        BC-->>CD: BookingConfirmation
        CD-->>U: code, flight, seat, fare, payments
        deactivate CD
    else dialog closed without paying
        BD->>BC: releaseHold(reservationId)
        activate BC
        BC->>R: cancel(now)
        BC->>RR: update(reservation)
        BC->>EB: publish(RESERVATIONS), publish(FLIGHTS)
        deactivate BC
        BD-->>U: "Payment cancelled. Seat 12C was released."
    end
    deactivate BD
```

## Notes

- **Race safety:** if two holds for 12C commit at the same moment, the database's partial unique index
  `ux_reservations_live_seat` rejects the second insert. `holdSeat` turns the `DuplicateKeyException` into the same
  "seat was just taken" message as the occupied-seat check.
- **Facade:** the dialog talks only to `BookingController`; pricing, promotions, codes, persistence and payment are
  hidden behind it.
- **Strategy:** `DiscountPolicy` (none or percentage), `RefundPolicy` and `ConfirmationRenderer` (text or HTML) are
  interchangeable strategies injected into or chosen by the controller.
