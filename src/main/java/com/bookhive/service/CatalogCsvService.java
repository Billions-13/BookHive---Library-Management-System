package com.bookhive.service;

import com.bookhive.dto.BookForm;
import com.bookhive.dto.BookRow;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CatalogCsvService {
    private final BookService books;

    @Transactional
    public int importCsv(MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 2_000_000)
            throw new IllegalArgumentException("Choose a CSV file no larger than 2 MB");
        var csv = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
            .setIgnoreSurroundingSpaces(true).get();
        int count = 0;
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = csv.parse(reader)) {
            Map<String, Integer> hdr = parser.getHeaderMap();
            if (!hdr.keySet().containsAll(List.of("isbn", "title", "publisher", "publication_year", "language")))
                throw new IllegalArgumentException("Required columns: isbn,title,publisher,publication_year,language");
            for (CSVRecord r : parser) {
                if (++count > 1000) throw new IllegalArgumentException("Maximum 1000 books per CSV");
                BookForm f = new BookForm();
                f.setIsbn(r.get("isbn")); f.setTitle(r.get("title"));
                f.setPublisher(r.get("publisher")); f.setLanguage(r.get("language"));
                if (!r.get("publication_year").isBlank()) {
                    try { f.setPublicationYear(Integer.valueOf(r.get("publication_year"))); }
                    catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid year on line " + r.getRecordNumber()); }
                }
                if (f.getIsbn().isBlank() || f.getTitle().isBlank())
                    throw new IllegalArgumentException("Missing ISBN/title on line " + r.getRecordNumber());
                books.create(f);
            }
        }
        return count;
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv() throws IOException {
        StringWriter sw = new StringWriter();
        try (CSVPrinter out = new CSVPrinter(sw, CSVFormat.DEFAULT)) {
            out.printRecord("isbn", "title", "publisher", "publication_year", "language", "available_copies");
            for (BookRow b : books.exportAll()) {
                out.printRecord(safe(b.isbn()), safe(b.title()), safe(b.publisher()),
                    b.publicationYear() == 0 ? "" : b.publicationYear(), safe(b.language()), b.availableCopies());
            }
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }
    private static String safe(String value) {
        if (value == null) return "";
        // Prevent spreadsheet formula injection if CSV is opened by Excel.
        return !value.isBlank() && "=+-@".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
    }
}
