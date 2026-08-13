package com.finance.dashboard.dto;

import com.finance.dashboard.model.Category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    @NotBlank(message = "Icon is required")
    private String icon;

    @NotBlank(message = "Color is required")
    private String color;

    @NotNull(message = "Category type is required")
    private CategoryType type;
}