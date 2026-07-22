package com.charity.app.payload;

import com.charity.app.model.Notice;

import jakarta.validation.constraints.NotBlank;

public class NoticeRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    private Notice.Position position;

    private boolean active = true;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Notice.Position getPosition() { return position; }
    public void setPosition(Notice.Position position) { this.position = position; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
