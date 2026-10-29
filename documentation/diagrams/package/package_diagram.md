# Package Diagram

| | |
|---|---|
| **Deliverable** | 7.1 G, package diagram (how the three layers interact, the classes in each package, which are public) |
| **Root package** | `ca.ucalgary.frs` |
| **Related** | [Class diagram](../class/domain_class_diagram.md) |

The dependency arrows below were taken from the `import` statements in the source, not drawn by hand. In particular:

- **Presentation never imports the data layer.** Pages reach controllers through `AppContext`.
- **The data layer never imports `control` or `ui`.** It depends only on business-layer interfaces and entities.
- **The business layer owns the interfaces** (`domain.repository`, `domain.gateway`) that the data layer implements,
  so the dependency between business and data points *upward* (dependency inversion).

## G.1 Layer overview

```mermaid
flowchart TB
    APP["«composition root»<br/><b>app</b> + FlightReservationApp<br/>builds every object and injects dependencies"]
    PRES["<b>Presentation layer</b><br/>ui.shell · ui.booking · ui.customer · ui.agent<br/>ui.admin · ui.components · ui.theme<br/>«boundary» classes"]
    BIZ["<b>Business logic layer</b><br/>control · control.confirmation<br/>domain.model · domain.policy · domain.repository<br/>domain.gateway · domain.security · domain.exception<br/>«control» and «entity» classes, interfaces"]
    DATA["<b>Data layer</b><br/>data.db · data.sqlite · data.migration<br/>data.gateway · data.file<br/>SQLite, simulated payment gateway, file export"]
    DB[("SQLite file<br/>frs.db")]

    PRES -->|"calls controllers,<br/>reads entities"| BIZ
    PRES -->|"gets controllers from AppContext"| APP
    DATA -.->|"implements repository<br/>and gateway interfaces"| BIZ
    DATA --> DB
    APP -->|"wires"| BIZ
    APP -->|"wires"| DATA

    classDef layer fill:#F4F6FA,stroke:#4B5563,color:#111
    class PRES,BIZ,DATA layer
```

## G.2 Packages and their classes

`+` marks a **public** class, `~` a **package-private** class (visible only inside its own package). Interfaces are
in *italics*.

