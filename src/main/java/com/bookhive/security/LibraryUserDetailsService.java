
package com.bookhive.security;

import com.bookhive.entity.User;
import com.bookhive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LibraryUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                new UsernameNotFoundException(
                    "Email hoặc mật khẩu không đúng"
                )
            );

        boolean locked =
            user.getLockedUntil() != null &&
            user.getLockedUntil()
                .isAfter(LocalDateTime.now());

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(
                "ROLE_" + user.getRole().getName().name()
            )
            .disabled(!user.isEnabled())
            .accountLocked(locked)
            .build();
    }
}
