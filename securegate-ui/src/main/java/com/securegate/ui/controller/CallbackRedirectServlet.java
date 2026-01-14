package com.securegate.ui.controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/callback")
public class CallbackRedirectServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String query = req.getQueryString();
        String target = "callback.xhtml";
        if (query != null && !query.isEmpty()) {
            target += "?" + query;
        }
        resp.sendRedirect(target); // 302 Redirect
    }
}
