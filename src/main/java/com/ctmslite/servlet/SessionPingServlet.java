package com.ctmslite.servlet;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Lightweight authenticated request to keep an actively used session alive. */
@WebServlet("/session-ping")
public class SessionPingServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        resp.setHeader("Cache-Control", "no-store");
    }
}
