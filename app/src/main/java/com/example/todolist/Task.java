package com.example.todolist;

import java.io.Serializable;
import java.util.UUID;

public class Task implements Serializable {
    private String id;
    private String title;
    private boolean isCompleted;
    private long createdAt;

    public Task(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.isCompleted = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
