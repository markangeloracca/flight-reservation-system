package ca.ucalgary.frs.data.db;

import ca.ucalgary.frs.domain.model.Money;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Thin JDBC helpers. Every statement is a {@link PreparedStatement} with bound parameters, never
 * string concatenation of user input.
 */
public final class Sql {
    /** Fixed-width timestamp format so text comparison in SQL matches chronological order. */
    public static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private Sql() {
    }

    public static <T> List<T> list(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
                return out;
            }
        }
    }

    public static <T> Optional<T> one(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> rows = list(c, sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public static boolean exists(Connection c, String sql, Object... params) throws SQLException {
        return one(c, sql, rs -> Boolean.TRUE, params).isPresent();
    }

    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Executes an INSERT and returns the generated row id. */
    public static long insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("No generated key returned");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object p = params[i];
            int idx = i + 1;
            if (p == null) {
                ps.setNull(idx, Types.NULL);
            } else if (p instanceof LocalDateTime t) {
                ps.setString(idx, t.format(TIMESTAMP));
            } else if (p instanceof LocalDate d) {
                ps.setString(idx, d.toString());
            } else if (p instanceof YearMonth m) {
                ps.setString(idx, m.toString());
            } else if (p instanceof Money m) {
                ps.setLong(idx, m.cents());
            } else if (p instanceof Enum<?> e) {
                ps.setString(idx, e.name());
            } else if (p instanceof Boolean b) {
                ps.setInt(idx, b ? 1 : 0);
            } else {
                ps.setObject(idx, p);
            }
        }
    }

    // ---- reading helpers ----

    public static LocalDateTime timestamp(ResultSet rs, String column) throws SQLException {
        String v = rs.getString(column);
        return v == null ? null : LocalDateTime.parse(v, TIMESTAMP);
    }

    public static LocalDate date(ResultSet rs, String column) throws SQLException {
        String v = rs.getString(column);
        return v == null ? null : LocalDate.parse(v);
    }

    public static Money money(ResultSet rs, String column) throws SQLException {
        return Money.ofCents(rs.getLong(column));
    }

    public static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long v = rs.getLong(column);
        return rs.wasNull() ? null : v;
    }

    /** Escapes a LIKE pattern fragment; use with {@code ESCAPE '\'}. */
    public static String likeContains(String text) {
        String escaped = text.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
