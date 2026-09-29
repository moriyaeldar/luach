package io.github.moriyaeldar.luach.tasks.service;

import java.util.UUID;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(UUID id) {
        super("Task not found: " + id);
    }
}
