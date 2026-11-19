package com.bookhive.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class AuthorForm {
    @NotBlank @Size(max = 150) private String name;
    @Size(max = 100) private String country;
    @Min(0) @Max(2100) private Integer birthYear;
    @Min(0) @Max(2100) private Integer deathYear;
    private String biography;
    @Size(max = 255) private String website;
}
