package com.ctmslite.servlet;

import com.ctmslite.util.AuditLogger;
import com.ctmslite.util.ConsentHelper;
import com.ctmslite.util.DBConnection;
import com.ctmslite.util.DBUtil;
import com.ctmslite.util.WebUtil;
import com.ctmslite.util.Randomizer;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Module 6.3 - Patient enrollment with the three validation checks:
 *   1. Consent must be "Obtained"
 *   2. Trial must be "Active" (Suspended / Completed / Pending are blocked)
 *   3. Patient must not already be enrolled in a trial
 * Every blocked attempt is written to audit_logs and shows up on the Admin risk alerts.
 */
@WebServlet("/enrollPatient")
public class EnrollmentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try (Connection c = DBConnection.getConnection()) {
            request.setAttribute("patients", DBUtil.query(c,
                    "SELECT p.patient_id, p.name, p.consent_status, p.enrolled_trial, t.trial_name "
                    + "FROM patients p LEFT JOIN trials t ON t.trial_id = p.enrolled_trial "
                    + "ORDER BY p.patient_id"));
            request.setAttribute("trialSites", DBUtil.query(c,
                    "SELECT ts.trial_site_id, ts.trial_id, ts.site_target, t.trial_name, t.phase, t.status, "
                    + "s.site_code, s.institution, "
                    + "(SELECT COUNT(*) FROM patients p WHERE p.enrolled_trial = ts.trial_id AND p.site_id = ts.site_id) AS enrolled "
                    + "FROM trial_sites ts JOIN trials t ON t.trial_id = ts.trial_id "
                    + "JOIN sites s ON s.site_id = ts.site_id WHERE s.status = 'Active' "
                    + "ORDER BY t.trial_id, s.site_code"));
            request.getRequestDispatcher("/WEB-INF/views/enroll.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int uid = WebUtil.userId(request);
        Integer patientId = WebUtil.parseInt(request.getParameter("patient_id"));
        Integer trialSiteId = WebUtil.parseInt(request.getParameter("trial_site_id"));

        if (patientId == null || trialSiteId == null) {
            WebUtil.flash(request, "danger", "Please select a patient and an available trial site.");
            WebUtil.redirect(request, response, "/enrollPatient");
            return;
        }

        boolean enrolled = false;
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                enrolled = enroll(c, request, uid, patientId, trialSiteId);
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            WebUtil.flash(request, "danger", "Database error. Enrollment was not saved.");
        }
        WebUtil.redirect(request, response, enrolled ? "/patients" : "/enrollPatient");
    }

    private boolean enroll(Connection c, HttpServletRequest req, int uid,
                           int patientId, int trialSiteId) throws SQLException {

        Map<String, Object> p = DBUtil.queryOne(c,
                "SELECT patient_id, name, consent_status, enrolled_trial "
                + "FROM patients WHERE patient_id = ? FOR UPDATE", patientId);
        Map<String, Object> ts = DBUtil.queryOne(c,
                "SELECT ts.trial_site_id, ts.trial_id, ts.site_id, ts.site_target, "
                + "t.trial_name, t.status, t.target_enrollment, s.site_code "
                + "FROM trial_sites ts JOIN trials t ON t.trial_id = ts.trial_id "
                + "JOIN sites s ON s.site_id = ts.site_id WHERE ts.trial_site_id = ? FOR UPDATE",
                trialSiteId);
        Map<String, Object> t = ts;

        if (p == null || t == null) {
            c.rollback();
            WebUtil.flash(req, "danger", "Patient or trial site not found.");
            return false;
        }
        int trialId = ((Number) t.get("trial_id")).intValue();
        int siteId = ((Number) t.get("site_id")).intValue();
        String siteCode = (String) t.get("site_code");
        String name = (String) p.get("name");
        String trialName = (String) t.get("trial_name");
        String who = "Patient ID " + patientId + " (" + name + ")";

        // Check 1: consent
        String consent = (String) p.get("consent_status");
        if (!"Obtained".equals(consent)) {
            block(c, req, uid, "CONSENT_VIOLATION",
                    "Enrollment blocked for " + who + ": consent is " + consent + ".",
                    "Enrollment blocked: consent has not been obtained for " + name + " (status: " + consent + ").");
            return false;
        }

        // Check 2: trial status
        String trialStatus = (String) t.get("status");
        if ("Suspended".equals(trialStatus)) {
            block(c, req, uid, "SUSPENDED_TRIAL_ACCESS",
                    "Enrollment blocked for " + who + ": Trial ID " + trialId + " is suspended.",
                    "Enrollment blocked: trial \"" + trialName + "\" is suspended.");
            return false;
        }
        if (!"Active".equals(trialStatus)) {
            block(c, req, uid, "ENROLL_BLOCKED",
                    "Enrollment blocked for " + who + ": Trial ID " + trialId + " is " + trialStatus + ".",
                    "Enrollment blocked: trial \"" + trialName + "\" is not active (status: " + trialStatus + ").");
            return false;
        }

        // Check 3: duplicate enrollment
        if (p.get("enrolled_trial") != null) {
            block(c, req, uid, "DUPLICATE_ENROLLMENT",
                    "Enrollment blocked for " + who + ": already enrolled in Trial ID " + p.get("enrolled_trial") + ".",
                    "Enrollment blocked: " + name + " is already enrolled in a trial.");
            return false;
        }

        // A subject who was randomized once must never receive a second allocation.
        if (DBUtil.count(c, "SELECT COUNT(*) FROM randomization_allocations WHERE patient_id = ?", patientId) > 0) {
            block(c, req, uid, "RE_RANDOMIZATION_BLOCKED",
                    "Enrollment blocked for " + who + ": a prior randomization allocation already exists.",
                    "This patient already has a randomization record. A new allocation cannot be issued.");
            return false;
        }

        // Lock the trial row so concurrent enrollment requests cannot allocate the same sequence.
        DBUtil.queryOne(c, "SELECT trial_id FROM trials WHERE trial_id = ? FOR UPDATE", trialId);
        long trialCount = DBUtil.count(c,
                "SELECT (SELECT COUNT(*) FROM randomization_allocations WHERE trial_id = ?) + "
                + "(SELECT COUNT(*) FROM patients WHERE enrolled_trial = ? AND randomization_no IS NULL)",
                trialId, trialId);
        long siteCount = DBUtil.count(c,
                "SELECT (SELECT COUNT(*) FROM randomization_allocations WHERE trial_id = ? AND site_id = ?) + "
                + "(SELECT COUNT(*) FROM patients WHERE enrolled_trial = ? AND site_id = ? AND randomization_no IS NULL)",
                trialId, siteId, trialId, siteId);
        long trialTarget = ((Number) t.get("target_enrollment")).longValue();
        long siteTarget = ((Number) t.get("site_target")).longValue();
        if (trialCount >= trialTarget || siteCount >= siteTarget) {
            block(c, req, uid, "ENROLLMENT_TARGET_REACHED",
                    "Enrollment blocked for " + who + ": target reached for trial/site " + trialId + "/" + siteCode + ".",
                    "Enrollment target has been reached for this trial site.");
            return false;
        }

        Map<String, Object> armCounts = new java.util.HashMap<String, Object>();
        for (Map<String, Object> row : DBUtil.query(c,
                 "SELECT arm, COUNT(*) AS cnt FROM randomization_allocations WHERE trial_id = ? GROUP BY arm",
                trialId)) {
            armCounts.put((String) row.get("arm"), row.get("cnt"));
        }
        String arm = Randomizer.chooseArm(armCounts);
        long allocationSequence = DBUtil.count(c,
                 "SELECT COUNT(*) FROM randomization_allocations WHERE trial_id = ?",
                trialId) + 1;
        String randomizationNo = Randomizer.number(trialId, allocationSequence);

        DBUtil.update(c,
                "INSERT INTO randomization_allocations (trial_id, patient_id, site_id, allocation_no, arm) "
                + "VALUES (?, ?, ?, ?, ?)",
                trialId, patientId, siteId, randomizationNo, arm);
        DBUtil.update(c,
                "UPDATE patients SET enrolled_trial = ?, site_id = ?, arm = ?, randomization_no = ?, "
                + "enrolled_by = ?, enrolled_at = NOW() WHERE patient_id = ?",
                trialId, siteId, arm, randomizationNo, uid, patientId);
        ConsentHelper.upsert(c, patientId, trialId, "Obtained", uid);

        String[] visitNames = {"Visit 1 - Baseline", "Visit 2 - Week 4", "Visit 3 - Week 12", "Visit 4 - Week 24"};
        int[] visitDays = {0, 28, 84, 168};
        for (int i = 0; i < visitNames.length; i++) {
            DBUtil.update(c,
                    "INSERT INTO visits (patient_id, visit_no, visit_name, due_date, status) "
                    + "VALUES (?, ?, ?, DATE_ADD(CURDATE(), INTERVAL ? DAY), 'Scheduled')",
                    patientId, i + 1, visitNames[i], visitDays[i]);
        }
        AuditLogger.log(c, uid, "PATIENT_ENROLLED", "patients",
                who + " enrolled in Trial ID " + trialId + " (" + trialName + "), site " + siteCode
                + ", randomized to " + arm + " (" + randomizationNo + ").");
        c.commit();
        WebUtil.flash(req, "success", name + " enrolled at " + siteCode + " and randomized to " + arm
                + ". Visit schedule generated.");
        return true;
    }

    private void block(Connection c, HttpServletRequest req, int uid, String actionType,
                       String logText, String userMessage) throws SQLException {
        AuditLogger.log(c, uid, actionType, "patients", logText);
        c.commit();
        WebUtil.flash(req, "danger", userMessage);
    }
}
