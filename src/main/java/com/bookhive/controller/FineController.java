package com.bookhive.controller;

import com.bookhive.enums.FineStatus;
import com.bookhive.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class FineController {
    private final FineService fines;
    private final CurrentUserService current;

    @GetMapping("/my/fines")
    public String mine(Model m) {
        m.addAttribute("fines", fines.forUser(current.requireUser().getId()));
        return "my/fines";
    }
    @GetMapping("/librarian/fines")
    public String all(Model m) { m.addAttribute("fines", fines.all()); return "librarian/fines"; }
    @PostMapping("/librarian/fines/{id}/resolve")
    public String resolve(@PathVariable Long id, @RequestParam FineStatus status, RedirectAttributes ra) {
        fines.updateStatus(id, status);
        ra.addFlashAttribute("success", status == FineStatus.PAID
            ? "Đã ghi nhận thu tiền khoản phạt #" + id + "."
            : "Đã miễn khoản phạt #" + id + ".");
        return "redirect:/librarian/fines";
    }
}
