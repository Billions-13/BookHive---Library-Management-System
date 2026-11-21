
package com.bookhive.service;

import com.bookhive.dto.RegisterRequest;
import com.bookhive.entity.User;
import com.bookhive.enums.RoleName;
import com.bookhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    @Transactional
    public void register(RegisterRequest request) {
        String email = request.getEmail().trim()
            .toLowerCase(Locale.ROOT);
        String studentCode = request.getStudentCode() == null || request.getStudentCode().isBlank()
            ? null : request.getStudentCode().trim().toUpperCase(Locale.ROOT);

        if (users.existsByEmail(email)) {
            throw new IllegalArgumentException(
                "Email này đã được đăng ký"
            );
        }
        if (studentCode != null && users.existsByStudentCode(studentCode)) {
            throw new IllegalArgumentException(
                "MSSV này đã được đăng ký"
            );
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setStudentCode(studentCode);
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
