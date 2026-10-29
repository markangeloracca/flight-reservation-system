# System Use-Case Diagram

| | |
|---|---|
| **Deliverable** | 7.1 B, system use-case diagram |
| **Actors** | Customer, Flight Agent, System Administrator (primary); Payment Gateway, System Timer (secondary) |
| **Related** | [Use-case scenarios](use_case_scenarios.md), [Traceability matrix](../README.md#traceability-matrix) |

## Actors

| Actor | Kind | Description |
|---|---|---|
| **User** | Abstract primary | Anyone with an account. Generalizes the three roles below and owns *Log in* and *Log out*. |
| **Customer** | Primary | End user who books flights for themselves. Can self-register. |
| **Flight Agent** | Primary | Employee who manages customers and any customer's bookings, and views schedules. |
| **System Administrator** | Primary | Maintains flights, routes, aircraft and schedules. Books nothing. |
| **Payment Gateway** | Secondary (external) | Authorizes, captures and refunds card payments. Simulated (requirement 3.5). |
| **System Timer** | Secondary | Fires at start-up and every 60 seconds to run housekeeping. |

## Diagram

Mermaid has no native use-case notation, so the diagram uses a flowchart: actors are person nodes, use cases are
ovals inside the system boundary (grouped by area), and `«include»` / `«extend»` are dashed arrows. The three roles
specialize the abstract **User** actor, shown in the [actor hierarchy](#actor-hierarchy) below; *Log in*, *Log out*
and *Search for flights* belong to every User.

```mermaid
flowchart LR
    User["🧑 User<br/>(any role)"]:::actor
    Customer["🧑 Customer"]:::actor
    Agent["🧑 Flight Agent"]:::actor
    Admin["🧑 System<br/>Administrator"]:::actor
    Gateway["🖥 Payment Gateway<br/>(simulated)"]:::external
    Timer["⏱ System Timer"]:::external

    subgraph FRS["Flight Reservation System"]
        subgraph ACC["Account"]
            UC01(["UC-01 Log in"])
            UC02(["UC-02 Log out"])
            UC03(["UC-03 Register as a customer"])
            UC13(["UC-13 Edit own profile"])
            UC20(["UC-20 Receive monthly promotion"])
        end
        subgraph SRCH["Flights"]
            UC04(["UC-04 Search for flights"])
            UC05(["UC-05 View flight details<br/>and seat availability"])
            UC15(["UC-15 View flight schedules"])
        end
        subgraph BOOK["Bookings and payments"]
            UC06(["UC-06 Book a flight"])
            UC07(["UC-07 Make a payment"])
            UC08(["UC-08 Cancel a reservation"])
            UC09(["UC-09 Modify a reservation"])
            UC10(["UC-10 View reservations<br/>and booking history"])
            UC11(["UC-11 Generate booking<br/>confirmation"])
            UC12(["UC-12 View payment history"])
            UC21(["UC-21 Process refund"])
            UC22(["UC-22 Release expired holds<br/>and complete flown trips"])
        end
        subgraph CUST["Customer management"]
            UC14(["UC-14 Manage customer profiles"])
        end
        subgraph ADM["Flight administration"]
            UC16(["UC-16 Manage flights"])
            UC17(["UC-17 Remove outdated flights"])
            UC18(["UC-18 Manage routes"])
            UC19(["UC-19 Manage aircraft"])
        end
    end

    User --- UC01
    User --- UC02
    User --- UC04

    Customer --- UC03
    Customer --- UC13
    Customer --- UC20
    Customer --- UC05
    Customer --- UC06
    Customer --- UC08
    Customer --- UC09
    Customer --- UC10
    Customer --- UC11
    Customer --- UC12

    Agent --- UC05
    Agent --- UC06
    Agent --- UC08
    Agent --- UC09
    Agent --- UC10
    Agent --- UC11
    Agent --- UC12
    Agent --- UC14
    Agent --- UC15

    UC15 --- Admin
    UC16 --- Admin
    UC17 --- Admin
    UC18 --- Admin
    UC19 --- Admin

    UC07 --- Gateway
    UC21 --- Gateway
    UC22 --- Timer

    UC06 -.->|«include»| UC05
    UC06 -.->|«include»| UC07
    UC09 -.->|«include»| UC05
    UC07 -.->|«extend»: fare increases| UC09
    UC21 -.->|«extend»: fare decreases| UC09
    UC08 -.->|«include»| UC21
    UC16 -.->|«include»: cancel flight| UC21
    UC11 -.->|«extend»: after payment| UC06

    classDef actor fill:#E8F0FE,stroke:#1A56DB,color:#0B2E6F
    classDef external fill:#FFF7E6,stroke:#B45309,color:#5C2D00
    style FRS fill:#FAFAFA,stroke:#555,stroke-width:2px
```

### Actor hierarchy

The roles are a generalization hierarchy, mirrored in code by the abstract `User` class and its subclasses
`CustomerUser`, `FlightAgent` and `SystemAdministrator`.

```mermaid
flowchart BT
    Customer["🧑 Customer"]:::actor -->|is a| User["🧑 User"]:::actor
    Agent["🧑 Flight Agent"]:::actor -->|is a| User
    Admin["🧑 System Administrator"]:::actor -->|is a| User
    classDef actor fill:#E8F0FE,stroke:#1A56DB,color:#0B2E6F
```

## Use-case catalogue

| ID | Use case | Actors | Requirement | Control class |
|---|---|---|---|---|
| UC-01 | Log in | User | §2.1 roles | `AuthController` |
| UC-02 | Log out | User | §2.1 roles | `AuthController` |
| UC-03 | Register as a customer | Customer | 3.3 | `AuthController` |
| UC-04 | Search for flights (origin, destination, date, airline) | User | 3.1 | `FlightSearchController` |
| UC-05 | View flight details and seat availability (seat map, times, prices) | Customer, Agent | 3.1 | `FlightSearchController` |
| UC-06 | Book a flight (hold a seat, then pay) | Customer, Agent | 3.2 | `BookingController` |
| UC-07 | Make a payment | Customer, Agent, Payment Gateway | 3.5 | `PaymentController` |
| UC-08 | Cancel a reservation (refund by policy) | Customer (own), Agent (any) | 3.2 | `BookingController` |
| UC-09 | Modify a reservation (new seat, or another flight on the same route) | Customer (own), Agent (any) | 3.2 | `BookingController` |
| UC-10 | View reservations and booking history | Customer (own), Agent (any) | §2 overview | `BookingController`, `CustomerController` |
| UC-11 | Generate booking confirmation (view, save as .txt or .html) | Customer, Agent | 3.2 | `BookingController` |
| UC-12 | View payment history | Customer (own), Agent (any) | 3.5 | `CustomerController`, `PaymentController` |
| UC-13 | Edit own profile | Customer | 3.3 | `CustomerController` |
| UC-14 | Manage customer profiles (create, view, edit) | Agent | 3.3 | `CustomerController` |
| UC-15 | View flight schedules | Agent, Administrator | §2.1 roles | `FlightSearchController` |
| UC-16 | Manage flights (add, update, delay, cancel, delete) | Administrator | 3.4 | `FlightManagementController` |
| UC-17 | Remove outdated flights | Administrator | 3.4 | `FlightManagementController` |
| UC-18 | Manage routes | Administrator | §2.1 roles | `RouteAndAircraftController` |
| UC-19 | Manage aircraft | Administrator | §2.1 roles | `RouteAndAircraftController` |
| UC-20 | Receive monthly promotion (random pick, shown once a month) | Customer | 3.7 | `PromotionController` |
| UC-21 | Process refund | Payment Gateway | 3.2, 3.5 | `PaymentController` |
| UC-22 | Release expired holds and complete flown trips | System Timer | Additional (reliability) | `BookingController` |

Requirement 3.6 (embedded database) is non-functional from the user's point of view; it is realized by the data
layer shown in the [package diagram](../package/package_diagram.md).
