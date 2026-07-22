package com.charity.app.payload;

import jakarta.validation.constraints.NotBlank;

public class CategoryRequest {
    @NotBlank(message = "نام دسته‌بندی الزامی است")
    private String name;
    private String description;
    private String iconUrl;
    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
