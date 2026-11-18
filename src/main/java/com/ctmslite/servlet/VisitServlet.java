package com.ctmslite.servlet;

import com.ctmslite.util.AuditLogger;
import com.ctmslite.util.DBConnection;
import com.ctmslite.util.DBUtil;
import com.ctmslite.util.WebUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Patient visit calendar with completion and missed-visit tracking. */
@WebServlet("/visits")
public class VisitServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String filter = req.getParameter("filter");
        String condition = "";
        if ("overdue".equals(filter)) condition = " AND v.status='Scheduled' AND v.due_date<CURDATE()";
        else if ("upcoming".equals(filter)) condition = " AND v.status='Scheduled' AND v.due_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(),INTERVAL 30 DAY)";
        else if ("completed".equals(filter)) condition = " AND v.status='Completed'";
        else if ("missed".equals(filter)) condition = " AND v.status='Missed'";
        else filter = "all";
        try (Connection c = DBConnection.getConnection()) {
            req.setAttribute("visits", DBUtil.query(c,
                    "SELECT v.visit_id,v.visit_name,v.due_date,v.actual_date,v.status,v.note, "
                    + "p.patient_id,p.name patient_name,t.trial_name,s.site_code, "
                    + "DATEDIFF(CURDATE(),v.due_date) days_late "
                    + "FROM visits v JOIN patients p ON p.patient_id=v.patient_id "
                    + "JOIN trials t ON t.trial_id=p.enrolled_trial LEFT JOIN sites s ON s.site_id=p.site_id "
                    + "WHERE 1=1 " + condition + " ORDER BY v.due_date,v.visit_id"));
            req.setAttribute("overdueCount", DBUtil.count(c,
                    "SELECT COUNT(*) FROM visits WHERE status='Scheduled' AND due_date<CURDATE()"));
            req.setAttribute("upcomingCount", DBUtil.count(c,
                    "SELECT COUNT(*) FROM visits WHERE status='Scheduled' AND due_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(),INTERVAL 30 DAY)"));
            req.setAttribute("completedCount", DBUtil.count(c, "SELECT COUNT(*) FROM visits WHERE status='Completed'"));
            req.setAttribute("missedCount", DBUtil.count(c, "SELECT COUNT(*) FROM visits WHERE status='Missed'"));
            req.setAttribute("filter", filter);
            req.getRequestDispatcher("/WEB-INF/views/visits.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Integer id = WebUtil.parseInt(req.getParameter("visit_id"));
        String action = req.getParameter("action");
        String reason = WebUtil.trim(req.getParameter("reason"));
        int uid = WebUtil.userId(req);
        if (!"Staff".equals(req.getSession().getAttribute("role"))) {
            WebUtil.flash(req, "danger", "Only Staff can record visit outcomes.");
            WebUtil.redirect(req, resp, "/visits");
            return;
        }
        if (id == null || WebUtil.isBlank(reason) || reason.length() > 255) {
            WebUtil.flash(req, "danger", "Visit and reason for the record are required.");
            WebUtil.redirect(req, resp, "/visits");
            return;
        }
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Map<String,Object> visit = DBUtil.queryOne(c,
                        "SELECT visit_id,patient_id,visit_name,status FROM visits WHERE visit_id=? FOR UPDATE", id);
                if (visit == null || !"Scheduled".equals(visit.get("status"))) {
                    WebUtil.flash(req, "warning", "Visit not found or already closed.");
                } else if ("complete".equals(action)) {
                    LocalDate actual = LocalDate.parse(req.getParameter("actual_date"));
                    if (actual.isAfter(LocalDate.now())) {
                        WebUtil.flash(req, "danger", "Actual visit date cannot be in the future.");
                    } else {
                        DBUtil.update(c, "UPDATE visits SET status='Completed',actual_date=?,note=NULL WHERE visit_id=?",
                                Date.valueOf(actual), id);
                        AuditLogger.log(c,uid,"VISIT_COMPLETED","visits",
                                visit.get("visit_name")+" for Patient ID "+visit.get("patient_id")+" completed on "+actual+". Reason: "+reason);
                        WebUtil.flash(req,"success","Visit marked completed.");
                    }
                } else if ("missed".equals(action)) {
                    DBUtil.update(c, "UPDATE visits SET status='Missed',note=? WHERE visit_id=?", reason,id);
                    AuditLogger.log(c,uid,"VISIT_MISSED","visits",
                            visit.get("visit_name")+" for Patient ID "+visit.get("patient_id")+" marked missed. Reason: "+reason);
                    WebUtil.flash(req,"warning","Visit marked missed and deviation recorded.");
                } else {
                    WebUtil.flash(req, "danger", "Unknown visit action.");
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            WebUtil.flash(req,"danger","Unable to update visit. Check the date and try again.");
        }
        WebUtil.redirect(req,resp,"/visits");
    }
}
