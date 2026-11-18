package com.ctmslite.util;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Writes one row into audit_logs for every critical action
 * (21 CFR Part 11 style audit trail).
 */
public class AuditLogger {

    /** Use this version inside an existing transaction / connection. */
    public static void log(Connection c, int userId, String actionType,
                           String affectedTable, String description) throws SQLException {
        DBUtil.update(c,
                "INSERT INTO audit_logs (user_id, action_type, affected_table, description) "
                + "VALUES (?, ?, ?, ?)",
                userId, actionType, affectedTable, description);
    }
}
