package com.bookhive;

import com.bookhive.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Login form wiring, navigation per role and the registration flow. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestFixtures.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // registers new accounts
class AuthFlowTest {

    @Autowired MockMvc mvc;
    @Autowired TestFixtures data;
    @Autowired com.bookhive.repository.UserRepository users;

    @BeforeEach
    void seed() {
        data.seed();
    }

    @Test
    void loginPageIsWiredToSpringSecurity() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("name=\"email\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")))
            .andExpect(content().string(containsString("Đăng nhập")));
    }

    @Test
    void loginWithCorrectPasswordRedirectsHome() throws Exception {
        mvc.perform(post("/login").with(csrf())
                .param("email", TestFixtures.STUDENT)
                .param("password", TestFixtures.PASSWORD))
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void loginWithWrongPasswordShowsError() throws Exception {
        mvc.perform(post("/login").with(csrf())
                .param("email", TestFixtures.LECTURER)
                .param("password", "wrong-password"))
            .andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login").param("error", ""))
            .andExpect(content().string(containsString("không chính xác")));
    }

    @Test
    void successfulRegistrationMessageIsShownOnLoginPage() throws Exception {
        mvc.perform(get("/login").param("registered", ""))
            .andExpect(content().string(containsString("Chúc mừng đăng ký thành công")));
    }

    @Test
    void registerCreatesStudentAccountWithStudentCode() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Võ Thị Mới")
                .param("studentCode", "ititiu22999")
                .param("email", "New.Student@Test.Local")
                .param("password", "MatKhau123")
                .param("confirmPassword", "MatKhau123"))
            .andExpect(redirectedUrl("/login?registered"));

        var saved = users.findByEmail("new.student@test.local").orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("ITITIU22999", saved.getStudentCode());
        org.junit.jupiter.api.Assertions.assertEquals(com.bookhive.enums.RoleName.STUDENT, saved.getRole().getName());
    }

    @Test
    void registerRejectsDuplicatesAndMismatchedPasswords() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Trùng Email")
                .param("email", TestFixtures.STUDENT)
                .param("password", "MatKhau123")
                .param("confirmPassword", "MatKhau123"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Email này đã được đăng ký")));

        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Trùng MSSV")
                .param("studentCode", "ITITIU22001")
                .param("email", "other@test.local")
                .param("password", "MatKhau123")
                .param("confirmPassword", "MatKhau123"))
            .andExpect(content().string(containsString("MSSV này đã được đăng ký")));

        mvc.perform(post("/register").with(csrf())
                .param("fullName", "Sai Xác Nhận")
                .param("email", "mismatch@test.local")
                .param("password", "MatKhau123")
                .param("confirmPassword", "MatKhau456"))
            .andExpect(content().string(containsString("Mật khẩu xác nhận không khớp")));
    }

    @Test
    void validationMessagesAreVietnamese() throws Exception {
        mvc.perform(post("/register").with(csrf())
                .param("fullName", "")
                .param("email", "khong-phai-email")
                .param("password", "123")
                .param("confirmPassword", "123"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Không được để trống")))
            .andExpect(content().string(containsString("Email không hợp lệ")))
            .andExpect(content().string(containsString("Mật khẩu phải dài từ 8 đến 72 ký tự")))
            .andExpect(content().string(not(containsString("name=\"phone\""))));
    }

    @Test
    void navigationDependsOnRole() throws Exception {
        mvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Khám phá tri thức")))
            .andExpect(content().string(containsString("Lập trình Java căn bản")))
            .andExpect(content().string(containsString("navbar-toggler")))
            .andExpect(content().string(not(containsString("Đăng xuất"))));

        mvc.perform(get("/").with(user(TestFixtures.STUDENT).roles("STUDENT")))
            .andExpect(content().string(containsString("Đăng xuất")))
            .andExpect(content().string(containsString("Sách đang mượn")))
            .andExpect(content().string(not(containsString("Quản trị người dùng"))));

        mvc.perform(get("/").with(user(TestFixtures.ADMIN).roles("ADMIN")))
            .andExpect(content().string(containsString("Quản trị người dùng")))
            .andExpect(content().string(containsString("Phiếu mượn / trả")));
    }
}
