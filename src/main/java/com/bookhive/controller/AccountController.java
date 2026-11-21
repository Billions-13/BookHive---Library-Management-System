package com.bookhive.controller;

import com.bookhive.dto.*;
import com.bookhive.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accounts;

    @GetMapping("/my/profile")
    public String profile(Model m) {
        m.addAttribute("form", accounts.profile());
        return "my/profile";
    }
    @PostMapping("/my/profile")
    public String saveProfile(@Valid @ModelAttribute("form") ProfileForm form,
            BindingResult errors) {
        if (errors.hasErrors()) return "my/profile";
        accounts.updateProfile(form);
        return "redirect:/my/profile?updated";
    }
    @GetMapping("/my/password")
    public String password(Model m) {
        m.addAttribute("form", new PasswordForm());
        return "my/password";
    }
    @PostMapping("/my/password")
    public String changePassword(@Valid @ModelAttribute("form") PasswordForm form,
            BindingResult errors, Model m) {
        if (errors.hasErrors()) return "my/password";
        try { accounts.changePassword(form); return "redirect:/my/password?updated"; }
        catch (IllegalArgumentException e) { m.addAttribute("error", e.getMessage()); return "my/password"; }
    }
}
