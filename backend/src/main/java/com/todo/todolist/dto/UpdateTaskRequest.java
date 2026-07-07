package com.todo.todolist.dto;

import com.todo.todolist.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateTaskRequest(
        @NotBlank(message = "Title must not be blank")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        String description,

        @NotNull(message = "Completed status must not be null")
        Boolean completed,

        @NotNull(message = "Priority must not be null")
        Priority priority,

        LocalDateTime dueDate
) {}
