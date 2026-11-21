package com.bookhive;

import com.bookhive.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Flows that change data (CSV import, borrowing, returns...). The context is
 * discarded afterwards so the read-only page tests always see the original fixtures.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestFixtures.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class LibraryFlowTest {

    @Autowired MockMvc mvc;
    @Autowired TestFixtures data;

    @BeforeEach
    void seed() {
        data.seed();
    }

    static RequestPostProcessor librarian() { return user(TestFixtures.LIBRARIAN).roles("LIBRARIAN"); }

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "books.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    static RequestPostProcessor student() { return user(TestFixtures.STUDENT).roles("STUDENT"); }

    @Test
    void borrowReturnAndFineFlow() throws Exception {
        // Reader borrows the last available copy.
        mvc.perform(post("/my/borrow/" + data.availableBookId).with(csrf()).with(student()))
            .andExpect(redirectedUrl("/my/loans"))
            .andExpect(flash().attribute("success", containsString("Mượn sách thành công")));
        mvc.perform(get("/books/" + data.availableBookId).with(student()))
            .andExpect(content().string(containsString("Hiện không có sách để mượn")));

        // Librarian receives the overdue loan (6 days late) -> 12.000 ₫ fine.
        mvc.perform(post("/librarian/loans/" + data.activeLoanId + "/return").with(csrf()).with(librarian()))
            .andExpect(redirectedUrl("/librarian/loans"))
            .andExpect(flash().attribute("success", containsString("Đã nhận trả sách")));
        mvc.perform(get("/my/fines").with(student()))
            .andExpect(content().string(containsString("12.000 ₫")))
            .andExpect(content().string(containsString("Trả trễ 6 ngày")));

        // Returning twice is rejected with a Vietnamese business error.
        mvc.perform(post("/librarian/loans/" + data.activeLoanId + "/return").with(csrf()).with(librarian()))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Phiếu mượn này đã được trả")));
    }

    @Test
    void reservationQueueFlow() throws Exception {
        // Another reader is already waiting for this title, so borrowing must go through the queue.
        mvc.perform(post("/my/borrow/" + data.outOfStockBookId).with(csrf()).with(student()))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Sách đang có người đặt trước")));

        mvc.perform(post("/my/reservations/" + data.outOfStockBookId).with(csrf()).with(student()))
            .andExpect(redirectedUrl("/my/reservations"))
            .andExpect(flash().attribute("success", containsString("Đã đặt trước")));
        mvc.perform(post("/my/reservations/" + data.outOfStockBookId).with(csrf()).with(student()))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Bạn đã đặt trước đầu sách này rồi")));

        // A reader cannot cancel someone else's reservation.
        mvc.perform(post("/my/reservations/" + data.reservationId + "/cancel").with(csrf()).with(student()))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("không thể huỷ lượt đặt trước của người khác")));
    }

    @Test
    void receivingABatchCreatesCopiesAndShowsSuccess() throws Exception {
        mvc.perform(post("/librarian/books/" + data.outOfStockBookId + "/batches").with(csrf()).with(librarian())
                .param("quantity", "3").param("supplier", "Nhà sách Phương Nam"))
            .andExpect(redirectedUrl("/librarian/books/" + data.outOfStockBookId + "/inventory"))
            .andExpect(flash().attribute("success", "Đã nhập 3 cuốn vào kho, mỗi cuốn có mã riêng."));

        mvc.perform(post("/librarian/books/" + data.outOfStockBookId + "/batches").with(csrf()).with(librarian())
                .param("quantity", "0"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Giá trị nhỏ nhất là 1")));
    }

    @Test
    void csvImportReportsResultInVietnamese() throws Exception {
        mvc.perform(multipart("/librarian/catalog/import").file(csv(
                "isbn,title,publisher,publication_year,language\n"
                + "9786040000001,Giải tích 1,NXB Đại học Quốc gia,2021,Tiếng Việt\n"
                + "9786040000002,Vật lý đại cương,NXB Giáo dục,2020,Tiếng Việt\n"))
                .with(csrf()).with(librarian()))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Đã nhập thành công 2 đầu sách")));
    }

    @Test
    void csvImportRejectsBadFilesWithVietnameseMessages() throws Exception {
        mvc.perform(multipart("/librarian/catalog/import").file(csv("ma,ten\n1,abc\n"))
                .with(csrf()).with(librarian()))
            .andExpect(content().string(containsString("File phải có đủ các cột")));

        mvc.perform(multipart("/librarian/catalog/import").file(csv(
                "isbn,title,publisher,publication_year,language\n"
                + "9786040000099,Sách lỗi năm,NXB Trẻ,năm-hai-nghìn,Tiếng Việt\n"))
                .with(csrf()).with(librarian()))
            .andExpect(content().string(containsString("Năm xuất bản không hợp lệ ở dòng 1")));
    }
}
