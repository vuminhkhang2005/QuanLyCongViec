package com.todo.todolist.service;

import com.todo.todolist.dto.*;

public interface UserService {
    void registerUser(RegisterRequest request);
    void registerUser(RegisterRequest request, String backendUrl, String frontendUrl);
    
    boolean verifyUser(String verificationCode);
    JwtResponse loginUser(LoginRequest request);
    JwtResponse refreshAccessToken(String refreshToken);
    
    void requestPasswordReset(ForgotPasswordRequest request);
    void requestPasswordReset(ForgotPasswordRequest request, String frontendUrl);
    
    void resetPassword(ResetPasswordRequest request);
}
