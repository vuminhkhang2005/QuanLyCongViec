package com.todo.todolist.dto;

import com.todo.todolist.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateTaskRequest(
        @NotBlank(message = "Title must not be blank")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        String description,

        Priority priority,

        LocalDateTime dueDate
) {}
