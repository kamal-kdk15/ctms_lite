package com.ctmslite.util;

import java.sql.Connection;
import java.sql.SQLException;

/** Keeps consent_records in sync (one row per patient per trial). */
public class ConsentHelper {

    public static void upsert(Connection c, int patientId, int trialId,
                              String status, int userId) throws SQLException {
        DBUtil.update(c,
                "INSERT INTO consent_records (patient_id, trial_id, consent_status, recorded_by) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE consent_status = VALUES(consent_status), "
                + "recorded_by = VALUES(recorded_by), recorded_at = NOW()",
                patientId, trialId, status, userId);
    }
}
