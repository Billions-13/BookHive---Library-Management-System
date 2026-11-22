
package com.bookhive.service;

import com.bookhive.dto.RegisterRequest;
import com.bookhive.entity.User;
import com.bookhive.enums.RoleName;
import com.bookhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail().trim()
            .toLowerCase();

        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException(
                "Email already registered"
            );
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(
            encoder.encode(request.getPassword())
        );
        user.setEnabled(true);
        user.setRole(
            roles.findByName(RoleName.STUDENT)
                .orElseThrow()
        );

        users.save(user);
    }
}
