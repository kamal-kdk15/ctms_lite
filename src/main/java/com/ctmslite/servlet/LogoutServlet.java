package com.ctmslite.servlet;

import com.ctmslite.util.AuditLogger;
import com.ctmslite.util.DBConnection;
import com.ctmslite.util.WebUtil;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        int userId = WebUtil.userId(request);
        if (userId > 0) {
            try (Connection c = DBConnection.getConnection()) {
                AuditLogger.log(c, userId, "LOGOUT", "users", "User logged out.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        WebUtil.redirect(request, response, "/login.jsp?msg=loggedout");
    }
}
