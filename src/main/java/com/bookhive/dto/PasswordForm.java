package com.bookhive.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PasswordForm {
    @NotBlank private String oldPassword;
    @NotBlank @Size(min = 8, max = 72) private String newPassword;
}
