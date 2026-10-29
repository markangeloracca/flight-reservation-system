package ca.ucalgary.frs.control;

import ca.ucalgary.frs.domain.exception.AuthenticationException;
import ca.ucalgary.frs.domain.exception.ValidationException;
import ca.ucalgary.frs.domain.model.Customer;
import ca.ucalgary.frs.domain.model.Role;
import ca.ucalgary.frs.domain.model.User;
import ca.ucalgary.frs.domain.model.UserFactory;
import ca.ucalgary.frs.domain.repository.CustomerRepository;
import ca.ucalgary.frs.domain.repository.TransactionManager;
import ca.ucalgary.frs.domain.repository.UserRepository;
import ca.ucalgary.frs.domain.security.PasswordHasher;

import java.util.regex.Pattern;

/** Use cases: Log in, Log out, Register as a customer. */
public final class AuthController {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    public static final int MIN_PASSWORD_LENGTH = 8;

    private final TransactionManager db;
    private final UserRepository users;
    private final CustomerRepository customers;
    private final PasswordHasher hasher;
    private final Session session;
    private final EventBus events;
    /** Used to spend the same time on unknown usernames as on wrong passwords. */
    private final String dummyHash;

    public AuthController(TransactionManager db, UserRepository users, CustomerRepository customers, PasswordHasher hasher,
                          Session session, EventBus events) {
        this.db = db;
        this.users = users;
        this.customers = customers;
        this.hasher = hasher;
        this.session = session;
        this.events = events;
        this.dummyHash = hasher.hash("not-a-real-password".toCharArray());
    }

    /** Verifies credentials and starts a session. The password buffer is wiped afterwards. */
    public User login(String username, char[] password) {
        try {
            if (username == null || username.isBlank() || password == null || password.length == 0) {
                throw new ValidationException("Enter your username and password.");
            }
            var credentials = users.findCredentials(username.trim());
            if (credentials.isEmpty()) {
                hasher.verify(password, dummyHash);
                throw new AuthenticationException();
            }
            if (!hasher.verify(password, credentials.get().passwordHash())) {
                throw new AuthenticationException();
            }
            User user = credentials.get().user();
            session.signIn(user);
            return user;
        } finally {
            PasswordHasher.wipe(password);
        }
    }

    public void logout() {
        session.signOut();
    }

    /** Creates a customer profile with a sign-in account, then signs the new customer in. */
    public User registerCustomer(Customer profile, String username, char[] password, char[] confirmPassword) {
        try {
            String name = username == null ? "" : username.trim();
            if (!USERNAME.matcher(name).matches()) {
                throw new ValidationException("username",
                        "Username must be 3-30 characters: letters, digits, dot, dash or underscore.");
            }
            validatePassword(password, confirmPassword);
            String hash = hasher.hash(password);

            User user = db.inTransaction(() -> {
                if (users.usernameExists(name)) {
                    throw new ValidationException("username", "That username is already taken.");
                }
                if (customers.findByEmail(profile.email()).isPresent()) {
                    throw new ValidationException("email",
                            "A customer with this email already exists. Ask a flight agent for help with your account.");
                }
                Customer saved = customers.save(profile.toBuilder().id(null).build());
                long userId = users.create(name, hash, Role.CUSTOMER, saved.fullName(), saved.email(), saved.id());
                return UserFactory.create(Role.CUSTOMER, userId, name, saved.fullName(), saved.email(), saved.id());
            });
            events.publish(EventBus.Topic.CUSTOMERS);
            session.signIn(user);
            return user;
        } finally {
            PasswordHasher.wipe(password);
            PasswordHasher.wipe(confirmPassword);
        }
    }

    static void validatePassword(char[] password, char[] confirm) {
        if (password == null || password.length < MIN_PASSWORD_LENGTH) {
            throw new ValidationException("password", "Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        boolean letter = false;
        boolean digit = false;
        for (char ch : password) {
            letter |= Character.isLetter(ch);
            digit |= Character.isDigit(ch);
        }
        if (!letter || !digit) {
            throw new ValidationException("password", "Password must contain at least one letter and one digit.");
        }
        if (confirm == null || !java.util.Arrays.equals(password, confirm)) {
            throw new ValidationException("confirmPassword", "Passwords do not match.");
        }
    }
}
