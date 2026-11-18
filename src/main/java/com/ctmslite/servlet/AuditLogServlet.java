package com.ctmslite.servlet;

import com.ctmslite.util.DBConnection;
import com.ctmslite.util.DBUtil;
import com.ctmslite.util.WebUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Module 6.6 - Audit log viewer with filters by date range, user and action type (Admin). */
@WebServlet("/auditLog")
public class AuditLogServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String from = WebUtil.trim(request.getParameter("from"));
        String to = WebUtil.trim(request.getParameter("to"));
        Integer userFilter = WebUtil.parseInt(request.getParameter("user"));
        String actionFilter = WebUtil.trim(request.getParameter("action"));

        StringBuilder sql = new StringBuilder(
                "SELECT l.log_id, l.action_type, l.affected_table, l.description, "
                + "l.`timestamp` AS log_time, u.username, u.role "
                + "FROM audit_logs l JOIN users u ON u.user_id = l.user_id WHERE 1=1");
        List<Object> params = new ArrayList<>();

        try {
            if (!WebUtil.isBlank(from)) {
                sql.append(" AND DATE(l.`timestamp`) >= ?");
                params.add(Date.valueOf(from));
            }
            if (!WebUtil.isBlank(to)) {
                sql.append(" AND DATE(l.`timestamp`) <= ?");
                params.add(Date.valueOf(to));
            }
        } catch (IllegalArgumentException e) {
            WebUtil.flash(request, "danger", "Invalid date in filter.");
            WebUtil.redirect(request, response, "/auditLog");
            return;
        }
        if (userFilter != null) {
            sql.append(" AND l.user_id = ?");
            params.add(userFilter);
        }
        if (!WebUtil.isBlank(actionFilter)) {
            sql.append(" AND l.action_type = ?");
            params.add(actionFilter);
        }
        sql.append(" ORDER BY l.log_id DESC LIMIT 200");

        try (Connection c = DBConnection.getConnection()) {
            request.setAttribute("logs", DBUtil.query(c, sql.toString(), params.toArray()));
            request.setAttribute("users", DBUtil.query(c,
                    "SELECT user_id, username FROM users ORDER BY username"));
            request.setAttribute("signatures", DBUtil.query(c,
                    "SELECT g.sign_id, g.record_type, g.record_id, g.meaning, g.record_state, g.signed_at, "
                    + "LEFT(g.signature_hash, 20) AS hash_short, u.username "
                    + "FROM e_signatures g JOIN users u ON u.user_id=g.user_id "
                    + "ORDER BY g.sign_id DESC LIMIT 20"));
            request.setAttribute("actions", DBUtil.query(c,
                    "SELECT DISTINCT action_type FROM audit_logs ORDER BY action_type"));
            request.setAttribute("selFrom", from);
            request.setAttribute("selTo", to);
            request.setAttribute("selUser", userFilter);
            request.setAttribute("selAction", actionFilter);
            request.getRequestDispatcher("/WEB-INF/views/auditLog.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
