package ca.pacific.frs.data;

import ca.pacific.frs.TestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationTest {
    @TempDir
    Path dir;

    @Test
    void reopeningAnExistingDatabaseDoesNotReseed() {
        TestContext first = new TestContext(dir);
        first.ctx.auth().login("admin", "admin123".toCharArray());
        int routes = first.ctx.routesAndAircraft().routes().size();
        TestContext second = assertDoesNotThrow(() -> new TestContext(dir));
        second.ctx.auth().login("admin", "admin123".toCharArray());
        assertTrue(routes > 0);
        assertTrue(second.ctx.routesAndAircraft().routes().size() == routes);
    }
}
