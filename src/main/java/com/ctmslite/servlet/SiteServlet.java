package com.ctmslite.servlet;

import com.ctmslite.util.AuditLogger;
import com.ctmslite.util.DBConnection;
import com.ctmslite.util.DBUtil;
import com.ctmslite.util.WebUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Admin management of investigative sites and trial-site enrollment targets. */
@WebServlet("/sites")
public class SiteServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection c = DBConnection.getConnection()) {
            req.setAttribute("sites", DBUtil.query(c,
                    "SELECT s.site_id, s.site_code, s.institution, s.location, s.principal_investigator, s.status, "
                    + "COUNT(DISTINCT ts.trial_site_id) AS trial_count "
                    + "FROM sites s LEFT JOIN trial_sites ts ON ts.site_id = s.site_id "
                    + "GROUP BY s.site_id ORDER BY s.site_code"));
            req.setAttribute("trials", DBUtil.query(c,
                    "SELECT trial_id, trial_name, target_enrollment FROM trials ORDER BY trial_name"));
            java.util.List<java.util.Map<String,Object>> assignments = DBUtil.query(c,
                    "SELECT ts.trial_site_id, t.trial_name, s.site_code, ts.site_target, "
                    +  "(SELECT COUNT(*) FROM randomization_allocations ra WHERE ra.trial_id=ts.trial_id AND ra.site_id=ts.site_id) + "
                    + "(SELECT COUNT(*) FROM patients p WHERE p.enrolled_trial=ts.trial_id AND p.site_id=ts.site_id AND p.randomization_no IS NULL) enrolled "
                    + "FROM trial_sites ts JOIN trials t ON t.trial_id=ts.trial_id "
                    + "JOIN sites s ON s.site_id=ts.site_id ORDER BY t.trial_name, s.site_code");
            for (java.util.Map<String,Object> row : assignments) {
                long actual = ((Number) row.get("enrolled")).longValue();
                long target = ((Number) row.get("site_target")).longValue();
                row.put("pct", Math.min(100L, actual * 100L / Math.max(1L, target)));
            }
            req.setAttribute("assignments", assignments);
            req.getRequestDispatcher("/WEB-INF/views/sites.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int uid = WebUtil.userId(req);
        String action = req.getParameter("action");
        try (Connection c = DBConnection.getConnection()) {
            if ("create".equals(action)) {
                String code = WebUtil.trim(req.getParameter("site_code"));
                String institution = WebUtil.trim(req.getParameter("institution"));
                String location = WebUtil.trim(req.getParameter("location"));
                String pi = WebUtil.trim(req.getParameter("principal_investigator"));
                if (WebUtil.isBlank(code) || WebUtil.isBlank(institution) || WebUtil.isBlank(location)
                        || WebUtil.isBlank(pi) || code.length() > 30 || institution.length() > 150
                        || location.length() > 150 || pi.length() > 120) {
                    WebUtil.flash(req, "danger", "Complete all site fields using the allowed lengths.");
                } else {
                    DBUtil.update(c, "INSERT INTO sites (site_code,institution,location,principal_investigator) VALUES (?,?,?,?)",
                            code, institution, location, pi);
                    AuditLogger.log(c, uid, "SITE_CREATED", "sites", "Investigative site " + code + " registered.");
                    WebUtil.flash(req, "success", "Site " + code + " created.");
                }
            } else if ("assign".equals(action)) {
                Integer trial = WebUtil.parseInt(req.getParameter("trial_id"));
                Integer site = WebUtil.parseInt(req.getParameter("site_id"));
                Integer target = WebUtil.parseInt(req.getParameter("site_target"));
                if (trial == null || site == null || target == null || target < 1 || target > 100000) {
                    WebUtil.flash(req, "danger", "Select a trial and site and enter a valid site target.");
                } else {
                    DBUtil.update(c,
                            "INSERT INTO trial_sites (trial_id,site_id,site_target) VALUES (?,?,?) "
                            + "ON DUPLICATE KEY UPDATE site_target=VALUES(site_target)",
                            trial, site, target);
                    AuditLogger.log(c, uid, "TRIAL_SITE_CONFIGURED", "trial_sites",
                            "Site " + site + " configured for Trial " + trial + " with target " + target + ".");
                    WebUtil.flash(req, "success", "Trial-site assignment saved.");
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                WebUtil.flash(req, "danger", "That site code already exists.");
            } else {
                e.printStackTrace();
                WebUtil.flash(req, "danger", "Database error. No site change was saved.");
            }
        }
        WebUtil.redirect(req, resp, "/sites");
    }
}
