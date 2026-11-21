package com.bookhive.controller;

import com.bookhive.dto.BatchForm;
import com.bookhive.enums.CopyStatus;
import com.bookhive.service.InventoryService;
import com.bookhive.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/librarian")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventory;
    private final BookService books;

    @GetMapping("/books/{bookId}/inventory")
    public String list(@PathVariable Long bookId, Model m) {
        m.addAttribute("book", books.details(bookId));
        m.addAttribute("batches", inventory.batches(bookId));
        m.addAttribute("copies", inventory.copies(bookId));
        m.addAttribute("batchForm", new BatchForm());
        m.addAttribute("statuses", new CopyStatus[]{CopyStatus.AVAILABLE, CopyStatus.DAMAGED,
            CopyStatus.LOST, CopyStatus.DISCARDED});
        return "librarian/inventory";
    }
    @PostMapping("/books/{bookId}/batches")
    public String receive(@PathVariable Long bookId,
             @Valid @ModelAttribute("batchForm") BatchForm form, BindingResult errors, Model m) {
        if (errors.hasErrors()) {
            m.addAttribute("error", "Số lượng hoặc nhà cung cấp của lô nhập không hợp lệ");
            m.addAttribute("book", books.details(bookId));
            m.addAttribute("batches", inventory.batches(bookId));
            m.addAttribute("copies", inventory.copies(bookId));
            m.addAttribute("statuses", new CopyStatus[]{CopyStatus.AVAILABLE, CopyStatus.DAMAGED,
                CopyStatus.LOST, CopyStatus.DISCARDED});
            return "librarian/inventory";
        }
        inventory.receive(bookId, form);
        return "redirect:/librarian/books/" + bookId + "/inventory";
    }
    @PostMapping("/books/{bookId}/copies/{copyId}/status")
    public String changeStatus(@PathVariable Long bookId, @PathVariable Long copyId,
            @RequestParam CopyStatus status, @RequestParam(required = false) String note) {
        inventory.changeStatus(bookId, copyId, status, note);
        return "redirect:/librarian/books/" + bookId + "/inventory";
    }
    @PostMapping("/books/{bookId}/batches/{batchId}/discard")
    public String discardBatch(@PathVariable Long bookId, @PathVariable Long batchId) {
        inventory.discardBatch(batchId);
        return "redirect:/librarian/books/" + bookId + "/inventory";
    }
}
