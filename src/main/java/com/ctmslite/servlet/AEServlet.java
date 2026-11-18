package com.ctmslite.servlet;

import com.ctmslite.util.AuditLogger;
import com.ctmslite.util.DBConnection;
import com.ctmslite.util.DBUtil;
import com.ctmslite.util.WebUtil;
import com.ctmslite.util.ESign;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Module 6.4 / 6.5
 *   /aeReport - Staff submit adverse events
 *   /aeReview - Admin review adverse events and update their status
 */
@WebServlet(urlPatterns = {"/aeReport", "/aeReview"})
public class AEServlet extends HttpServlet {

    private static final List<String> SEVERITIES = Arrays.asList("Mild", "Moderate", "Severe");
    private static final List<String> STATUSES = Arrays.asList("Open", "Under Review", "Resolved");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try (Connection c = DBConnection.getConnection()) {
            if ("/aeReport".equals(request.getServletPath())) {
                int uid = WebUtil.userId(request);
                request.setAttribute("enrolledPatients", DBUtil.query(c,
                        "SELECT p.patient_id, p.name, t.trial_name FROM patients p "
                        + "JOIN trials t ON t.trial_id = p.enrolled_trial ORDER BY p.name"));
                request.setAttribute("myAEs", DBUtil.query(c,
                         "SELECT a.ae_id, p.name AS patient_name, t.trial_name, a.severity, a.status, a.is_sae, "
                        + "a.occurrence_date, a.description, a.reported_at "
                        + "FROM adverse_events a JOIN patients p ON p.patient_id = a.patient_id "
                        + "JOIN trials t ON t.trial_id = a.trial_id "
                        + "WHERE a.reported_by = ? ORDER BY a.ae_id DESC LIMIT 10", uid));
                request.setAttribute("today", LocalDate.now().toString());
                request.getRequestDispatcher("/WEB-INF/views/aeReport.jsp").forward(request, response);
            } else {
                String severity = request.getParameter("severity");
                String status = request.getParameter("status");
                if (!SEVERITIES.contains(severity)) {
                    severity = null;
                }
                if (!STATUSES.contains(status)) {
                    status = null;
                }
                StringBuilder sql = new StringBuilder(
                         "SELECT a.ae_id, a.description, a.severity, a.status, a.is_sae, a.occurrence_date, "
                        + "a.reported_at, p.name AS patient_name, t.trial_name, u.username AS reporter, "
                        + "TIMESTAMPDIFF(HOUR, a.reported_at, NOW()) AS hrs_open, "
                        + "(86400 - TIMESTAMPDIFF(SECOND, a.reported_at, NOW())) AS sae_seconds_left "
                        + "FROM adverse_events a "
                        + "JOIN patients p ON p.patient_id = a.patient_id "
                        + "JOIN trials t ON t.trial_id = a.trial_id "
                        + "JOIN users u ON u.user_id = a.reported_by WHERE 1=1");
                List<Object> params = new ArrayList<>();
                if (severity != null) {
                    sql.append(" AND a.severity = ?");
                    params.add(severity);
                }
                if (status != null) {
                    sql.append(" AND a.status = ?");
                    params.add(status);
                }
                sql.append(" ORDER BY FIELD(a.status,'Open','Under Review','Resolved'), "
                        + "FIELD(a.severity,'Severe','Moderate','Mild'), a.reported_at");
                request.setAttribute("aes", DBUtil.query(c, sql.toString(), params.toArray()));
                request.setAttribute("selSeverity", severity);
                request.setAttribute("selStatus", status);
                request.getRequestDispatcher("/WEB-INF/views/aeReview.jsp").forward(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int uid = WebUtil.userId(request);
        boolean report = "/aeReport".equals(request.getServletPath());
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (report) {
                    submitAE(c, request, uid);
                } else {
                    updateStatus(c, request, uid);
                }
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            WebUtil.flash(request, "danger", "Database error. Please try again.");
        }
        WebUtil.redirect(request, response, report ? "/aeReport" : "/aeReview");
    }

    private void submitAE(Connection c, HttpServletRequest req, int uid) throws SQLException {
        Integer pid = WebUtil.parseInt(req.getParameter("patient_id"));
        String severity = req.getParameter("severity");
        String description = WebUtil.trim(req.getParameter("description"));
        String occ = req.getParameter("occurrence_date");
        boolean isSae = "on".equals(req.getParameter("is_sae"));

        if (pid == null || !SEVERITIES.contains(severity) || WebUtil.isBlank(description)
                || WebUtil.isBlank(occ)) {
            WebUtil.flash(req, "danger", "Patient, severity, date and description are required.");
            return;
        }
        if (description.length() > 2000) {
            WebUtil.flash(req, "danger", "Description is too long (max 2000 characters).");
            return;
        }
        LocalDate occDate;
        try {
            occDate = LocalDate.parse(occ);
        } catch (DateTimeParseException e) {
            WebUtil.flash(req, "danger", "Please enter a valid date of occurrence.");
            return;
        }
        if (occDate.isAfter(LocalDate.now())) {
            WebUtil.flash(req, "danger", "Date of occurrence cannot be in the future.");
            return;
        }
        Map<String, Object> p = DBUtil.queryOne(c,
                "SELECT patient_id, name, enrolled_trial FROM patients WHERE patient_id = ?", pid);
        if (p == null || p.get("enrolled_trial") == null) {
            WebUtil.flash(req, "danger", "Adverse events can only be reported for enrolled patients.");
            return;
        }
        int trialId = ((Number) p.get("enrolled_trial")).intValue();

        DBUtil.update(c,
                "INSERT INTO adverse_events (patient_id, trial_id, description, severity, status, is_sae, "
                + "occurrence_date, reported_by) VALUES (?, ?, ?, ?, 'Open', ?, ?, ?)",
                pid, trialId, description, severity, isSae ? 1 : 0, Date.valueOf(occDate), uid);
        AuditLogger.log(c, uid, "AE_SUBMITTED", "adverse_events",
                severity + (isSae ? " serious" : "") + " adverse event reported for Patient ID " + pid + " in Trial ID " + trialId + ".");
        WebUtil.flash(req, "success", "Adverse event reported (" + severity + (isSae ? ", SAE" : "") + "). Status: Open.");
    }

    private void updateStatus(Connection c, HttpServletRequest req, int uid) throws SQLException {
        Integer aeId = WebUtil.parseInt(req.getParameter("ae_id"));
        String status = req.getParameter("status");
        if (aeId == null || !STATUSES.contains(status)) {
            WebUtil.flash(req, "danger", "Invalid adverse event or status.");
            return;
        }
        Map<String, Object> ae = DBUtil.queryOne(c,
                "SELECT status, is_sae FROM adverse_events WHERE ae_id = ?", aeId);
        if (ae == null) {
            WebUtil.flash(req, "danger", "Adverse event not found.");
            return;
        }
        String oldStatus = (String) ae.get("status");
        if (oldStatus.equals(status)) {
            WebUtil.flash(req, "warning", "Adverse event is already " + status + ".");
            return;
        }
        String sigError = ESign.apply(c, req, uid, "AE_REVIEW", aeId,
                "status=" + oldStatus + ">" + status);
        if (sigError != null) {
            WebUtil.flash(req, "danger", sigError);
            return;
        }
        DBUtil.update(c,
                "UPDATE adverse_events SET status = ?, sae_reviewed_at = CASE WHEN is_sae = 1 AND ? <> 'Open' "
                + "THEN NOW() ELSE sae_reviewed_at END WHERE ae_id = ?", status, status, aeId);
        AuditLogger.log(c, uid, "AE_STATUS_UPDATE", "adverse_events",
                "AE ID " + aeId + " status changed from " + oldStatus + " to " + status
                + " [e-signature: " + req.getAttribute("sigMeaning") + "].");
        WebUtil.flash(req, "success", "AE #" + aeId + " moved to " + status + " with electronic signature.");
    }
}
