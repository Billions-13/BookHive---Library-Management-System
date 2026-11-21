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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
            .andExpect(content().string(containsString("Lập trình Java căn bản")));
        mvc.perform(get("/librarian/books/" + data.availableBookId + "/inventory").with(librarian()))
            .andExpect(content().string(containsString("BATCH-T1")))
            .andExpect(content().string(containsString("COPYT0002")));
    }

    @Test
    void catalogShowsStockAndKeepsSelectedFilters() throws Exception {
        mvc.perform(get("/books").param("size", "20").param("sort", "publisher").param("direction", "desc"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Còn 1 cuốn")))
            .andExpect(content().string(containsString("Hết sách")))
            .andExpect(content().string(containsString("<option value=\"20\" selected=\"selected\">")))
            .andExpect(content().string(containsString("<option value=\"publisher\" selected=\"selected\">")))
            .andExpect(content().string(containsString("<option value=\"desc\" selected=\"selected\">")));

        mvc.perform(get("/books").param("author", "không-có-tác-giả-này"))
            .andExpect(content().string(containsString("Không tìm thấy sách phù hợp")));
    }

    @Test
    void bookDetailOffersBorrowOrReservation() throws Exception {
        mvc.perform(get("/books/" + data.availableBookId).with(student()))
            .andExpect(content().string(containsString("Mượn sách này")))
            .andExpect(content().string(not(containsString("Ẩn đầu sách"))));

        mvc.perform(get("/books/" + data.outOfStockBookId).with(student()))
            .andExpect(content().string(containsString("Hiện không có sách để mượn")))
            .andExpect(content().string(containsString("Đặt trước")))
            .andExpect(content().string(not(containsString("Mượn sách này"))));

        mvc.perform(get("/books/" + data.availableBookId).with(librarian()))
            .andExpect(content().string(containsString("Ẩn đầu sách")))
            .andExpect(content().string(containsString("Kho sách")));

        mvc.perform(get("/books/" + data.availableBookId))
            .andExpect(content().string(containsString("Đăng nhập để mượn")));
    }

    @Test
    void readerPagesShowLoanStateAndFines() throws Exception {
        mvc.perform(get("/my/loans").with(student()))
            .andExpect(content().string(containsString("Quá hạn")))
            .andExpect(content().string(containsString("Lịch sử mượn trả")))
            .andExpect(content().string(containsString("Trả trễ")));

        mvc.perform(get("/my/fines").with(student()))
            .andExpect(content().string(containsString("10.000 ₫")))
            .andExpect(content().string(containsString("Chưa thanh toán")));

        mvc.perform(get("/my/reservations").with(user(TestFixtures.LECTURER).roles("LECTURER")))
            .andExpect(content().string(containsString("Đang chờ")))
            .andExpect(content().string(containsString("Huỷ đặt trước")));

        mvc.perform(get("/my/profile").with(student()))
            .andExpect(content().string(containsString("Nguyễn Văn Sinh")))
            .andExpect(content().string(containsString("Sinh viên")));
    }

    @Test
    void librarianPagesShowCirculationData() throws Exception {
        mvc.perform(get("/librarian/loans").param("bookId", String.valueOf(data.availableBookId)).with(librarian()))
            .andExpect(content().string(containsString("Quá hạn")))
            .andExpect(content().string(containsString("Nhận trả")))
            .andExpect(content().string(containsString("value=\"" + data.availableBookId + "\"")))
            // Only active students/lecturers can be chosen as readers.
            .andExpect(content().string(containsString("Nguyễn Văn Sinh")))
            .andExpect(content().string(not(containsString("Lê Thủ Thư –"))));

        mvc.perform(get("/librarian/fines").with(librarian()))
            .andExpect(content().string(containsString("10.000 ₫")))
            .andExpect(content().string(containsString("Đã thu tiền")));

        mvc.perform(get("/librarian/books/" + data.availableBookId + "/inventory").with(librarian()))
            .andExpect(content().string(containsString("Hư hỏng")))
            .andExpect(content().string(containsString("Đang lưu thông")))
            .andExpect(content().string(containsString("Thanh lý cả lô")));

        mvc.perform(get("/librarian/books/new").with(librarian()))
            .andExpect(content().string(containsString("Thêm đầu sách")))
            .andExpect(content().string(containsString("Nguyễn Nhật Ánh")));
        mvc.perform(get("/librarian/books/" + data.availableBookId + "/edit").with(librarian()))
            .andExpect(content().string(containsString("Sửa đầu sách")))
            // th:field escapes accented letters as HTML entities, so check the ISBN value instead of the title.
            .andExpect(content().string(containsString("value=\"9780000000001\"")));
    }

    @Test
    void metadataAndCsvPagesRenderInVietnamese() throws Exception {
        mvc.perform(get("/librarian/authors").param("active", "true").param("size", "50").with(librarian()))
            .andExpect(content().string(containsString("Nguyễn Nhật Ánh")))
            .andExpect(content().string(containsString("<option value=\"true\" selected=\"selected\">")))
            .andExpect(content().string(containsString("<option value=\"50\" selected=\"selected\">")));

        mvc.perform(get("/librarian/categories").with(librarian()))
            .andExpect(content().string(containsString("cong-nghe-thong-tin")))
            .andExpect(content().string(containsString("Đang sử dụng")));

        mvc.perform(get("/librarian/categories/new").with(librarian()))
            .andExpect(content().string(containsString("Thêm thể loại")))
            .andExpect(content().string(containsString("data-slug-from=\"#name\"")));

        mvc.perform(get("/librarian/catalog/csv").with(librarian()))
            .andExpect(content().string(containsString("Nhập sách từ file CSV")))
            .andExpect(content().string(containsString("Tải file CSV")));
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
