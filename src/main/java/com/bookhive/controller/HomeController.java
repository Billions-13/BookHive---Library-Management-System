package com.bookhive.controller;

import com.bookhive.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {
    private final BookService books;

    @GetMapping("/")
    public String index(Model model) {
        // Newest titles for the "Sách mới cập nhật" section of the home page.
        model.addAttribute("latestBooks", books
            .search("", "", "", "", "", "", null, 0, 10, "id", "desc")
            .getContent().stream().limit(8).toList());
        return "index";
    }
}
