package com.ctmslite.util;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Small helpers shared by all servlets. */
public class WebUtil {

    /** Logged-in user's id, or -1 when nobody is logged in. */
    public static int userId(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null || s.getAttribute("user_id") == null) {
            return -1;
        }
        return (Integer) s.getAttribute("user_id");
    }

    /** One-time message shown at the top of the next page (type = success | danger | warning). */
    public static void flash(HttpServletRequest req, String type, String message) {
        HttpSession s = req.getSession();
        s.setAttribute("flash", message);
        s.setAttribute("flashType", type);
    }

    public static void redirect(HttpServletRequest req, HttpServletResponse resp, String path)
            throws IOException {
        resp.sendRedirect(req.getContextPath() + path);
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String trim(String s) {
        return s == null ? null : s.trim();
    }

    /** Parses an int; returns null for null / empty / invalid input. */
    public static Integer parseInt(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
