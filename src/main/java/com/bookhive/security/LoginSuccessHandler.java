package com.bookhive.security;

import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginSuccessHandler
        implements AuthenticationSuccessHandler {

    private final LoginAttemptService attempts;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {

        attempts.success(authentication.getName());

        response.sendRedirect(
            request.getContextPath() + "/"
        );
    }
}