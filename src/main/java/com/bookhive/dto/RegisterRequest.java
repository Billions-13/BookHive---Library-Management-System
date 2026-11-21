package com.bookhive.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {

    @NotBlank
    @Size(max = 120)
    private String fullName;

    /** Optional: lecturers have no student code. Stored in users.student_code (unique). */
    @Pattern(regexp = "^$|^[A-Za-z0-9]{4,30}$", message = "MSSV chỉ gồm chữ và số, dài 4–30 ký tự")
    private String studentCode;

    @Email
    @NotBlank
    @Size(max = 191)
    private String email;

    @NotBlank
    @Size(min = 8, max = 72, message = "Mật khẩu phải dài từ 8 đến 72 ký tự")
    private String password;

    /** Checked against password in AuthController; never stored. */
    @NotBlank
    private String confirmPassword;
}
