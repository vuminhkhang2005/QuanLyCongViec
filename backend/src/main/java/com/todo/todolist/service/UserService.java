package com.todo.todolist.service;

import com.todo.todolist.dto.*;

public interface UserService {
    void registerUser(RegisterRequest request);
    boolean verifyUser(String verificationCode);
    JwtResponse loginUser(LoginRequest request);
    JwtResponse refreshAccessToken(String refreshToken);
    void requestPasswordReset(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
