package com.bookhive.dto;

public record BookRow(Long id, String isbn, String title, String authorNames,
                      String publisher, String language, int publicationYear,
                      long availableCopies) {}
