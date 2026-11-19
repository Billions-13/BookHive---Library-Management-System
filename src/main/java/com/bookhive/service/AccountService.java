package com.bookhive.service;

import com.bookhive.dto.*;
import com.bookhive.entity.User;
import com.bookhive.enums.RoleName;
import com.bookhive.exception.NotFoundException;
import com.bookhive.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final CurrentUserService current;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public ProfileForm profile() {
        User user = current.requireUser();
        ProfileForm f = new ProfileForm();
        f.setFullName(user.getFullName()); f.setPhone(user.getPhone());
        return f;
    }
    @Transactional
    public void updateProfile(ProfileForm f) {
        User user = current.requireUser();
        user.setFullName(f.getFullName().trim());
        user.setPhone(f.getPhone());
        audit.log(user, "PROFILE_UPDATED", "User", user.getId(), "Updated name/phone");
    }
    @Transactional
    public void changePassword(PasswordForm form) {
        User user = current.requireUser();
        if (!encoder.matches(form.getOldPassword(), user.getPasswordHash()))
            throw new IllegalArgumentException("Current password is incorrect");
        if (form.getNewPassword() == null || form.getNewPassword().length() < 8 ||
            form.getNewPassword().length() > 72)
            throw new IllegalArgumentException("New password must be 8–72 characters");
        user.setPasswordHash(encoder.encode(form.getNewPassword()));
        audit.log(user, "PASSWORD_CHANGED", "User", user.getId(), "Changed own password");
    }
    @Transactional(readOnly = true)
    public List<User> listUsers() { return users.findAll(); }
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<User> searchUsers(String name, String email,
            RoleName role, Boolean enabled, String studentCode,
            int page, int size, String sort, String direction) {
        size = java.util.List.of(10, 20, 50, 100).contains(size) ? size : 10;
        if (!java.util.Set.of("fullName", "email", "id", "studentCode").contains(sort)) sort = "id";
        var dir = "desc".equalsIgnoreCase(direction) ? org.springframework.data.domain.Sort.Direction.DESC
            : org.springframework.data.domain.Sort.Direction.ASC;
        var paging = org.springframework.data.domain.PageRequest.of(Math.max(0, page), size,
            org.springframework.data.domain.Sort.by(dir, sort));
        org.springframework.data.jpa.domain.Specification<User> spec = (root, query, cb) -> {
            var filters = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (name != null && !name.isBlank()) filters.add(cb.like(cb.lower(root.get("fullName")), "%" + name.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
            if (email != null && !email.isBlank()) filters.add(cb.like(cb.lower(root.get("email")), "%" + email.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
            if (studentCode != null && !studentCode.isBlank()) filters.add(cb.like(cb.lower(root.get("studentCode")), "%" + studentCode.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
            if (role != null) filters.add(cb.equal(root.get("role").get("name"), role));
            if (enabled != null) filters.add(cb.equal(root.get("enabled"), enabled));
            return cb.and(filters.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return users.findAll(spec, paging);
    }

    @Transactional(readOnly = true)
    public AdminUserForm adminForm(Long id) {
        User u = user(id);
        AdminUserForm f = new AdminUserForm();
        f.setFullName(u.getFullName()); f.setEmail(u.getEmail());
        f.setPhone(u.getPhone()); f.setStudentCode(u.getStudentCode());
        f.setRole(u.getRole().getName()); f.setEnabled(u.isEnabled());
        return f;
    }
    @Transactional
    public Long createUser(AdminUserForm f) {
        if (f.getPassword() == null || f.getPassword().length() < 8 || f.getPassword().length() > 72)
            throw new IllegalArgumentException("Password must be 8–72 characters");
        String email = f.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new IllegalArgumentException("Email already exists");
        User u = new User();
        apply(u, f); u.setEmail(email);
        u.setPasswordHash(encoder.encode(f.getPassword()));
        users.saveAndFlush(u);
        audit.log(current.requireUser(), "USER_CREATED", "User", u.getId(), email);
        return u.getId();
    }
    @Transactional
    public void editUser(Long id, AdminUserForm f) {
        User u = user(id);
        String email = f.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailAndIdNot(email, id)) throw new IllegalArgumentException("Email already exists");
        if (current.requireUser().getId().equals(id) &&
            (!f.isEnabled() || f.getRole() != RoleName.ADMIN))
            throw new IllegalArgumentException("Cannot disable/demote your own admin account");
        apply(u, f); u.setEmail(email);
        if (f.getPassword() != null && !f.getPassword().isBlank()) {
            if (f.getPassword().length() < 8 || f.getPassword().length() > 72)
                throw new IllegalArgumentException("Password must be 8–72 characters");
            u.setPasswordHash(encoder.encode(f.getPassword()));
        }
        audit.log(current.requireUser(), "USER_UPDATED", "User", id, email);
    }
    @Transactional
    public void toggle(Long id) {
        User u = user(id);
        if (current.requireUser().getId().equals(id))
            throw new IllegalArgumentException("Cannot lock own admin account");
        u.setEnabled(!u.isEnabled());
        audit.log(current.requireUser(), "USER_ENABLED_CHANGED", "User", id, String.valueOf(u.isEnabled()));
    }
    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }
    private void apply(User u, AdminUserForm f) {
        u.setFullName(f.getFullName().trim());
        u.setPhone(f.getPhone());
        u.setStudentCode(f.getStudentCode() == null || f.getStudentCode().isBlank() ? null : f.getStudentCode().trim());
        if (f.getRole() == null) throw new IllegalArgumentException("Role is required");
        u.setRole(roles.findByName(f.getRole()).orElseThrow(() -> new IllegalArgumentException("Role not found")));
        u.setEnabled(f.isEnabled());
    }
}
