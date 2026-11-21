package com.bookhive.controller;

import com.bookhive.repository.UserRepository;
import com.bookhive.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String borrow(@PathVariable Long bookId, RedirectAttributes ra) {
        var reader = current.requireUser();
        loans.borrow(bookId, reader.getId(), reader);
        ra.addFlashAttribute("success", "Mượn sách thành công. Vui lòng nhận sách tại quầy và trả đúng hạn.");
        return "redirect:/my/loans";
    }
    @GetMapping("/librarian/loans")
    public String manage(Model m) {
        m.addAttribute("loans", loans.all());
        m.addAttribute("users", users.findAll());
        return "librarian/loans";
    }
    @PostMapping("/librarian/loans")
    public String issue(@RequestParam Long bookId, @RequestParam Long userId, RedirectAttributes ra) {
        Long loanId = loans.borrow(bookId, userId, current.requireUser());
        ra.addFlashAttribute("success", "Đã lập phiếu mượn #" + loanId + ".");
        return "redirect:/librarian/loans";
    }
    @PostMapping("/librarian/loans/{id}/return")
    public String acceptReturn(@PathVariable Long id, RedirectAttributes ra) {
        loans.returnCopy(id, current.requireUser());
        ra.addFlashAttribute("success", "Đã nhận trả sách cho phiếu #" + id + ". Nếu trả trễ, khoản phạt đã được tạo tự động.");
        return "redirect:/librarian/loans";
    }
}
