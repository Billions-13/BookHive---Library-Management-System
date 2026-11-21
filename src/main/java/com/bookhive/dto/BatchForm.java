package com.bookhive.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchForm {
    @NotNull
    @Min(1)
    @Max(1000)
    private Integer quantity;

    @Size(max = 150)
    private String supplier;

    private String notes;
}
