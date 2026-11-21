package com.bookhive.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BookForm {
    private java.util.List<Long> authorIds = new java.util.ArrayList<>();
    private java.util.List<Long> categoryIds = new java.util.ArrayList<>();
    @NotBlank @Size(max = 20)
    private String isbn;
    @NotBlank @Size(max = 255)
    private String title;
    @Size(max = 80)
    private String edition;
    @Size(max = 150)
    private String publisher;
    @Min(1400) @Max(2100)
    private Integer publicationYear;
    @Size(max = 50)
    private String language;
    private String description;
    @Size(max = 500)
    @Pattern(regexp = "^(https?://.+)?$", message = "Link ảnh bìa phải bắt đầu bằng http:// hoặc https://")
    private String coverUrl;
}
