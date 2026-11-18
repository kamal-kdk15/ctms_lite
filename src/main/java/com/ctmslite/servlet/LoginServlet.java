package com.ctmslite.servlet;

import com.ctmslite.util.AuditLogger;
import com.ctmslite.util.DBConnection;
import com.ctmslite.util.DBUtil;
import com.ctmslite.util.PasswordUtil;
import com.ctmslite.util.WebUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Module 6.1 - Login and role-based redirection. */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        WebUtil.redirect(request, response, "/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = WebUtil.trim(request.getParameter("username"));
        String password = request.getParameter("password");

        if (WebUtil.isBlank(username) || WebUtil.isBlank(password)) {
            WebUtil.redirect(request, response, "/login.jsp?error=empty");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            Map<String, Object> user = DBUtil.queryOne(conn,
                    "SELECT user_id, username, password, role, failed_attempts, locked_until FROM users WHERE username = ?",
                    username);

            if (user != null && user.get("locked_until") != null) {
                Map<String, Object> lock = DBUtil.queryOne(conn,
                        "SELECT (locked_until > NOW()) AS active FROM users WHERE user_id = ?",
                        ((Number) user.get("user_id")).intValue());
                if (lock != null && ((Number) lock.get("active")).intValue() == 1) {
                    WebUtil.redirect(request, response, "/login.jsp?error=locked");
                    return;
                }
            }

            String hash = PasswordUtil.sha256(password);

            if (user != null && hash.equals(user.get("password"))) {
                DBUtil.update(conn, "UPDATE users SET failed_attempts = 0, locked_until = NULL WHERE user_id = ?",
                        ((Number) user.get("user_id")).intValue());
                // new session on every login (prevents session fixation)
                HttpSession old = request.getSession(false);
                if (old != null) {
                    old.invalidate();
                }
                HttpSession session = request.getSession(true);
                int userId = ((Number) user.get("user_id")).intValue();
                String role = (String) user.get("role");

                session.setAttribute("user_id", userId);
                session.setAttribute("username", username);
                session.setAttribute("role", role);
                session.setMaxInactiveInterval(15 * 60);

                AuditLogger.log(conn, userId, "LOGIN", "users",
                        "User " + username + " (" + role + ") logged in successfully.");

                if ("Admin".equals(role)) {
                    WebUtil.redirect(request, response, "/adminDashboard");
                } else {
                    WebUtil.redirect(request, response, "/staffDashboard");
                }
            } else {
                if (user != null) {
                    int userId = ((Number) user.get("user_id")).intValue();
                    DBUtil.update(conn,
                            "UPDATE users SET failed_attempts = failed_attempts + 1, "
                            + "locked_until = CASE WHEN failed_attempts + 1 >= 3 "
                            + "THEN DATE_ADD(NOW(), INTERVAL 15 MINUTE) ELSE locked_until END "
                            + "WHERE user_id = ?", userId);
                    AuditLogger.log(conn, userId, "LOGIN_FAILED", "users",
                            "Failed login attempt for user " + username + ".");
                    Map<String, Object> attempts = DBUtil.queryOne(conn,
                            "SELECT failed_attempts FROM users WHERE user_id = ?", userId);
                    if (attempts != null && ((Number) attempts.get("failed_attempts")).intValue() >= 3) {
                        AuditLogger.log(conn, userId, "ACCOUNT_LOCKED", "users",
                                "Account " + username + " locked for 15 minutes after three failed logins.");
                    }
                }
                WebUtil.redirect(request, response, "/login.jsp?error=1");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            WebUtil.redirect(request, response, "/login.jsp?error=db");
        }
    }
}
