# Activity Diagram 1 — Login Process

| | |
|---|---|
| **Deliverable** | 7.1 A, activity diagram 1 (login process) |
| **Use cases** | UC-01 Log in, UC-03 Register as a customer, UC-20 Receive monthly promotion |
| **Control class** | `AuthController` (`login`, `registerCustomer`), `PromotionController` |
| **Boundary classes** | `LoginView`, `RegisterDialog`, `MainFrame`, `PromotionDialog` |
| **Related diagrams** | [Login sequence](../sequence/login_sequence.md), [System state machine](../state/system_state_machine.md) |

## Description

The sign-in screen is the first thing every user sees. A user signs in with a username and password, uses a
one-click demo account button, or (customers only) creates a new account. Credentials are checked against a salted
PBKDF2 hash. An unknown username is checked against a dummy hash so both failures take the same time and give the same
message. The password buffer is wiped after every attempt.

On success the `Session` changes, and `MainFrame` builds a workspace containing only the pages the user's role allows.
A customer who has not yet seen this month's promotion is shown it once.

## Diagram

```mermaid
flowchart TD
    start(( )):::terminal --> showForm("Show sign-in screen")

    showForm --> choice{"User chooses"}
    choice -->|"Sign in"| enter("Enter username and password")
    choice -->|"Demo account button"| demo("Fill in demo credentials")
    choice -->|"Create an account"| regForm("Fill in profile, username,<br/>password and confirmation")

    demo --> submit
    enter --> submit("Submit credentials")
    submit --> blank{"Username or<br/>password blank?"}
    blank -->|yes| errBlank["Show 'Enter your username<br/>and password'"]:::error
    errBlank --> enter
    blank -->|no| lookup("Look up stored credentials<br/>by username")
    lookup --> found{"Username<br/>found?"}
    found -->|no| dummy("Verify against dummy hash<br/>(same timing as a real check)")
    dummy --> errAuth
    found -->|yes| verify("Verify password against<br/>salted PBKDF2 hash")
    verify --> match{"Password<br/>matches?"}
    match -->|no| errAuth["Show 'Invalid username or password'<br/>and clear the password field"]:::error
    errAuth --> enter
    match -->|yes| signIn

    regForm --> regValid{"Username, password and<br/>profile fields valid?"}
    regValid -->|no| errReg["Show the error next to<br/>the field at fault"]:::error
    errReg --> regForm
    regValid -->|yes| regUnique{"Username free and<br/>email not already used?"}
    regUnique -->|no| errReg
    regUnique -->|yes| regSave("Create Customer and User account<br/>in one transaction")
    regSave --> signIn

    signIn("Wipe password buffer and<br/>start the session") --> build("Build the workspace from<br/>the role's permissions")
    build --> toast("Show 'Signed in as …'")
    toast --> isCust{"Signed-in user<br/>is a customer?"}
    isCust -->|no| done
    isCust -->|yes| promo{"Unseen promotion<br/>this month?"}
    promo -->|no| done
    promo -->|yes| showPromo("Show promotion dialog<br/>and mark it as seen")
    showPromo --> done(((" "))):::terminal

    classDef terminal fill:#111,stroke:#111,color:#111
    classDef error fill:#FDECEA,stroke:#C62828,color:#7F1D1D
```

## Notes

- **Validation rules (registration):** username is 3–30 characters (letters, digits, `.`, `-`, `_`); password is at
  least 8 characters with a letter and a digit; the confirmation must match.
- **Concurrency:** the credential check runs off the Swing event thread through `BackgroundTask`. The sign-in button
  shows "Signing in…" and is disabled until the check finishes.
- **Errors:** shown in an inline banner (`Banner`) above the form, never in a blocking pop-up.
