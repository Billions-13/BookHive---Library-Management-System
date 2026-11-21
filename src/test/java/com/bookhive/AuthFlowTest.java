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
class AuthFlowTest {

    @Autowired MockMvc mvc;
    @Autowired TestFixtures data;

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
