package com.bookhive;

import com.bookhive.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Renders every page as the role that is allowed to open it. A Thymeleaf or
 * lazy-loading error makes the request fail, so this catches broken templates
 * before they reach the deployed site.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestFixtures.class)
class PageRenderingTest {

    @Autowired MockMvc mvc;
    @Autowired TestFixtures data;

    @BeforeEach
    void seed() {
        data.seed();
    }

    static RequestPostProcessor student() { return user(TestFixtures.STUDENT).roles("STUDENT"); }
    static RequestPostProcessor librarian() { return user(TestFixtures.LIBRARIAN).roles("LIBRARIAN"); }
    static RequestPostProcessor admin() { return user(TestFixtures.ADMIN).roles("ADMIN"); }

    @Test
    void publicPagesRender() throws Exception {
        for (String url : new String[]{"/", "/books", "/books/" + data.availableBookId,
                "/books/" + data.outOfStockBookId, "/login", "/register"}) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    void readerPagesRender() throws Exception {
        for (String url : new String[]{"/my/loans", "/my/reservations", "/my/fines",
                "/my/profile", "/my/password"}) {
            mvc.perform(get(url).with(student())).andExpect(status().isOk());
        }
    }

    @Test
    void librarianPagesRender() throws Exception {
        for (String url : new String[]{
                "/librarian/books/new",
                "/librarian/books/" + data.availableBookId + "/edit",
                "/librarian/books/" + data.availableBookId + "/inventory",
                "/librarian/loans", "/librarian/fines",
                "/librarian/authors", "/librarian/authors/new",
                "/librarian/authors/" + data.authorId + "/edit",
                "/librarian/categories", "/librarian/categories/new",
                "/librarian/categories/" + data.categoryId + "/edit",
                "/librarian/catalog/csv"}) {
            mvc.perform(get(url).with(librarian())).andExpect(status().isOk());
        }
    }

    @Test
    void adminPagesRender() throws Exception {
        for (String url : new String[]{"/admin/users", "/admin/users/new",
                "/admin/users/" + data.studentId + "/edit"}) {
            mvc.perform(get(url).with(admin())).andExpect(status().isOk());
        }
    }

    @Test
    void lazyAssociationsRenderWithoutOpenSessionInView() throws Exception {
        // copy.batch is LAZY; these pages walk loan -> copy -> batch -> book after the service transaction ends.
        mvc.perform(get("/my/loans").with(student()))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Lập trình Java căn bản")));
        mvc.perform(get("/librarian/books/" + data.availableBookId + "/inventory").with(librarian()))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("BATCH-T1")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("COPYT0002")));
    }

    @Test
    void statsApiReturnsJson() throws Exception {
        mvc.perform(get("/api/stats").with(librarian()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalTitles").value(2))
            .andExpect(jsonPath("$.availableCopies").value(1))
            .andExpect(jsonPath("$.borrowedCopies").value(2))
            .andExpect(jsonPath("$.overdueLoans").value(1));
    }

    @Test
    void accessRulesAreEnforced() throws Exception {
        mvc.perform(get("/my/loans")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/librarian/loans").with(student())).andExpect(status().isForbidden());
        mvc.perform(get("/admin/users").with(librarian())).andExpect(status().isForbidden());
        mvc.perform(get("/api/stats").with(student())).andExpect(status().isForbidden());
    }
}