```mermaid
flowchart TB
    subgraph PRES["Presentation layer"]
        direction LR
        ui_shell["<b>ui.shell</b><hr/>+ MainFrame<br/>+ <i>Page</i><br/>+ BasePage<br/>+ PageFactory<br/>+ Brand<br/>+ Splash<br/>~ LoginView<br/>~ RegisterDialog<br/>~ PromotionDialog<br/>~ ShellPanel<br/>~ NavButton"]
        ui_booking["<b>ui.booking</b><hr/>+ FlightSearchPage<br/>+ BookingDialog<br/>+ SeatMapPanel<br/>+ FareBreakdown<br/>+ PaymentDialog<br/>+ ConfirmationDialog<br/>+ MyTripsPage<br/>+ ModifyReservationDialog<br/>+ ReservationActions<br/>+ ReservationColumns<br/>+ FlightSummary<br/>+ StatusViews"]
        ui_customer["<b>ui.customer</b><hr/>+ CustomersPage<br/>+ ProfilePage<br/>+ CustomerEditorDialog<br/>+ CustomerForm"]
        ui_agent["<b>ui.agent</b><hr/>+ ReservationsPage<br/>+ SchedulePage<br/>+ FlightColumns"]
        ui_admin["<b>ui.admin</b><hr/>+ FlightsPage<br/>+ FlightEditorDialog<br/>+ RoutesPage<br/>+ RouteEditorDialog<br/>+ AircraftPage<br/>+ AircraftEditorDialog"]
        ui_components["<b>ui.components</b><hr/>+ BackgroundTask<br/>+ ListTableModel<br/>+ AppDialog · Card · Badge<br/>+ Banner · Toasts · Tone<br/>+ EmptyState · Skeleton<br/>+ ContentPane · Toolbar<br/>+ FormBuilder · DateField<br/>+ SearchField · Segmented<br/>+ Choice · Tables · Formats<br/>+ Icons · UiErrors · WrapLabel"]
        ui_theme["<b>ui.theme</b><hr/>+ Theme<br/>+ Palette<br/>+ Typography"]
    end

    subgraph BIZ["Business logic layer"]
        direction LR
        control["<b>control</b><hr/>+ AuthController<br/>+ FlightSearchController<br/>+ BookingController<br/>+ PaymentController<br/>+ CustomerController<br/>+ FlightManagementController<br/>+ RouteAndAircraftController<br/>+ PromotionController<br/>+ Session<br/>+ EventBus<br/>~ AccessGuard"]
        control_conf["<b>control.confirmation</b><hr/>+ <i>ConfirmationRenderer</i><br/>+ PlainTextConfirmationRenderer<br/>+ HtmlConfirmationRenderer<br/>~ ConfirmationFormats"]
        domain_model["<b>domain.model</b><hr/>+ User · CustomerUser<br/>+ FlightAgent · SystemAdministrator<br/>+ UserFactory · Role · Permission<br/>+ Customer<br/>+ Flight · FlightStatus · FlightAvailability<br/>+ Airline · Airport · Route<br/>+ Aircraft · Seat · SeatClass · SeatMap<br/>+ Reservation · <i>ReservationState</i><br/>+ ReservationStatus · FareQuote<br/>+ BookingConfirmation<br/>+ Payment · PaymentStatus · PaymentType<br/>+ PaymentMethodType · CardDetails<br/>+ Promotion · Money · Validate<br/>~ PendingPaymentState<br/>~ ConfirmedState<br/>~ CancelledState<br/>~ CompletedState"]
        domain_policy["<b>domain.policy</b><hr/>+ FareCalculator<br/>+ <i>DiscountPolicy</i><br/>+ PercentageDiscountPolicy<br/>+ <i>RefundPolicy</i><br/>+ StandardRefundPolicy<br/>+ ConfirmationCodeGenerator"]
        domain_repo["<b>domain.repository</b><hr/>+ <i>TransactionManager</i><br/>+ <i>UserRepository</i><br/>+ <i>CustomerRepository</i><br/>+ <i>FlightQueryRepository</i><br/>+ <i>FlightRepository</i><br/>+ FlightSearchCriteria<br/>+ <i>ReservationRepository</i><br/>+ <i>PaymentRepository</i><br/>+ <i>RouteRepository</i><br/>+ <i>AircraftRepository</i><br/>+ <i>ReferenceDataRepository</i><br/>+ <i>PromotionRepository</i><br/>+ <i>SettingsRepository</i>"]
        domain_gateway["<b>domain.gateway</b><hr/>+ <i>PaymentGateway</i><br/>+ <i>FileExporter</i>"]
        domain_security["<b>domain.security</b><hr/>+ PasswordHasher"]
        domain_exception["<b>domain.exception</b><hr/>+ ValidationException<br/>+ BusinessRuleException<br/>+ AuthenticationException<br/>+ AuthorizationException<br/>+ NotFoundException<br/>+ DuplicateKeyException<br/>+ PaymentDeclinedException<br/>+ PersistenceException"]
    end

    subgraph DATA["Data layer"]
        direction LR
        data_db["<b>data.db</b><hr/>+ Database<br/>+ <i>ConnectionFactory</i><br/>+ SqliteConnectionFactory<br/>+ Sql<br/>+ <i>SqlWork</i><br/>+ <i>RowMapper</i>"]
        data_sqlite["<b>data.sqlite</b><hr/>+ SqliteUserRepository<br/>+ SqliteCustomerRepository<br/>+ SqliteFlightRepository<br/>+ SqliteReservationRepository<br/>+ SqlitePaymentRepository<br/>+ SqliteRouteRepository<br/>+ SqliteAircraftRepository<br/>+ SqliteReferenceDataRepository<br/>+ SqlitePromotionRepository<br/>+ SqliteSettingsRepository<br/>~ Mappers"]
        data_migration["<b>data.migration</b><hr/>+ SchemaMigrator<br/>+ <i>Migration</i><br/>+ SqlScriptMigration<br/>+ DemoDataSeeder"]
        data_gateway["<b>data.gateway</b><hr/>+ SimulatedPaymentGateway"]
        data_file["<b>data.file</b><hr/>+ LocalFileExporter"]
    end

    subgraph ROOT["Composition root"]
        app["<b>app</b><hr/>+ AppContext<br/>+ AppPaths<br/>+ SingleInstanceLock"]
        main["<b>ca.ucalgary.frs</b><hr/>+ FlightReservationApp"]
    end

    PRES -->|uses| control
    PRES -->|uses| control_conf
    PRES -->|reads| domain_model
    PRES -->|catches| domain_exception
    PRES -->|AppContext| app

    control --> control_conf
    control --> domain_model
    control --> domain_policy
    control --> domain_repo
    control --> domain_gateway
    control --> domain_security
    domain_policy --> domain_model
    domain_repo --> domain_model
    domain_gateway --> domain_model

    data_sqlite -.->|realizes| domain_repo
    data_db -.->|"realizes TransactionManager"| domain_repo
    data_gateway -.->|realizes| domain_gateway
    data_file -.->|realizes| domain_gateway
    data_sqlite --> data_db
    data_migration --> data_db
    DATA -->|maps rows to| domain_model

    main --> app
    main --> PRES
    app -->|wires| control
    app -->|wires| DATA

    style PRES fill:#EEF4FF,stroke:#1A56DB
    style BIZ fill:#F0FAF3,stroke:#15803D
    style DATA fill:#FFF7E6,stroke:#B45309
    style ROOT fill:#F5F5F5,stroke:#6B7280
```

## Dependency rules (verified against the imports)

| From | May depend on | Never depends on |
|---|---|---|
| Presentation (`ui.*`) | `control`, `control.confirmation`, `domain.model`, `domain.exception`, `app` | `data.*` |
| `control` | `domain.*`, `control.confirmation` | `ui.*`, `data.*` |
| `domain.*` | other `domain.*` packages | `control`, `ui.*`, `data.*` |
| Data (`data.*`) | `domain.model`, `domain.repository`, `domain.gateway`, `domain.exception`, `domain.security` (seeding) | `control`, `ui.*` |
| `app` | everything (it is the only place that calls `new` on repositories and controllers) | — |

Inside the presentation layer, `ui.shell` builds the pages of the feature packages through `PageFactory`, and all
feature packages share `ui.components` and `ui.theme`.

## Why some classes are package-private

| Class | Package | Reason |
|---|---|---|
| `PendingPaymentState`, `ConfirmedState`, `CancelledState`, `CompletedState` | `domain.model` | Only `Reservation` may change its state; outside code goes through `ReservationStatus.state()` |
| `AccessGuard` | `control` | Ownership checks shared by controllers, not an API for the UI |
| `ConfirmationFormats` | `control.confirmation` | Formatting helpers shared by the two renderers |
| `Mappers` | `data.sqlite` | Row-to-entity mapping used only by the SQLite repositories |
| `LoginView`, `RegisterDialog`, `PromotionDialog`, `ShellPanel`, `NavButton` | `ui.shell` | Created and owned by `MainFrame`; no other package needs them |
