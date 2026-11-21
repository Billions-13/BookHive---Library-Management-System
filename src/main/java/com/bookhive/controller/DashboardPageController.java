package com.bookhive.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Statistics page; the numbers and charts are loaded by /js/dashboard.js from /api/stats. */
@Controller
public class DashboardPageController {

    @GetMapping("/librarian/dashboard")
    public String dashboard() {
        return "librarian/dashboard";
    }
}
