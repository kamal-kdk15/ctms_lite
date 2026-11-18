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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Module 6.2 - Trial registration and status management (Admin only). */
@WebServlet("/trials")
public class TrialServlet extends HttpServlet {

    private static final List<String> PHASES = Arrays.asList("I", "II", "III", "IV");
    private static final List<String> STATUSES =
            Arrays.asList("Active", "Pending", "Suspended", "Completed");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try (Connection c = DBConnection.getConnection()) {
            request.setAttribute("trials", DBUtil.query(c,
                    "SELECT t.trial_id, t.trial_name, t.drug_name, t.phase, t.start_date, "
                    + "t.end_date, t.status, t.target_enrollment, "
                    + "(SELECT COUNT(*) FROM randomization_allocations ra WHERE ra.trial_id = t.trial_id) + "
                    + "(SELECT COUNT(*) FROM patients p WHERE p.enrolled_trial = t.trial_id AND p.randomization_no IS NULL) AS enrolled "
                    + "FROM trials t ORDER BY t.trial_id"));
            request.getRequestDispatcher("/WEB-INF/views/trials.jsp").forward(request, response);
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
            c.setAutoCommit(false);
            try {
                if ("create".equals(action)) {
                    createTrial(c, request, uid);
                } else if ("updateStatus".equals(action)) {
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
        WebUtil.redirect(request, response, "/trials");
    }

    private void createTrial(Connection c, HttpServletRequest req, int uid) throws SQLException {
        String name = WebUtil.trim(req.getParameter("trial_name"));
        String drug = WebUtil.trim(req.getParameter("drug_name"));
        String phase = req.getParameter("phase");
        String status = req.getParameter("status");
        String sd = req.getParameter("start_date");
        String ed = req.getParameter("end_date");
        Integer targetEnrollment = WebUtil.parseInt(req.getParameter("target_enrollment"));

        if (targetEnrollment == null || targetEnrollment < 1 || targetEnrollment > 100000) {
            WebUtil.flash(req, "danger", "Enrollment target must be between 1 and 100000.");
            return;
        }
        if (WebUtil.isBlank(name) || WebUtil.isBlank(drug) || WebUtil.isBlank(sd)
                || WebUtil.isBlank(ed) || !PHASES.contains(phase) || !STATUSES.contains(status)) {
            WebUtil.flash(req, "danger", "All fields are required and must be valid.");
            return;
        }
        if (name.length() > 100 || drug.length() > 100) {
            WebUtil.flash(req, "danger", "Trial name and drug name can be at most 100 characters.");
            return;
        }
        Date start;
        Date end;
        try {
            start = Date.valueOf(sd);
            end = Date.valueOf(ed);
        } catch (IllegalArgumentException e) {
            WebUtil.flash(req, "danger", "Please enter valid dates.");
            return;
        }
        if (end.before(start)) {
            WebUtil.flash(req, "danger", "End date cannot be before start date.");
            return;
        }

        DBUtil.update(c,
                "INSERT INTO trials (trial_name, drug_name, phase, start_date, end_date, status, created_by, target_enrollment) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                name, drug, phase, start, end, status, uid, targetEnrollment);
        AuditLogger.log(c, uid, "TRIAL_CREATED", "trials",
                "Trial \"" + name + "\" (Phase " + phase + ", " + drug + ") registered with status " + status + ".");
        WebUtil.flash(req, "success", "Trial \"" + name + "\" registered successfully.");
    }

    private void updateStatus(Connection c, HttpServletRequest req, int uid) throws SQLException {
        Integer trialId = WebUtil.parseInt(req.getParameter("trial_id"));
        String status = req.getParameter("status");
        if (trialId == null || !STATUSES.contains(status)) {
            WebUtil.flash(req, "danger", "Invalid trial or status.");
            return;
        }
        Map<String, Object> t = DBUtil.queryOne(c,
                "SELECT trial_name, status FROM trials WHERE trial_id = ?", trialId);
        if (t == null) {
            WebUtil.flash(req, "danger", "Trial not found.");
            return;
        }
        String oldStatus = (String) t.get("status");
        if (oldStatus.equals(status)) {
            WebUtil.flash(req, "warning", "Trial is already " + status + ".");
            return;
        }
        String sigError = ESign.apply(c, req, uid, "TRIAL_STATUS", trialId,
                "status=" + oldStatus + ">" + status);
        if (sigError != null) {
            WebUtil.flash(req, "danger", sigError);
            return;
        }
        DBUtil.update(c, "UPDATE trials SET status = ? WHERE trial_id = ?", status, trialId);
        AuditLogger.log(c, uid, "TRIAL_STATUS_UPDATE", "trials",
                "Trial ID " + trialId + " status changed from " + oldStatus + " to " + status
                + " [e-signature: " + req.getAttribute("sigMeaning") + "].");
        WebUtil.flash(req, "success", "Trial status updated with electronic signature (" + req.getAttribute("sigMeaning") + ").");
    }
}
