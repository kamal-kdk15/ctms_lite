package com.ctmslite.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Session + role check for every request.
 *  - Not logged in            -> redirect to login page
 *  - Wrong role for the URL   -> redirect to login page
 * This is the "role-based access enforced at the Servlet layer" requirement.
 */
@WebFilter("/*")
public class AuthFilter implements Filter {

    private static final Set<String> ADMIN_ONLY = new HashSet<>(
            Arrays.asList("/adminDashboard", "/trials", "/aeReview", "/auditLog", "/sites", "/users", "/export"));
    private static final Set<String> STAFF_ONLY = new HashSet<>(
            Arrays.asList("/staffDashboard", "/patients", "/enrollPatient", "/aeReport"));

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        request.setCharacterEncoding("UTF-8");

        String path = request.getServletPath();
        if (isPublic(path)) {
            chain.doFilter(req, res);
            return;
        }

        HttpSession session = request.getSession(false);
        String role = (session == null) ? null : (String) session.getAttribute("role");

        if (role == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=session");
            return;
        }
        if ((ADMIN_ONLY.contains(path) && !"Admin".equals(role))
                || (STAFF_ONLY.contains(path) && !"Staff".equals(role))) {
            response.sendRedirect(request.getContextPath() + "/login.jsp?error=denied");
            return;
        }

        // stop the browser "Back" button from showing protected pages after logout
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        chain.doFilter(req, res);
    }

    private boolean isPublic(String path) {
        return path == null || path.isEmpty() || path.equals("/")
                || path.equals("/login") || path.equals("/login.jsp")
                || path.startsWith("/css/");
    }
}
