package com.bookhive.service;

import com.bookhive.entity.User;
import com.bookhive.repository.UserRepository;
import com.bookhive.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository users;

    public User requireUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Account not found"));
    }
}
