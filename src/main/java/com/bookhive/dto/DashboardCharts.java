package com.bookhive.dto;

import java.util.List;

/**
 * Data for the Chart.js dashboard (GET /api/stats/charts).
 * Labels are codes (copy status, yyyy-MM); the page translates them for display.
 */
public record DashboardCharts(
    List<StatusCount> copyStatus,
    List<MonthCount> loansPerMonth,
    List<BookCount> topBooks
) {
    public record StatusCount(String status, long count) {}
    public record MonthCount(String month, long count) {}
    public record BookCount(Long bookId, String title, long loans) {}
}
