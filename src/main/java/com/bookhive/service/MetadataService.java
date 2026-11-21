package com.bookhive.service;

import com.bookhive.dto.*;
import com.bookhive.entity.*;
import com.bookhive.exception.NotFoundException;
import com.bookhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MetadataService {
    private final AuthorRepository authors;
    private final CategoryRepository categories;
    private final AuditService audit;
    private final CurrentUserService current;

    @Transactional(readOnly = true)
    public List<Author> authors() { return authors.findByActiveTrueOrderByNameAsc(); }
    @Transactional(readOnly = true)
    public List<Category> categories() { return categories.findByActiveTrueOrderByDisplayOrderAscNameAsc(); }
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Author> searchAuthors(String name, String country,
            Integer birthYear, Integer deathYear, Boolean active,
            int page, int size, String sort, String direction) {
        if (!java.util.Set.of("name", "country", "birthYear", "id").contains(sort)) sort = "name";
        var paging = paging(page, size, sort, direction);
        return authors.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (notBlank(name)) predicates.add(cb.like(cb.lower(root.get("name")), like(name)));
            if (notBlank(country)) predicates.add(cb.like(cb.lower(root.get("country")), like(country)));
            if (birthYear != null) predicates.add(cb.equal(root.get("birthYear"), birthYear));
            if (deathYear != null) predicates.add(cb.equal(root.get("deathYear"), deathYear));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, paging);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Category> searchCategories(String name, String slug,
            String audience, Integer displayOrder, Boolean active,
            int page, int size, String sort, String direction) {
        if (!java.util.Set.of("name", "slug", "displayOrder", "id").contains(sort)) sort = "name";
        var paging = paging(page, size, sort, direction);
        return categories.findAll((root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (notBlank(name)) predicates.add(cb.like(cb.lower(root.get("name")), like(name)));
            if (notBlank(slug)) predicates.add(cb.like(cb.lower(root.get("slug")), like(slug)));
            if (notBlank(audience)) predicates.add(cb.like(cb.lower(root.get("targetAudience")), like(audience)));
            if (displayOrder != null) predicates.add(cb.equal(root.get("displayOrder"), displayOrder));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, paging);
    }
    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static String like(String s) { return "%" + s.trim().toLowerCase(Locale.ROOT) + "%"; }
    private static org.springframework.data.domain.Pageable paging(int page, int size,
            String sort, String direction) {
        size = List.of(10, 20, 50, 100).contains(size) ? size : 10;
        var dir = "desc".equalsIgnoreCase(direction) ? org.springframework.data.domain.Sort.Direction.DESC
            : org.springframework.data.domain.Sort.Direction.ASC;
        return org.springframework.data.domain.PageRequest.of(Math.max(page, 0), size,
            org.springframework.data.domain.Sort.by(dir, sort));
    }

    @Transactional(readOnly = true)
    public AuthorForm authorForm(Long id) {
        Author a = authors.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy tác giả"));
        AuthorForm f = new AuthorForm();
        f.setName(a.getName()); f.setCountry(a.getCountry()); f.setBirthYear(a.getBirthYear());
        f.setDeathYear(a.getDeathYear()); f.setBiography(a.getBiography()); f.setWebsite(a.getWebsite());
        return f;
    }
    @Transactional(readOnly = true)
    public CategoryForm categoryForm(Long id) {
        Category c = categories.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại"));
        CategoryForm f = new CategoryForm();
        f.setName(c.getName()); f.setSlug(c.getSlug()); f.setDescription(c.getDescription());
        f.setTargetAudience(c.getTargetAudience()); f.setDisplayOrder(c.getDisplayOrder());
        return f;
    }
    @Transactional
    public void saveAuthor(Long id, AuthorForm f) {
        Author a = id == null ? new Author() : authors.findById(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy tác giả"));
        if (f.getBirthYear() != null && f.getDeathYear() != null && f.getDeathYear() < f.getBirthYear())
            throw new IllegalArgumentException("Năm mất không thể trước năm sinh");
        if (id == null && authors.existsByName(f.getName().trim()))
            throw new IllegalArgumentException("Tác giả này đã tồn tại");
        a.setName(f.getName().trim()); a.setCountry(f.getCountry());
        a.setBirthYear(f.getBirthYear()); a.setDeathYear(f.getDeathYear());
        a.setBiography(f.getBiography()); a.setWebsite(f.getWebsite());
        authors.saveAndFlush(a);
        audit.log(current.requireUser(), "AUTHOR_SAVED", "Author", a.getId(), a.getName());
    }
    @Transactional
    public void saveCategory(Long id, CategoryForm f) {
        Category c = id == null ? new Category() : categories.findById(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại"));
        if (id == null && (categories.existsByName(f.getName().trim()) || categories.existsBySlug(f.getSlug().trim())))
            throw new IllegalArgumentException("Tên hoặc slug thể loại đã tồn tại");
        c.setName(f.getName().trim()); c.setSlug(f.getSlug().trim());
        c.setDescription(f.getDescription()); c.setTargetAudience(f.getTargetAudience());
        c.setDisplayOrder(f.getDisplayOrder()); categories.saveAndFlush(c);
        audit.log(current.requireUser(), "CATEGORY_SAVED", "Category", c.getId(), c.getName());
    }
    @Transactional
    public void archiveAuthor(Long id) {
        Author a = authors.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy tác giả"));
        a.setActive(false);
        audit.log(current.requireUser(), "AUTHOR_ARCHIVED", "Author", id, a.getName());
    }
    @Transactional
    public void archiveCategory(Long id) {
        Category c = categories.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy thể loại"));
        c.setActive(false);
        audit.log(current.requireUser(), "CATEGORY_ARCHIVED", "Category", id, c.getName());
    }
}
