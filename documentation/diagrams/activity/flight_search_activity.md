# Activity Diagram 2 — Browsing for and Selecting a Flight

| | |
|---|---|
| **Deliverable** | 7.1 A, activity diagram 2 (browsing for and selecting a flight) |
| **Use cases** | UC-04 Search for flights, UC-05 View flight details and seat availability |
| **Control class** | `FlightSearchController` (`airports`, `airlines`, `search`, `seatMap`), `PromotionController` |
| **Boundary classes** | `FlightSearchPage`, `BookingDialog`, `SeatMapPanel` |
| **Related diagrams** | [Flight search sequence](../sequence/flight_search_sequence.md), [Booking activity](flight_booking_activity.md) |

## Description

A signed-in customer or flight agent opens **Find a flight**. The page loads the airports, airlines and this month's
promotion, then immediately lists every bookable flight. Each change to a filter (origin, destination, date or
"any date", airline) re-runs the search after a 200 ms pause, so typing and clicking never stutter.

Only *bookable* flights are listed: not cancelled and departing after now. Each row shows the flight number,
airline, route, departure and arrival times, duration, seats left per cabin and the lowest available fare. Choosing
**Select seats** on a flight that still has seats opens the booking dialog with that flight's live seat map.

## Diagram

```mermaid
flowchart TD
    start(( )):::terminal --> open("Open 'Find a flight'")
    open --> loadRef("Load airports, airlines and<br/>this month's promotion")
    loadRef --> hasPromo{"Promotion with a<br/>discount this month?"}
    hasPromo -->|yes| banner("Show promotion banner:<br/>discount applied at checkout")
    hasPromo -->|no| initial
    banner --> initial("Search with no filters")

    initial --> query
    filters("Choose origin, destination,<br/>date or 'any date', airline") --> debounce("Wait 200 ms for<br/>further changes")
    debounce --> query("Build search criteria:<br/>filters + departing after now<br/>+ bookable only")
    query --> fetch("Query flights with seats<br/>left per cabin and lowest fare")
    fetch --> any{"Any flights<br/>found?"}
    any -->|no| empty("Show empty state:<br/>'No flights match'")
    empty --> filters
    any -->|yes| list("Show results table:<br/>times, duration, seats left, fare")

    list --> act{"User action"}
    act -->|"Change a filter"| filters
    act -->|"Sort or scan results"| list
    act -->|"Select seats on a flight"| full{"Flight has any<br/>seat left?"}
    full -->|no| warnFull["Warn: 'This flight is full.<br/>Please choose another flight.'"]:::error
    warnFull --> list
    full -->|yes| openBook("Open booking dialog<br/>for the chosen flight")
    openBook --> seatMap("Load the seat map:<br/>aircraft layout + occupied seats")
    seatMap --> done(((" "))):::terminal

    classDef terminal fill:#111,stroke:#111,color:#111
    classDef error fill:#FDECEA,stroke:#C62828,color:#7F1D1D
```

The flow continues in [Activity Diagram 3 — Booking a Flight](flight_booking_activity.md).

## Notes

- **Filters are optional.** Any filter left at "Any" is ignored, so the first search lists every bookable flight.
- **Result limit:** at most 500 rows are shown; the count label says "Showing the first 500 flights" when capped.
- **Seat counts** come from the database: a seat is occupied when a reservation on that flight is
  `PENDING_PAYMENT` or `CONFIRMED`.
- **Agents** use the same page. Administrators can search but book nothing; they use the schedule views instead.
