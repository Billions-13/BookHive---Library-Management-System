package com.bookhive.controller;

import com.bookhive.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    public String reserve(@PathVariable Long bookId) {
        reservations.reserve(bookId, current.requireUser());
        return "redirect:/my/reservations";
    }
    @PostMapping("/my/reservations/{id}/cancel")
    public String cancel(@PathVariable Long id) {
        reservations.cancel(id, current.requireUser());
        return "redirect:/my/reservations";
    }
}
