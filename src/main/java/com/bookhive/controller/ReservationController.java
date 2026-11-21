package com.bookhive.controller;

import com.bookhive.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ReservationController {
    private final ReservationService reservations;
    private final CurrentUserService current;

    @GetMapping("/my/reservations")
    public String list(Model m) {
        m.addAttribute("reservations", reservations.forUser(current.requireUser().getId()));
        return "my/reservations";
    }
    @PostMapping("/my/reservations/{bookId}")
    public String reserve(@PathVariable Long bookId, RedirectAttributes ra) {
        reservations.reserve(bookId, current.requireUser());
        ra.addFlashAttribute("success", "Đã đặt trước. Khi có sách trả về, thư viện sẽ giữ sách cho bạn 2 ngày.");
        return "redirect:/my/reservations";
    }
    @PostMapping("/my/reservations/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        reservations.cancel(id, current.requireUser());
        ra.addFlashAttribute("success", "Đã huỷ đặt trước.");
        return "redirect:/my/reservations";
    }
}
