package com.bookhive.controller;

import com.bookhive.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class BookController {
    private final BookService service;

    @GetMapping("/books")
    public String list(@RequestParam(defaultValue = "") String title,
            @RequestParam(defaultValue = "") String isbn,
            @RequestParam(defaultValue = "") String author,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String publisher,
            @RequestParam(defaultValue = "") String language,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "title") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Model model) {
        model.addAttribute("books", service.search(title, isbn, author, category, publisher,
                language, year, page, size, sort, direction));
        model.addAttribute("title", title);
        model.addAttribute("isbn", isbn);
        model.addAttribute("author", author);
        model.addAttribute("category", category);
        model.addAttribute("publisher", publisher);
        model.addAttribute("language", language);
        model.addAttribute("year", year);
        model.addAttribute("size", size);
        model.addAttribute("sort", sort);
        model.addAttribute("direction", direction);
        return "books/list";
    }

    @GetMapping("/books/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("book", service.details(id));
        return "books/detail";
    }
}
