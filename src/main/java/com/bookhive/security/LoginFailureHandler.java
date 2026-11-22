package com.bookhive.security;

import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginFailureHandler
        implements AuthenticationFailureHandler {

    private final LoginAttemptService attempts;

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {

        attempts.fail(request.getParameter("email"));
        response.sendRedirect(
            request.getContextPath() + "/login?error"
        );
    }
}