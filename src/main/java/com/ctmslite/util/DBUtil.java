package com.ctmslite.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Small helper around PreparedStatement so that every query in the
 * project stays parameterised (protects against SQL injection).
 */
public class DBUtil {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss", Locale.ENGLISH);

    /** DATE and TIMESTAMP columns are converted to readable text so the JSP pages can print them directly. */
    private static Object readable(Object v) {
        if (v instanceof Timestamp) {
            return TIME_FMT.format(((Timestamp) v).toLocalDateTime());
        }
        if (v instanceof java.sql.Date) {
            return DATE_FMT.format(((java.sql.Date) v).toLocalDate());
        }
        return v;
    }

    private static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    /** Runs a SELECT and returns every row as a column-name -> value map (dates as readable text). */
    public static List<Map<String, Object>> query(Connection c, String sql, Object... params)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(md.getColumnLabel(i), readable(rs.getObject(i)));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    /** Returns the first row of a SELECT, or null when nothing matches. */
    public static Map<String, Object> queryOne(Connection c, String sql, Object... params)
            throws SQLException {
        List<Map<String, Object>> rows = query(c, sql, params);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** For SELECT COUNT(*) ... style queries. */
    public static long count(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    /** INSERT / UPDATE / DELETE. Returns the number of affected rows. */
    public static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }
}
