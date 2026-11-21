package com.bookhive.controller;

import com.bookhive.dto.DashboardStats;
import com.bookhive.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboard;

    @GetMapping
    public DashboardStats stats() {
        return dashboard.getStats();
    }

    /** Copy status breakdown, loans per month and most borrowed titles for Chart.js. */
    @GetMapping("/charts")
    public com.bookhive.dto.DashboardCharts charts() {
        return dashboard.getCharts();
    }
}