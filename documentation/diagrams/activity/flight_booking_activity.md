# Activity Diagram 3 — Booking a Flight

| | |
|---|---|
| **Deliverable** | 7.1 A, activity diagram 3 (booking a flight) |
| **Use cases** | UC-06 Book a flight (includes UC-07 Make a payment) |
| **Control class** | `BookingController` (`quote`, `holdSeat`, `pay`, `releaseHold`, `confirmation`) |
| **Boundary classes** | `BookingDialog`, `SeatMapPanel`, `FareBreakdown`, `PaymentDialog`, `ConfirmationDialog` |
| **Related diagrams** | [Booking sequence](../sequence/flight_booking_sequence.md), [Reservation state machine](../state/reservation_state_machine.md) |

## Description

Booking happens in two steps so that two people can never pay for the same seat:

1. **Hold the seat.** A `Reservation` is created in `PENDING_PAYMENT` state. A unique database index on
   *(flight, seat)* for live reservations guarantees only one hold can exist per seat, even if two bookings race.
2. **Pay.** The payment dialog opens (see [Activity Diagram 4](payment_activity.md)). Payment confirms the
   reservation. Closing the dialog without paying releases the hold at once; an abandoned hold is released
   automatically after 15 minutes.

A flight agent books on behalf of a customer and picks the customer first. A customer always books for themselves.

## Diagram

```mermaid
flowchart TD
    start(( )):::terminal --> seatMap("Show seat map: business and economy<br/>cabins, occupied seats disabled")
    seatMap --> role{"Signed-in user<br/>is an agent?"}
    role -->|yes| pickCust("Choose the customer<br/>to book for")
    role -->|no| self("Passenger is the<br/>signed-in customer")
    pickCust --> pickSeat
    self --> pickSeat("Pick a free seat")

    pickSeat --> quote("Quote the fare: base fare × cabin multiplier,<br/>minus promotion discount, plus 5% tax")
    quote --> showFare("Show fare breakdown")
    showFare --> next{"User action"}
    next -->|"Pick another seat"| pickSeat
    next -->|"Cancel"| done
    next -->|"Continue to payment"| perm{"Allowed to book<br/>for this customer?"}
    perm -->|no| errPerm["Show 'You can only access<br/>your own account.'"]:::error
    errPerm --> done

    perm -->|yes| tx("Begin transaction:<br/>load customer, flight and seat")
    tx --> free{"Seat still free and<br/>flight still bookable?"}
    free -->|no| errSeat["Show 'Seat … is no longer available'<br/>and reload the seat map"]:::error
    errSeat --> pickSeat
    free -->|yes| code("Generate a unique<br/>6-character confirmation code")
    code --> insert("Insert reservation as<br/>PENDING_PAYMENT (seat held)")
    insert --> race{"Unique seat index<br/>violated by a<br/>concurrent hold?"}
    race -->|yes| errSeat
    race -->|no| commit("Commit and notify open pages<br/>that flights and reservations changed")

    commit --> pay[["Make a payment<br/>(Activity Diagram 4)"]]
    pay --> paid{"Payment<br/>completed?"}
    paid -->|yes| confirmed("Reservation is CONFIRMED")
    confirmed --> showConf("Show booking confirmation:<br/>view, copy code, save as .txt or .html")
    showConf --> done
    paid -->|"no, dialog closed"| release("Release the hold: cancel the<br/>reservation and free the seat")
    release --> infoRel("Show 'Payment cancelled.<br/>Seat … was released.'")
    infoRel --> seatMap

    done(((" "))):::terminal

    classDef terminal fill:#111,stroke:#111,color:#111
    classDef error fill:#FDECEA,stroke:#C62828,color:#7F1D1D
```

## Notes

- **Fare rules (`FareCalculator`):** Business costs 2.5 × the base fare, Economy 1.0 ×. The monthly promotion
  (`PercentageDiscountPolicy`) takes its percentage off the fare before tax. Tax is 5%. Money is held as integer cents.
- **Hold expiry:** `BookingController.runHousekeeping()` runs at start-up and every 60 s. It cancels any hold older
  than 15 minutes (`HOLD_EXPIRY`) and marks confirmed trips whose flight has departed as `COMPLETED`.
- **Events:** after a commit the controller publishes `RESERVATIONS` and `FLIGHTS` on the `EventBus`, so open
  tables (search results, My trips, agent reservations) reload without a manual refresh.
