package com.ctmslite.servlet;

import com.ctmslite.util.DBConnection;
import com.ctmslite.util.DBUtil;
import com.ctmslite.util.WebUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Module 6.5 - Admin dashboard (live counts + risk alerts) and Staff dashboard.
 * All numbers come from live SQL COUNT queries on every page load.
 */
@WebServlet(urlPatterns = {"/adminDashboard", "/staffDashboard"})
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try (Connection c = DBConnection.getConnection()) {
            if ("/adminDashboard".equals(request.getServletPath())) {
                loadAdmin(c, request);
                request.getRequestDispatcher("/WEB-INF/views/adminDashboard.jsp")
                        .forward(request, response);
            } else {
                loadStaff(c, request);
                request.getRequestDispatcher("/WEB-INF/views/staffDashboard.jsp")
                        .forward(request, response);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void loadAdmin(Connection c, HttpServletRequest request) throws SQLException {
        request.setAttribute("activeTrials",
                DBUtil.count(c, "SELECT COUNT(*) FROM trials WHERE status = 'Active'"));
        request.setAttribute("totalTrials", DBUtil.count(c, "SELECT COUNT(*) FROM trials"));
        request.setAttribute("enrolledPatients",
                DBUtil.count(c, "SELECT COUNT(*) FROM patients WHERE enrolled_trial IS NOT NULL"));
        request.setAttribute("openAEs",
                DBUtil.count(c, "SELECT COUNT(*) FROM adverse_events WHERE status = 'Open'"));

        List<Map<String, Object>> alerts = new ArrayList<>();

        // 1) Explicitly classified SAEs still awaiting first review. Clock starts at report time.
        List<Map<String, Object>> severe = DBUtil.query(c,
                "SELECT a.ae_id, p.name AS patient_name, t.trial_name, "
                + "TIMESTAMPDIFF(HOUR, a.reported_at, NOW()) AS hrs "
                + "FROM adverse_events a "
                + "JOIN patients p ON p.patient_id = a.patient_id "
                + "JOIN trials t ON t.trial_id = a.trial_id "
                 + "WHERE a.is_sae = 1 AND a.status = 'Open' "
                + "ORDER BY a.reported_at");
        for (Map<String, Object> r : severe) {
            long hrs = ((Number) r.get("hrs")).longValue();
            String text = "AE #" + r.get("ae_id") + " for " + r.get("patient_name")
                    + " (" + r.get("trial_name") + ") is an SAE and still awaiting review";
            if (hrs >= 24) {
                text += " for " + hrs + " hours - 24-hour review deadline exceeded.";
                alerts.add(alert("alert-danger", "SAE 24-Hour Review Overdue", text));
            } else {
                text += " - immediate review required.";
                alerts.add(alert("alert-danger", "Immediate Review Required", text));
            }
        }

        // 2) Scheduled visits more than three days overdue.
        List<Map<String, Object>> overdueVisits = DBUtil.query(c,
                "SELECT v.visit_name, v.due_date, p.name AS patient_name, t.trial_name, "
                + "DATEDIFF(CURDATE(), v.due_date) AS days_late "
                + "FROM visits v JOIN patients p ON p.patient_id = v.patient_id "
                + "JOIN trials t ON t.trial_id = p.enrolled_trial "
                + "WHERE v.status = 'Scheduled' AND v.due_date < DATE_SUB(CURDATE(), INTERVAL 3 DAY) "
                + "ORDER BY v.due_date");
        // One grouped alert instead of one red row per visit (keeps the dashboard readable).
        if (!overdueVisits.isEmpty()) {
            Map<String, Object> oldest = overdueVisits.get(0);   // list is ordered by due_date
            int n = overdueVisits.size();
            alerts.add(alert("alert-warning", "Missed Visit Risk",
                    n + " scheduled visit" + (n == 1 ? " is" : "s are") + " overdue. Oldest: "
                    + oldest.get("visit_name") + " for " + oldest.get("patient_name")
                    + " (" + oldest.get("days_late") + " days). Open the Visits page to review."));
        }

        // A visit explicitly recorded as missed remains a protocol-deviation risk.
        List<Map<String, Object>> missedVisits = DBUtil.query(c,
                "SELECT v.visit_name, v.due_date, p.name AS patient_name, t.trial_name "
                + "FROM visits v JOIN patients p ON p.patient_id = v.patient_id "
                + "JOIN trials t ON t.trial_id = p.enrolled_trial "
                + "WHERE v.status = 'Missed' ORDER BY v.due_date");
        for (Map<String, Object> r : missedVisits) {
            alerts.add(alert("alert-danger", "Missed Visit / Protocol Deviation",
                    r.get("visit_name") + " for " + r.get("patient_name") + " (" + r.get("trial_name")
                    + ") was recorded as missed."));
        }

        // 3) Active trials ending within 7 days (yellow)
        List<Map<String, Object>> ending = DBUtil.query(c,
                "SELECT trial_name, end_date, DATEDIFF(end_date, CURDATE()) AS days_left "
                + "FROM trials WHERE status = 'Active' "
                + "AND end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 7 DAY) "
                + "ORDER BY end_date");
        for (Map<String, Object> r : ending) {
            alerts.add(alert("alert-warning", "Trial Ending Soon",
                    r.get("trial_name") + " ends in " + r.get("days_left")
                    + " day(s) (" + r.get("end_date") + ")."));
        }

        // 4) Blocked enrollment attempts in the last 7 days (orange / blue)
        List<Map<String, Object>> blocked = DBUtil.query(c,
                "SELECT action_type, COUNT(*) AS cnt FROM audit_logs "
                + "WHERE action_type IN ('CONSENT_VIOLATION','SUSPENDED_TRIAL_ACCESS','DUPLICATE_ENROLLMENT') "
                + "AND `timestamp` >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY action_type");
        for (Map<String, Object> r : blocked) {
            String type = (String) r.get("action_type");
            String text = r.get("cnt") + " attempt(s) blocked in the last 7 days.";
            if ("CONSENT_VIOLATION".equals(type)) {
                alerts.add(alert("alert-orange", "Consent Violation Attempted", text));
            } else if ("SUSPENDED_TRIAL_ACCESS".equals(type)) {
                alerts.add(alert("alert-orange", "Suspended Trial Access Attempted", text));
            } else {
                alerts.add(alert("alert-info", "Duplicate Enrollment Attempted", text));
            }
        }
        request.setAttribute("alerts", alerts);

        request.setAttribute("trials", DBUtil.query(c,
                 "SELECT t.trial_id, t.trial_name, t.drug_name, t.phase, t.end_date, t.status, t.target_enrollment, "
                 + "(SELECT COUNT(*) FROM randomization_allocations ra WHERE ra.trial_id = t.trial_id) + "
                + "(SELECT COUNT(*) FROM patients p WHERE p.enrolled_trial = t.trial_id AND p.randomization_no IS NULL) AS enrolled "
                + "FROM trials t ORDER BY t.trial_id"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trialRows = (List<Map<String, Object>>) request.getAttribute("trials");
        for (Map<String, Object> row : trialRows) {
            long enrolled = ((Number) row.get("enrolled")).longValue();
            long target = ((Number) row.get("target_enrollment")).longValue();
            row.put("pct", Math.min(100L, enrolled * 100L / Math.max(1L, target)));
        }
        List<Map<String, Object>> siteProgress = DBUtil.query(c,
                "SELECT t.trial_name, s.site_code, ts.site_target, "
                +  "(SELECT COUNT(*) FROM randomization_allocations ra WHERE ra.trial_id = ts.trial_id AND ra.site_id = ts.site_id) + "
                + "(SELECT COUNT(*) FROM patients p WHERE p.enrolled_trial = ts.trial_id AND p.site_id = ts.site_id AND p.randomization_no IS NULL) AS enrolled "
                + "FROM trial_sites ts JOIN trials t ON t.trial_id = ts.trial_id "
                + "JOIN sites s ON s.site_id = ts.site_id ORDER BY t.trial_id, s.site_code");
        for (Map<String, Object> row : siteProgress) {
            long enrolled = ((Number) row.get("enrolled")).longValue();
            long target = ((Number) row.get("site_target")).longValue();
            row.put("pct", Math.min(100L, enrolled * 100L / Math.max(1L, target)));
        }
        request.setAttribute("siteProgress", siteProgress);
        request.setAttribute("overdueVisits", DBUtil.count(c,
                "SELECT COUNT(*) FROM visits WHERE status = 'Scheduled' AND due_date < DATE_SUB(CURDATE(), INTERVAL 3 DAY)"));
        request.setAttribute("openSae", DBUtil.count(c,
                "SELECT COUNT(*) FROM adverse_events WHERE is_sae = 1 AND status = 'Open'"));
        request.setAttribute("missedVisits", DBUtil.count(c,
                "SELECT COUNT(*) FROM visits WHERE status = 'Missed'"));

        request.setAttribute("recentLogs", DBUtil.query(c,
                "SELECT l.action_type, l.description, l.`timestamp` AS log_time, u.username "
                + "FROM audit_logs l JOIN users u ON u.user_id = l.user_id "
                + "ORDER BY l.log_id DESC LIMIT 6"));

        long mild = 0, moderate = 0, severeCount = 0;
        for (Map<String, Object> r : DBUtil.query(c,
                "SELECT severity, COUNT(*) AS cnt FROM adverse_events GROUP BY severity")) {
            long n = ((Number) r.get("cnt")).longValue();
            String s = (String) r.get("severity");
            if ("Mild".equals(s)) {
                mild = n;
            } else if ("Moderate".equals(s)) {
                moderate = n;
            } else {
                severeCount = n;
            }
        }
        request.setAttribute("sevMild", mild);
        request.setAttribute("sevModerate", moderate);
        request.setAttribute("sevSevere", severeCount);
    }

    private void loadStaff(Connection c, HttpServletRequest request) throws SQLException {
        int uid = WebUtil.userId(request);
        request.setAttribute("totalPatients", DBUtil.count(c, "SELECT COUNT(*) FROM patients"));
        request.setAttribute("enrolledPatients",
                DBUtil.count(c, "SELECT COUNT(*) FROM patients WHERE enrolled_trial IS NOT NULL"));
        request.setAttribute("pendingConsent",
                DBUtil.count(c, "SELECT COUNT(*) FROM patients WHERE consent_status = 'Pending'"));
        request.setAttribute("myAEs",
                DBUtil.count(c, "SELECT COUNT(*) FROM adverse_events WHERE reported_by = ?", uid));
        request.setAttribute("recentPatients", DBUtil.query(c,
                "SELECT p.patient_id, p.name, p.age, p.gender, p.consent_status, t.trial_name "
                + "FROM patients p LEFT JOIN trials t ON t.trial_id = p.enrolled_trial "
                + "ORDER BY p.patient_id DESC LIMIT 5"));
    }

    private Map<String, Object> alert(String css, String title, String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("css", css);
        m.put("title", title);
        m.put("text", text);
        return m;
    }
}
