# State Machine 3 — Reservation Object

| | |
|---|---|
| **Deliverable** | 7.1 E, state-transition diagram 3 (a reservation object) |
| **Implemented by** | State pattern: `Reservation` delegates to `ReservationState`, implemented by `PendingPaymentState`, `ConfirmedState`, `CancelledState`, `CompletedState` (one shared instance each). `ReservationStatus` is the persisted value. |
| **Triggered by** | `BookingController` (`holdSeat`, `releaseHold`, `cancel`, `modify`, `runHousekeeping`), `PaymentController.payReservation`, `FlightManagementController.cancelFlight` |
| **Related diagrams** | [Booking activity](../activity/flight_booking_activity.md), [Booking sequence](../sequence/flight_booking_sequence.md) |

## Diagram

```mermaid
stateDiagram-v2
    direction TB
    [*] --> PENDING_PAYMENT : hold(...) [flight bookable and seat free]<br/>/ insert, seat held

    PENDING_PAYMENT --> PENDING_PAYMENT : card declined
    PENDING_PAYMENT --> CONFIRMED : confirm(now) [payment completed]
    PENDING_PAYMENT --> CANCELLED : cancel(now) [payment dialog closed (releaseHold)<br/>or hold older than 15 min (housekeeping)<br/>or flight cancelled] / free seat

    CONFIRMED --> CONFIRMED : modify(flight, seat, fare) [same route, new flight bookable,<br/>not departed] / charge or refund the difference
    CONFIRMED --> CANCELLED : cancel(now) [flight not departed]<br/>/ free seat, refund 100% (24 h+ before) or 50%
    CONFIRMED --> CANCELLED : flight cancelled by admin / full refund
    CONFIRMED --> COMPLETED : complete(now) [flight has departed] (housekeeping)

    CANCELLED --> [*]
    COMPLETED --> [*]

    note right of PENDING_PAYMENT
        holdsSeat() = true in PENDING_PAYMENT and CONFIRMED.
        A partial unique index allows one such
        reservation per (flight, seat).
    end note
```

## Allowed operations per state

Calling an operation that the state does not allow throws `BusinessRuleException`
("A reservation that is cancelled cannot be changed.").

| State | `confirm` | `cancel` | `modify` | `complete` | Holds seat |
|---|:-:|:-:|:-:|:-:|:-:|
| `PENDING_PAYMENT` | ✓ → CONFIRMED | ✓ → CANCELLED | ✗ "Complete payment before changing" | ✗ | ✓ |
| `CONFIRMED` | ✗ | ✓ → CANCELLED (not after departure) | ✓ stays CONFIRMED | ✓ → COMPLETED | ✓ |
| `CANCELLED` | ✗ | ✗ | ✗ | ✗ | ✗ |
| `COMPLETED` | ✗ | ✗ | ✗ | ✗ | ✗ |
