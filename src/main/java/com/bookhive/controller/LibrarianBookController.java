package com.bookhive.controller;

import com.bookhive.dto.BookForm;
import com.bookhive.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/librarian/books")
@RequiredArgsConstructor
public class LibrarianBookController {
    private final BookService books;
    private final com.bookhive.service.MetadataService metadata;

    @org.springframework.web.bind.annotation.ModelAttribute("authors")
    public java.util.List<com.bookhive.entity.Author> authors() { return metadata.authors(); }

    @org.springframework.web.bind.annotation.ModelAttribute("categories")
    public java.util.List<com.bookhive.entity.Category> categories() { return metadata.categories(); }

    @GetMapping
    public String manage() { return "redirect:/books"; }

    @GetMapping("/new")
    public String createPage(Model m) {
        m.addAttribute("bookForm", new BookForm());
        m.addAttribute("formAction", "/librarian/books/new");
        return "librarian/book-form";
    }
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("bookForm") BookForm form,
                         BindingResult errors, Model m, RedirectAttributes ra) {
        if (!errors.hasErrors()) {
            try {
                Long id = books.create(form);
                ra.addFlashAttribute("success", "Đã thêm đầu sách. Hãy vào Kho sách để nhập lô cho mượn.");
                return "redirect:/books/" + id;
            }
            catch (IllegalArgumentException e) { m.addAttribute("error", e.getMessage()); }
        }
        m.addAttribute("formAction", "/librarian/books/new");
        return "librarian/book-form";
    }
    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model m) {
        m.addAttribute("bookForm", books.form(id));
        m.addAttribute("formAction", "/librarian/books/" + id + "/edit");
        return "librarian/book-form";
    }
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
            @Valid @ModelAttribute("bookForm") BookForm form, BindingResult errors, Model m,
            RedirectAttributes ra) {
        if (!errors.hasErrors()) {
            try {
                books.edit(id, form);
                ra.addFlashAttribute("success", "Đã lưu thay đổi đầu sách.");
                return "redirect:/books/" + id;
            }
            catch (IllegalArgumentException e) { m.addAttribute("error", e.getMessage()); }
        }
        m.addAttribute("formAction", "/librarian/books/" + id + "/edit");
        return "librarian/book-form";
    }
    @PostMapping("/{id}/archive")
    public String archive(@PathVariable Long id, RedirectAttributes ra) {
        books.archive(id);
        ra.addFlashAttribute("success", "Đã ẩn đầu sách khỏi danh mục.");
        return "redirect:/books";
    }
}
