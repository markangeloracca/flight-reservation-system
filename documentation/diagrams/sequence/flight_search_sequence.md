# Sequence Diagram 2 — Search for Flights (`FlightSearchController`)

| | |
|---|---|
| **Deliverable** | 7.1 D, sequence diagram 2 |
| **Use cases** | UC-04 Search for flights, UC-05 View flight details and seat availability ([Scenario 1](../usecase/use_case_scenarios.md#scenario-1--uc-04-search-for-flights-and-select-one)) |
| **Control class** | `FlightSearchController` |
| **Boundary classes** | `FlightSearchPage`, `BookingDialog` |
| **Related diagrams** | [Flight search activity](../activity/flight_search_activity.md), [Flight state machine](../state/flight_state_machine.md) |

## Diagram

```mermaid
sequenceDiagram
    autonumber
    actor U as Customer
    participant FSP as «boundary»<br/>FlightSearchPage
    participant FSC as «control»<br/>FlightSearchController
    participant PC as «control»<br/>PromotionController
    participant SS as «control»<br/>Session
    participant FCR as «query object»<br/>FlightSearchCriteria
    participant RDR as «data access»<br/>SqliteReferenceDataRepository
    participant FR as «data access»<br/>SqliteFlightRepository
    participant DB as «database»<br/>SQLite
    participant BD as «boundary»<br/>BookingDialog
    participant SM as «entity»<br/>SeatMap

    U->>FSP: open "Find a flight"
    activate FSP
    FSP->>FSC: airports(), airlines()
    activate FSC
    FSC->>RDR: findAllAirports(), findAllAirlines()
    RDR->>DB: SELECT airports, SELECT airlines
    DB-->>RDR: rows
    RDR-->>FSC: airports and airlines
    FSC-->>FSP: airports and airlines
    deactivate FSC
    FSP->>PC: currentPromotion()
    PC-->>FSP: Optional of Promotion
    opt promotion with a discount
        FSP-->>U: banner "x% off every fare booked this month"
    end

    loop initial load, then each filter change (debounced 200 ms)
        U->>FSP: choose origin, destination, date, airline
        FSP->>FSC: search(originCode, destinationCode, date, airlineCode)
        activate FSC
        FSC->>SS: require(SEARCH_FLIGHTS)
        SS-->>FSC: User
        FSC->>FCR: builder().origin().destination().date().airline()<br/>.departingAfter(now).bookableOnly(true).build()
        FCR-->>FSC: criteria
        FSC->>FR: search(criteria)
        activate FR
        FR->>DB: SELECT flights JOIN routes, airports, airlines, aircraft<br/>with live-reservation counts per cabin, LIMIT 500
        DB-->>FR: rows
        FR->>FR: map each row to Flight and FlightAvailability
        FR-->>FSC: list of FlightAvailability
        deactivate FR
        FSC-->>FSP: list of FlightAvailability
        deactivate FSC
        alt no flights
            FSP-->>U: empty state "No flights match"
        else flights found
            FSP-->>U: table: times, duration, seats left, lowestFare()
        end
    end

    U->>FSP: Select seats on a flight
    alt totalAvailable() == 0
        FSP-->>U: warning "This flight is full"
    else seats available
        FSP->>BD: open(owner, ctx, flight)
        activate BD
        BD->>FSC: seatMap(flightId)
        activate FSC
        FSC->>SS: requireUser()
        FSC->>FR: findById(flightId)
        FR-->>FSC: Flight (with Route, Airline, Aircraft)
        FSC->>FR: occupiedSeats(flightId)
        FR->>DB: SELECT seat_code WHERE status IN (PENDING_PAYMENT, CONFIRMED)
        DB-->>FR: seat codes
        FR-->>FSC: set of occupied seat codes
        FSC->>SM: new SeatMap(flight, occupiedSeatCodes)
        FSC-->>BD: SeatMap
        deactivate FSC
        BD-->>U: cabin map, occupied seats disabled
        deactivate BD
    end
    deactivate FSP
```

## Notes

- **Builder:** `FlightSearchCriteria.Builder` keeps the call readable when most filters are optional (`null` = any).
- **Interface segregation:** `FlightSearchController` depends on the read-only `FlightQueryRepository`, not on
  `FlightRepository`, so search code cannot modify flights.
- **Adapter:** results reach the table through `ListTableModel`, which adapts a `List<FlightAvailability>` to Swing's
  `TableModel`.
