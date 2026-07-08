package com.todo.todolist.controller;

import com.todo.todolist.dto.*;
import com.todo.todolist.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // CORS toàn cầu cho controller này
public class AuthController {

    private final UserService userService;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận tài khoản."
        ));
    }

    @GetMapping("/verify")
    public RedirectView verifyEmail(@RequestParam("code") String code) {
        boolean verified = userService.verifyUser(code);
        if (verified) {
            return new RedirectView(frontendUrl + "/login.html?verified=true");
        } else {
            return new RedirectView(frontendUrl + "/login.html?error=verification_failed");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse response = userService.loginUser(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest request) {
        JwtResponse response = userService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.requestPasswordReset(request);
        return ResponseEntity.ok(Map.of(
                "message", "Liên kết khôi phục mật khẩu đã được gửi tới email của bạn!"
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
        return ResponseEntity.ok(Map.of(
                "message", "Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới."
        ));
    }
}
