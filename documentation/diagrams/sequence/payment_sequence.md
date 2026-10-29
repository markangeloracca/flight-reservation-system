# Sequence Diagram 4 — Make a Payment (`PaymentController`)

| | |
|---|---|
| **Deliverable** | 7.1 D, sequence diagram 4 |
| **Use case** | UC-07 Make a payment ([Scenario 3](../usecase/use_case_scenarios.md#scenario-3--uc-07-make-a-payment)) |
| **Control class** | `PaymentController` |
| **Boundary class** | `PaymentDialog` |
| **External actor** | Payment Gateway (`SimulatedPaymentGateway` behind the `PaymentGateway` interface) |
| **Related diagrams** | [Payment activity](../activity/payment_activity.md), [Payment state machine](../state/payment_state_machine.md) |

## Diagram

```mermaid
sequenceDiagram
    autonumber
    actor U as Customer
    participant PD as «boundary»<br/>PaymentDialog
    participant CDt as «value object»<br/>CardDetails
    participant BC as «control»<br/>BookingController
    participant PC as «control»<br/>PaymentController
    participant AG as «control»<br/>AccessGuard
    participant RR as «data access»<br/>SqliteReservationRepository
    participant PR as «data access»<br/>SqlitePaymentRepository
    participant P as «entity»<br/>Payment
    participant GW as «gateway»<br/>SimulatedPaymentGateway
    participant R as «entity»<br/>Reservation
    participant EB as «control»<br/>EventBus

    U->>PD: choose card type, enter name, number, expiry, CVV, click Pay
    activate PD
    PD->>CDt: new CardDetails(method, name, number, expiry, cvv)
    activate CDt
    CDt->>CDt: luhnValid(number), expiry not past, CVV 3–4 digits
    CDt-->>PD: CardDetails (or ValidationException shown on the field)
    deactivate CDt
    PD->>PD: disable form, show "Processing…"

    PD->>BC: pay(reservationId, card)
    activate BC
    BC->>PC: payReservation(reservationId, card)
    activate PC
    PC->>RR: findById(reservationId)
    RR-->>PC: Reservation
    PC->>AG: requireReservationAccess(reservation)
    AG-->>PC: User
    PC->>PC: check status == PENDING_PAYMENT
    PC->>PR: findByReservation(reservationId)
    PR-->>PC: earlier payments
    PC->>PC: due = fare.total() − amountPaid()

    Note over PC,GW: charge(reservationId, card, due)
    PC->>P: initiate(reservationId, CHARGE, due, card, now)
    P-->>PC: Payment in INITIATED (brand + last 4 only)
    PC->>PR: insert(payment)
    PR-->>PC: Payment with id
    PC->>GW: authorize(card, due)
    activate GW
    GW-->>PC: GatewayResult
    deactivate GW

    alt declined (e.g. card ending 0002: insufficient funds)
        PC->>P: decline(reason)
        PC->>PR: update(payment)
        PC->>EB: publish(PAYMENTS)
        PC-->>BC: throw PaymentDeclinedException(reason)
        BC-->>PD: PaymentDeclinedException
        PD-->>U: "Payment declined: insufficient funds" (seat stays held)
    else approved
        PC->>P: authorize(reference "AUTH-…")
        PC->>GW: capture(reference, due)
        GW-->>PC: GatewayResult approved "TXN-…"
        PC->>P: complete()
        PC->>PR: update(payment)
        PC->>EB: publish(PAYMENTS)
        rect rgba(120, 144, 156, 0.12)
            Note over PC,R: Database.inTransaction
            PC->>RR: findById(reservationId)
            RR-->>PC: Reservation
            PC->>R: confirm(now)
            activate R
            R->>R: state.confirm(this): PendingPaymentState → ConfirmedState
            deactivate R
            PC->>RR: update(reservation)
        end
        PC->>EB: publish(RESERVATIONS)
        PC-->>BC: Payment in COMPLETED
        BC->>RR: findById(reservationId)
        RR-->>BC: Reservation in CONFIRMED
        BC-->>PD: Reservation
        PD-->>U: close dialog, open booking confirmation
    end
    deactivate PC
    deactivate BC
    deactivate PD
```

## Notes

- **Every attempt is recorded.** The payment row is inserted before the gateway is called and updated afterwards, so a
  decline still appears in payment history with its reason.
- **Payment state rules** live in `PaymentStatus.next()`: `INITIATED → AUTHORIZED | DECLINED`,
  `AUTHORIZED → COMPLETED | DECLINED`, `COMPLETED → REFUNDED`. Any other move throws. See the
  [payment state machine](../state/payment_state_machine.md).
- **Strategy / dependency inversion:** `PaymentController` depends on the `PaymentGateway` interface in the domain
  layer. `SimulatedPaymentGateway` in the data layer is one implementation; a real processor could replace it without
  changing the controller.
- **No card data at rest:** `CardDetails` lives only in memory for the call; `Payment` keeps brand, last 4 digits and
  cardholder name.
