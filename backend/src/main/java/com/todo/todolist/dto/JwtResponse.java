package com.todo.todolist.dto;

public record JwtResponse(
        String accessToken,
        String refreshToken,
        String email
) {}
