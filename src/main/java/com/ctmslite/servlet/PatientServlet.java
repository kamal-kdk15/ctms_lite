package com.ctmslite.servlet;

import com.ctmslite.util.AuditLogger;
import com.ctmslite.util.ConsentHelper;
import com.ctmslite.util.DBConnection;
import com.ctmslite.util.DBUtil;
import com.ctmslite.util.WebUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Module 6.3 - Patient registration and consent tracking (Staff). */
@WebServlet("/patients")
public class PatientServlet extends HttpServlet {

    private static final List<String> GENDERS = Arrays.asList("Male", "Female", "Other");
    private static final List<String> CONSENTS = Arrays.asList("Obtained", "Pending", "Withdrawn");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Integer trialFilter = WebUtil.parseInt(request.getParameter("trial"));
        String consentFilter = request.getParameter("consent");
        if (!CONSENTS.contains(consentFilter)) {
            consentFilter = null;
        }

        StringBuilder sql = new StringBuilder(
                "SELECT p.patient_id, p.name, p.age, p.gender, p.consent_status, "
                + "p.enrolled_trial, p.enrolled_at, p.arm, p.randomization_no, s.site_code, t.trial_name "
                + "FROM patients p LEFT JOIN trials t ON t.trial_id = p.enrolled_trial "
                + "LEFT JOIN sites s ON s.site_id = p.site_id WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (trialFilter != null) {
            sql.append(" AND p.enrolled_trial = ?");
            params.add(trialFilter);
        }
        if (consentFilter != null) {
            sql.append(" AND p.consent_status = ?");
            params.add(consentFilter);
        }
        sql.append(" ORDER BY p.patient_id");

        try (Connection c = DBConnection.getConnection()) {
            request.setAttribute("patients", DBUtil.query(c, sql.toString(), params.toArray()));
            request.setAttribute("trials", DBUtil.query(c,
                    "SELECT trial_id, trial_name FROM trials ORDER BY trial_id"));
            request.setAttribute("selTrial", trialFilter);
            request.setAttribute("selConsent", consentFilter);
            request.getRequestDispatcher("/WEB-INF/views/patients.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String action = request.getParameter("action");
        int uid = WebUtil.userId(request);
        try (Connection c = DBConnection.getConnection()) {
            if ("register".equals(action)) {
                registerPatient(c, request, uid);
            } else if ("updateConsent".equals(action)) {
                updateConsent(c, request, uid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            WebUtil.flash(request, "danger", "Database error. Please try again.");
        }
        WebUtil.redirect(request, response, "/patients");
    }

    private void registerPatient(Connection c, HttpServletRequest req, int uid) throws SQLException {
        String name = WebUtil.trim(req.getParameter("name"));
        Integer age = WebUtil.parseInt(req.getParameter("age"));
        String gender = req.getParameter("gender");
        String consent = req.getParameter("consent_status");

        if (WebUtil.isBlank(name) || name.length() > 100 || age == null || age < 1 || age > 120
                || !GENDERS.contains(gender) || !CONSENTS.contains(consent)) {
            WebUtil.flash(req, "danger", "Please enter a valid name, age (1-120), gender and consent status.");
            return;
        }
        DBUtil.update(c,
                "INSERT INTO patients (name, age, gender, consent_status) VALUES (?, ?, ?, ?)",
                name, age, gender, consent);
        AuditLogger.log(c, uid, "PATIENT_REGISTERED", "patients",
                "Patient \"" + name + "\" registered with consent status " + consent + ".");
        WebUtil.flash(req, "success", "Patient \"" + name + "\" registered. You can now enroll them in a trial.");
    }

    private void updateConsent(Connection c, HttpServletRequest req, int uid) throws SQLException {
        Integer pid = WebUtil.parseInt(req.getParameter("patient_id"));
        String status = req.getParameter("consent_status");
        if (pid == null || !CONSENTS.contains(status)) {
            WebUtil.flash(req, "danger", "Invalid patient or consent status.");
            return;
        }
        Map<String, Object> p = DBUtil.queryOne(c,
                "SELECT patient_id, name, consent_status, enrolled_trial FROM patients WHERE patient_id = ?",
                pid);
        if (p == null) {
            WebUtil.flash(req, "danger", "Patient not found.");
            return;
        }
        String oldStatus = (String) p.get("consent_status");
        if (oldStatus.equals(status)) {
            WebUtil.flash(req, "warning", "Consent is already " + status + ".");
            return;
        }

        Object trialObj = p.get("enrolled_trial");
        c.setAutoCommit(false);
        try {
            if ("Withdrawn".equals(status) && trialObj != null) {
                // withdrawn consent: the patient must not stay enrolled in the trial
                int trialId = ((Number) trialObj).intValue();
                ConsentHelper.upsert(c, pid, trialId, status, uid);
                DBUtil.update(c,
                        "UPDATE patients SET consent_status = ?, enrolled_trial = NULL, "
                        + "enrolled_by = NULL, enrolled_at = NULL WHERE patient_id = ?",
                        status, pid);
                int cancelledVisits = DBUtil.update(c,
                        "UPDATE visits SET status = 'Cancelled', note = 'Cancelled after consent withdrawal' "
                        + "WHERE patient_id = ? AND status = 'Scheduled'", pid);
                if (cancelledVisits > 0) {
                    AuditLogger.log(c, uid, "VISITS_CANCELLED", "visits",
                            cancelledVisits + " scheduled visit(s) cancelled after consent withdrawal for Patient ID " + pid + ".");
                }
                AuditLogger.log(c, uid, "CONSENT_UPDATED", "consent_records",
                        "Consent for Patient ID " + pid + " changed from " + oldStatus + " to " + status + ".");
                AuditLogger.log(c, uid, "PATIENT_UNENROLLED", "patients",
                        "Patient ID " + pid + " removed from Trial ID " + trialId + " after consent withdrawal.");
                WebUtil.flash(req, "warning",
                        "Consent withdrawn. " + p.get("name") + " has been removed from the trial.");
            } else {
                DBUtil.update(c, "UPDATE patients SET consent_status = ? WHERE patient_id = ?", status, pid);
                if (trialObj != null) {
                    ConsentHelper.upsert(c, pid, ((Number) trialObj).intValue(), status, uid);
                }
                AuditLogger.log(c, uid, "CONSENT_UPDATED", "consent_records",
                        "Consent for Patient ID " + pid + " changed from " + oldStatus + " to " + status + ".");
                WebUtil.flash(req, "success", "Consent status updated to " + status + ".");
            }
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        }
    }
}
