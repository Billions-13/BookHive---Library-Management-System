package com.bookhive.dto;

import com.bookhive.enums.RoleName;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AdminUserForm {
    @NotBlank @Size(max = 120) private String fullName;
    @Email @NotBlank @Size(max = 191) private String email;
    @Size(max = 20) private String phone;
    @Size(max = 30) private String studentCode;
    @NotNull private RoleName role;
    private boolean enabled = true;
    // Used ONLY when creating; blank keeps existing password when editing.
    private String password;
}
