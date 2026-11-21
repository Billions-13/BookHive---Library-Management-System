package com.bookhive.controller;

import com.bookhive.repository.UserRepository;
import com.bookhive.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class LoanController {
    private final LoanService loans;
    private final CurrentUserService current;
    private final UserRepository users;

    @GetMapping("/my/loans")
    public String myLoans(Model m) {
        m.addAttribute("loans", loans.forUser(current.requireUser().getId()));
        return "my/loans";
    }
    @PostMapping("/my/borrow/{bookId}")
    public String borrow(@PathVariable Long bookId) {
        var reader = current.requireUser();
        loans.borrow(bookId, reader.getId(), reader);
        return "redirect:/my/loans";
    }
    @GetMapping("/librarian/loans")
    public String manage(Model m) {
        m.addAttribute("loans", loans.all());
        m.addAttribute("users", users.findAll());
        return "librarian/loans";
    }
    @PostMapping("/librarian/loans")
    public String issue(@RequestParam Long bookId, @RequestParam Long userId) {
        loans.borrow(bookId, userId, current.requireUser());
        return "redirect:/librarian/loans";
    }
    @PostMapping("/librarian/loans/{id}/return")
    public String acceptReturn(@PathVariable Long id) {
        loans.returnCopy(id, current.requireUser());
        return "redirect:/librarian/loans";
    }
}
