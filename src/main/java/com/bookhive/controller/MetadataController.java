package com.bookhive.controller;

import com.bookhive.dto.*;
import com.bookhive.service.MetadataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/librarian")
@RequiredArgsConstructor
public class MetadataController {
    private final MetadataService service;

    @GetMapping("/authors")
    public String authors(@RequestParam(defaultValue = "") String name,
        @RequestParam(defaultValue = "") String country,
        @RequestParam(required = false) Integer birthYear,
        @RequestParam(required = false) Integer deathYear,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "name") String sort,
        @RequestParam(defaultValue = "asc") String direction, Model m) {
        var result = service.searchAuthors(name,country,birthYear,deathYear,active,page,size,sort,direction);
        m.addAttribute("authors", result.getContent()); m.addAttribute("pages", result);
        m.addAttribute("name", name); m.addAttribute("country", country);
        m.addAttribute("birthYear", birthYear); m.addAttribute("deathYear", deathYear);
        m.addAttribute("active", active); m.addAttribute("size", size);
        m.addAttribute("sort", sort); m.addAttribute("direction", direction);
        return "librarian/authors";
    }
    @GetMapping("/authors/new")
    public String newAuthor(Model m) { return authorForm(m, new AuthorForm(), "/librarian/authors/new"); }
    @GetMapping("/authors/{id}/edit")
    public String editAuthor(@PathVariable Long id, Model m) {
        return authorForm(m, service.authorForm(id), "/librarian/authors/" + id + "/edit");
    }
    @PostMapping("/authors/new")
    public String saveAuthor(@Valid @ModelAttribute("form") AuthorForm f, BindingResult e, Model m,
            RedirectAttributes ra) {
        return saveAuthor(null, f, e, m, ra);
    }
    @PostMapping("/authors/{id}/edit")
    public String updateAuthor(@PathVariable Long id, @Valid @ModelAttribute("form") AuthorForm f,
             BindingResult e, Model m, RedirectAttributes ra) { return saveAuthor(id, f, e, m, ra); }
    private String saveAuthor(Long id, AuthorForm f, BindingResult e, Model m, RedirectAttributes ra) {
        if (!e.hasErrors()) {
            try {
                service.saveAuthor(id, f);
                ra.addFlashAttribute("success", "Đã lưu tác giả " + f.getName() + ".");
                return "redirect:/librarian/authors";
            }
            catch (IllegalArgumentException ex) { m.addAttribute("error", ex.getMessage()); }
        }
        return authorForm(m, f, id == null ? "/librarian/authors/new" : "/librarian/authors/" + id + "/edit");
    }
    @PostMapping("/authors/{id}/archive")
    public String archiveAuthor(@PathVariable Long id, RedirectAttributes ra) {
        service.archiveAuthor(id);
        ra.addFlashAttribute("success", "Đã ẩn tác giả.");
        return "redirect:/librarian/authors";
    }

    @GetMapping("/categories")
    public String categories(@RequestParam(defaultValue = "") String name,
        @RequestParam(defaultValue = "") String slug,
        @RequestParam(defaultValue = "") String audience,
        @RequestParam(required = false) Integer displayOrder,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "name") String sort,
        @RequestParam(defaultValue = "asc") String direction, Model m) {
        var result = service.searchCategories(name,slug,audience,displayOrder,active,page,size,sort,direction);
        m.addAttribute("categories", result.getContent()); m.addAttribute("pages", result);
        m.addAttribute("name", name); m.addAttribute("slug", slug);
        m.addAttribute("audience", audience); m.addAttribute("displayOrder", displayOrder);
        m.addAttribute("active", active); m.addAttribute("size", size);
        m.addAttribute("sort", sort); m.addAttribute("direction", direction);
        return "librarian/categories";
    }
    @GetMapping("/categories/new")
    public String newCategory(Model m) { return categoryForm(m, new CategoryForm(), "/librarian/categories/new"); }
    @GetMapping("/categories/{id}/edit")
    public String editCategory(@PathVariable Long id, Model m) {
        return categoryForm(m, service.categoryForm(id), "/librarian/categories/" + id + "/edit");
    }
    @PostMapping("/categories/new")
    public String saveCategory(@Valid @ModelAttribute("form") CategoryForm f, BindingResult e, Model m,
            RedirectAttributes ra) {
        return saveCategory(null, f, e, m, ra);
    }
    @PostMapping("/categories/{id}/edit")
    public String updateCategory(@PathVariable Long id, @Valid @ModelAttribute("form") CategoryForm f,
             BindingResult e, Model m, RedirectAttributes ra) { return saveCategory(id, f, e, m, ra); }
    private String saveCategory(Long id, CategoryForm f, BindingResult e, Model m, RedirectAttributes ra) {
        if (!e.hasErrors()) {
            try {
                service.saveCategory(id, f);
                ra.addFlashAttribute("success", "Đã lưu thể loại " + f.getName() + ".");
                return "redirect:/librarian/categories";
            }
            catch (IllegalArgumentException ex) { m.addAttribute("error", ex.getMessage()); }
        }
        return categoryForm(m, f, id == null ? "/librarian/categories/new" : "/librarian/categories/" + id + "/edit");
    }
    @PostMapping("/categories/{id}/archive")
    public String archiveCategory(@PathVariable Long id, RedirectAttributes ra) {
        service.archiveCategory(id);
        ra.addFlashAttribute("success", "Đã ẩn thể loại.");
        return "redirect:/librarian/categories";
    }

    private String authorForm(Model m, AuthorForm f, String action) {
        m.addAttribute("form", f); m.addAttribute("formAction", action);
        return "librarian/author-form";
    }
    private String categoryForm(Model m, CategoryForm f, String action) {
        m.addAttribute("form", f); m.addAttribute("formAction", action);
        return "librarian/category-form";
    }
}
