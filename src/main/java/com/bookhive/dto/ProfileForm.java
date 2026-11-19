package com.bookhive.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProfileForm {
    @NotBlank @Size(max = 120)
    private String fullName;
    @Size(max = 20)
    private String phone;
}
