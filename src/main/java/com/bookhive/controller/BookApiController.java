package com.bookhive.controller;

import com.bookhive.dto.BookDetails;
import com.bookhive.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Read-only JSON used by the book detail page to refresh its stock counters. */
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookApiController {
    private final BookService books;

    @GetMapping("/{id}/availability")
    public Map<String, Object> availability(@PathVariable Long id) {
        BookDetails b = books.details(id);
        return Map.of(
            "bookId", b.id(),
            "totalCopies", b.totalCopies(),
            "availableCopies", b.availableCopies(),
            "borrowedCopies", b.borrowedCopies(),
            "damagedCopies", b.damagedCopies()
        );
    }
}
