# State Machine 2 — Payment Process

| | |
|---|---|
| **Deliverable** | 7.1 E, state-transition diagram 2 (the payment process) |
| **Implemented by** | `Payment` (`authorize`, `complete`, `decline`, `markRefunded`), `PaymentStatus.next()` / `canMoveTo()`, `PaymentController`, `PaymentDialog` |
| **Related diagrams** | [Payment activity](../activity/payment_activity.md), [Payment sequence](../sequence/payment_sequence.md) |

## 2a. Payment process (what the user goes through)

```mermaid
stateDiagram-v2
    direction TB
    [*] --> EnteringDetails : PaymentDialog opens with the amount due

    EnteringDetails --> ValidatingCard : Pay clicked
    ValidatingCard --> EnteringDetails : [card invalid] / show field error
    ValidatingCard --> Processing : [card valid] / disable form, BackgroundTask

    state Processing {
        [*] --> CheckingReservation
        CheckingReservation --> Charging : [access allowed and PENDING_PAYMENT, amount due > 0]
        CheckingReservation --> Confirming : [nothing due]
        Charging --> Confirming : gateway approved and captured
        Confirming --> [*] : reservation CONFIRMED
    }

    Processing --> Declined : gateway declined / record attempt
    Processing --> Rejected : reservation not awaiting payment
    Declined --> EnteringDetails : show reason, seat stays held
    Processing --> Paid
    Paid --> [*] : close dialog, show confirmation
    Rejected --> [*] : show error
    EnteringDetails --> Abandoned : Cancel or close
    Abandoned --> [*] : caller releases hold (booking) or drops change (modify)
```

## 2b. Payment object lifecycle (`PaymentStatus`)

Every charge and every refund is a `Payment` row. Its status can only move along these transitions; any other move
throws `IllegalStateException`.

```mermaid
stateDiagram-v2
    direction LR
    [*] --> INITIATED : Payment.initiate(...) / initiateRefund(...)<br/>/ insert row (brand + last 4)
    INITIATED --> AUTHORIZED : authorize(ref) [gateway approved]<br/>/ store transaction reference
    INITIATED --> DECLINED : decline(reason) [authorization refused]
    AUTHORIZED --> COMPLETED : complete() [capture approved]
    AUTHORIZED --> DECLINED : decline(reason) [capture refused]
    COMPLETED --> REFUNDED : markRefunded() [CHARGE, and net amount paid is 0]
    DECLINED --> [*]
    COMPLETED --> [*]
    REFUNDED --> [*]

    note right of COMPLETED
        netEffect(): a completed or refunded CHARGE adds its amount,
        a completed REFUND subtracts it. amountPaid() sums netEffect().
    end note
```

| Status | Meaning |
|---|---|
| `INITIATED` | Recorded before calling the gateway, so even a crash leaves a trace |
| `AUTHORIZED` | Gateway approved the amount and returned a reference (`AUTH-…`, or `RFD-…` for refunds) |
| `COMPLETED` | Money captured (charge) or returned (refund) |
| `DECLINED` | Gateway refused; `failureReason` explains why. Final. |
| `REFUNDED` | A completed charge whose full amount has since been refunded. Final. |
