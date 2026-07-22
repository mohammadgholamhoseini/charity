package com.charity.app.payload;

import jakarta.validation.constraints.NotBlank;

public class NameRequest {
    @NotBlank(message = "نام الزامی است")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
