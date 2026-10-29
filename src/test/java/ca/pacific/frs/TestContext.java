package ca.pacific.frs;

import ca.pacific.frs.app.AppContext;
import ca.pacific.frs.data.db.SqliteConnectionFactory;
import ca.pacific.frs.data.gateway.SimulatedPaymentGateway;
import ca.pacific.frs.domain.model.CardDetails;
import ca.pacific.frs.domain.model.PaymentMethodType;
import ca.pacific.frs.domain.security.PasswordHasher;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Builds a full application context on a fresh SQLite file with a controllable clock. */
public final class TestContext {
    public static final LocalDateTime START = LocalDateTime.of(2026, 11, 2, 10, 0);

    public final MutableClock clock = new MutableClock(START);
    public final AppContext ctx;

    public TestContext(Path dir) {
        ctx = new AppContext(new SqliteConnectionFactory(dir.resolve("test.db")), clock, new PasswordHasher(1_000),
                new SimulatedPaymentGateway(0));
    }

    public TestContext login(String user, String pass) {
        ctx.auth().login(user, pass.toCharArray());
        return this;
    }

    public static CardDetails goodCard() {
        return new CardDetails(PaymentMethodType.CREDIT_CARD, "Test Person", "4242 4242 4242 4242",
                YearMonth.now().plusYears(2), "123");
    }

    public static CardDetails declinedCard() {
        return new CardDetails(PaymentMethodType.CREDIT_CARD, "Test Person", "4000 0000 0000 0002",
                YearMonth.now().plusYears(2), "123");
    }

    /** Clock whose time tests can move forward. */
    public static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(LocalDateTime start) {
            this.now = start.toInstant(ZoneOffset.UTC);
        }

        public void advance(Duration d) {
            now = now.plus(d);
        }

        public void set(LocalDateTime t) {
            now = t.toInstant(ZoneOffset.UTC);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
