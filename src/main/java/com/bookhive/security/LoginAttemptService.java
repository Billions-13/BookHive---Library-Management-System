package com.bookhive.security;

import com.bookhive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private final UserRepository users;

    @Transactional
    public void fail(String email) {
        users.findByEmail(email).ifPresent(u -> {
            if (u.getLockedUntil() != null &&
                u.getLockedUntil().isAfter(LocalDateTime.now())) {
                return;
            }

            int attempts = u.getFailedAttempts() + 1;
            u.setFailedAttempts(attempts);

            if (attempts >= 5) {
                u.setLockedUntil(
                    LocalDateTime.now().plusMinutes(15)
                );
            }
        });
    }

    @Transactional
    public void success(String email) {
        users.findByEmail(email).ifPresent(u -> {
            u.setFailedAttempts(0);
            u.setLockedUntil(null);
        });
    }
}