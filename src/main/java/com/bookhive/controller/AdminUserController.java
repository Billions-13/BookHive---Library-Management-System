package com.bookhive.controller;

import com.bookhive.dto.AdminUserForm;
import com.bookhive.enums.RoleName;
import com.bookhive.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AccountService accounts;

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String name,
            @RequestParam(defaultValue = "") String email,
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "") String studentCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String direction, Model m) {
        var result = accounts.searchUsers(name, email, role, enabled, studentCode,
            page, size, sort, direction);
        m.addAttribute("users", result.getContent()); m.addAttribute("pages", result);
        m.addAttribute("name", name); m.addAttribute("email", email);
        m.addAttribute("selectedRole", role); m.addAttribute("selectedEnabled", enabled);
        m.addAttribute("studentCode", studentCode); m.addAttribute("size", size);
        m.addAttribute("sort", sort); m.addAttribute("direction", direction);
        m.addAttribute("roles", RoleName.values());
        return "admin/users";
    }
    @GetMapping("/new")
    public String create(Model m) { return form(m, new AdminUserForm(), "/admin/users/new"); }
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("form") AdminUserForm f, BindingResult errors, Model m) {
        if (!errors.hasErrors()) {
            try { accounts.createUser(f); return "redirect:/admin/users"; }
            catch (IllegalArgumentException e) { m.addAttribute("error", e.getMessage()); }
        }
        return form(m, f, "/admin/users/new");
    }
    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model m) {
        return form(m, accounts.adminForm(id), "/admin/users/" + id + "/edit");
    }
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("form") AdminUserForm f,
            BindingResult errors, Model m) {
        if (!errors.hasErrors()) {
            try { accounts.editUser(id, f); return "redirect:/admin/users"; }
            catch (IllegalArgumentException e) { m.addAttribute("error", e.getMessage()); }
        }
        return form(m, f, "/admin/users/" + id + "/edit");
    }
    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        accounts.toggle(id); return "redirect:/admin/users";
    }
    private String form(Model m, AdminUserForm f, String action) {
        m.addAttribute("form", f);
        m.addAttribute("formAction", action);
        m.addAttribute("roles", RoleName.values());
        return "admin/user-form";
    }
}
