package com.aiagent.model;

import java.time.LocalDateTime;
import java.util.Objects;

public final class ChatMessage {
    public enum Role { USER, AI }

    private final Role role;
    private final String text;
    private final LocalDateTime createdAt;

    public ChatMessage(Role role, String text, LocalDateTime createdAt) {
        this.role = Objects.requireNonNull(role, "role");
        this.text = text == null ? "" : text;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public Role getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

