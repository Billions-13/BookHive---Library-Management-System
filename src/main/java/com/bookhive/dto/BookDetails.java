package com.bookhive.dto;

import java.util.List;

public record BookDetails(Long id, String isbn, String title, String edition, String publisher,
        Integer publicationYear, String language, String description, String coverUrl,
        List<String> authorNames, List<String> categoryNames,
        long totalCopies, long availableCopies, long borrowedCopies, long damagedCopies) {}
