package com.bookhive.dto;

public record DashboardStats(
    long totalTitles,
    long availableCopies,
    long borrowedCopies,
    long overdueLoans
) {}