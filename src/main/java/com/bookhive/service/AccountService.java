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
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        if (form.getNewPassword() == null || form.getNewPassword().length() < 8 ||
            form.getNewPassword().length() > 72)
            throw new IllegalArgumentException("Mật khẩu mới phải dài từ 8 đến 72 ký tự");
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
            throw new IllegalArgumentException("Mật khẩu phải dài từ 8 đến 72 ký tự");
        String email = f.getEmail().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmail(email)) throw new IllegalArgumentException("Email này đã được sử dụng");
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
        if (users.existsByEmailAndIdNot(email, id)) throw new IllegalArgumentException("Email này đã được sử dụng");
        if (current.requireUser().getId().equals(id) &&
            (!f.isEnabled() || f.getRole() != RoleName.ADMIN))
            throw new IllegalArgumentException("Không thể tự khoá hoặc hạ quyền tài khoản quản trị của chính bạn");
        apply(u, f); u.setEmail(email);
        if (f.getPassword() != null && !f.getPassword().isBlank()) {
            if (f.getPassword().length() < 8 || f.getPassword().length() > 72)
                throw new IllegalArgumentException("Mật khẩu phải dài từ 8 đến 72 ký tự");
            u.setPasswordHash(encoder.encode(f.getPassword()));
        }
        audit.log(current.requireUser(), "USER_UPDATED", "User", id, email);
    }
    @Transactional
    public void toggle(Long id) {
        User u = user(id);
        if (current.requireUser().getId().equals(id))
            throw new IllegalArgumentException("Không thể tự khoá tài khoản quản trị của chính bạn");
        u.setEnabled(!u.isEnabled());
        audit.log(current.requireUser(), "USER_ENABLED_CHANGED", "User", id, String.valueOf(u.isEnabled()));
    }
    private User user(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng"));
    }
    private void apply(User u, AdminUserForm f) {
        u.setFullName(f.getFullName().trim());
        u.setPhone(f.getPhone());
        u.setStudentCode(f.getStudentCode() == null || f.getStudentCode().isBlank() ? null : f.getStudentCode().trim());
        if (f.getRole() == null) throw new IllegalArgumentException("Vui lòng chọn vai trò");
        u.setRole(roles.findByName(f.getRole()).orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại")));
        u.setEnabled(f.isEnabled());
    }
}
