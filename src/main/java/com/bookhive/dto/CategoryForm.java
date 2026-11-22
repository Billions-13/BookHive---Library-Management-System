package com.bookhive.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CategoryForm {
    @NotBlank @Size(max = 100) private String name;
    @NotBlank @Size(max = 120) private String slug;
    private String description;
    @Size(max = 80) private String targetAudience;
    private int displayOrder;
}
