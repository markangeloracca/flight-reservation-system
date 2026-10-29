# Sequence Diagram 1 — Log In (`AuthController`)

| | |
|---|---|
| **Deliverable** | 7.1 D, sequence diagram 1 |
| **Use case** | UC-01 Log in ([Scenario 4](../usecase/use_case_scenarios.md#scenario-4--uc-01-log-in)) |
| **Control class** | `AuthController` |
| **Boundary classes** | `LoginView`, `MainFrame`, `PromotionDialog` |
| **Related diagrams** | [Login activity](../activity/login_activity.md), [System state machine](../state/system_state_machine.md) |

## Diagram

```mermaid
sequenceDiagram
    autonumber
    actor U as User
    participant LV as «boundary»<br/>LoginView
    participant BT as «utility»<br/>BackgroundTask
    participant AC as «control»<br/>AuthController
    participant UR as «data access»<br/>SqliteUserRepository
    participant DB as «database»<br/>SQLite
    participant PH as «security»<br/>PasswordHasher
    participant SS as «control»<br/>Session
    participant MF as «boundary»<br/>MainFrame
    participant PF as «factory»<br/>PageFactory
    participant PC as «control»<br/>PromotionController

    U->>LV: enter username and password, click Sign in
    activate LV
    LV->>LV: disable button, show "Signing in…"
    LV->>BT: of(auth.login(username, password)).start()
    deactivate LV
    activate BT
    Note over BT: runs on a worker thread, off the Swing event thread

    BT->>AC: login(username, password)
    activate AC
    AC->>AC: check username and password are not blank
    AC->>UR: findCredentials(username)
    activate UR
    UR->>DB: SELECT user and password_hash WHERE username = ?
    DB-->>UR: row or nothing
    UR-->>AC: Optional of StoredCredentials(user, passwordHash)
    deactivate UR

    alt username found
        AC->>PH: verify(password, storedHash)
        PH-->>AC: matches?
    else unknown username
        AC->>PH: verify(password, dummyHash)
        PH-->>AC: false (same time as a real check)
    end

    opt password matches
        AC->>SS: signIn(user)
        activate SS
        SS-)MF: propertyChange("user", null, user)
        deactivate SS
    end
    AC->>PH: wipe(password)
    AC-->>BT: User, or throw AuthenticationException
    deactivate AC
    BT-->>LV: onSuccess / onFailure (on the event thread)
    deactivate BT

    alt signed in
        LV->>LV: re-enable Sign in button
        MF->>MF: onUserChanged(user) via invokeLater
        activate MF
        MF->>PF: pagesFor(user, ctx)
        PF->>PF: keep pages whose Permission user.can(…)
        PF-->>MF: pages for this role
        MF-->>U: show role workspace and "Signed in as …"
        opt user is a customer
            MF->>PC: unseenPromotionForCurrentCustomer()
            activate PC
            PC-->>MF: Optional of Promotion
            deactivate PC
            opt promotion not yet seen this month
                MF-->>U: PromotionDialog.show(promotion)
                MF->>PC: markSeenByCurrentCustomer()
            end
        end
        deactivate MF
    else invalid credentials
        LV-->>U: banner "Invalid username or password", clear password
    end
```

## Notes

- **Observer:** `AuthController` does not know about `MainFrame`. It only changes the `Session`; `MainFrame` listens
  for the `user` property and swaps the sign-in screen for the workspace.
- **Factory Method:** `PageFactory` builds the navigation from a registry of *(permission → page)*, so adding a page
  for a role never touches `MainFrame`.
- **Security:** passwords are compared as salted PBKDF2 hashes and the `char[]` buffer is wiped in a `finally` block,
  on success and on failure.
