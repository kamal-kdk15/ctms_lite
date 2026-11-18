package com.ctmslite.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/** Re-authenticated electronic-signature workflow for this educational CTMS prototype. */
public final class ESign {
    public static final List<String> MEANINGS =
            Arrays.asList("Reviewed", "Approved", "Verified", "Authorized");

    private ESign() {}

    public static String apply(Connection c, HttpServletRequest req, int userId,
                               String recordType, int recordId, String recordState) throws SQLException {
        String meaning = req.getParameter("sig_meaning");
        String password = req.getParameter("sig_password");
        boolean attest = "on".equals(req.getParameter("sig_attest"));
        if (!MEANINGS.contains(meaning) || WebUtil.isBlank(password) || !attest) {
            return "Electronic signature required: select a meaning, re-enter your password and accept the declaration.";
        }
        Map<String, Object> user = DBUtil.queryOne(c,
                "SELECT password FROM users WHERE user_id = ?", userId);
        if (user == null || !PasswordUtil.sha256(password).equals(user.get("password"))) {
            AuditLogger.log(c, userId, "SIGNATURE_FAILED", "e_signatures",
                    "Electronic signature authentication failed for " + recordType + " #" + recordId + ".");
            return "Password verification failed. No change was made.";
        }
        String signedAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date());
        String payload = userId + "|" + recordType + "|" + recordId + "|" + meaning
                + "|" + recordState + "|" + signedAt;
        DBUtil.update(c,
                "INSERT INTO e_signatures (user_id, record_type, record_id, meaning, record_state, signed_at, signature_hash) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                userId, recordType, recordId, meaning, recordState,
                java.sql.Timestamp.valueOf(signedAt), PasswordUtil.sha256(payload));
        req.setAttribute("sigMeaning", meaning);
        AuditLogger.log(c, userId, "ESIGN_APPLIED", "e_signatures",
                meaning + " signature applied to " + recordType + " #" + recordId
                + " for state " + recordState + " (hash " + PasswordUtil.sha256(payload).substring(0, 16) + ").");
        return null;
    }
}
