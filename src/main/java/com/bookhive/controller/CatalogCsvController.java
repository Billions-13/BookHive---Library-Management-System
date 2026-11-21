package com.bookhive.controller;

import com.bookhive.service.CatalogCsvService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Controller
@RequestMapping("/librarian/catalog")
@RequiredArgsConstructor
public class CatalogCsvController {
    private final CatalogCsvService csv;

    @GetMapping("/csv")
    public String page() { return "librarian/csv"; }
    @PostMapping("/import")
    public String upload(@RequestParam("file") MultipartFile file, Model m) throws IOException {
        try {
            m.addAttribute("result", "Đã nhập thành công " + csv.importCsv(file) + " đầu sách");
        } catch (IllegalArgumentException ex) {
            m.addAttribute("error", ex.getMessage());
        }
        return "librarian/csv";
    }
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export() throws IOException {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bookhive-catalog.csv")
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .body(csv.exportCsv());
    }
}
