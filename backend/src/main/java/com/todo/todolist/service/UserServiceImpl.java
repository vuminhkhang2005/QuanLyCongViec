package com.todo.todolist.service;

import com.todo.todolist.dto.*;
import com.todo.todolist.entity.User;
import com.todo.todolist.exception.ResourceNotFoundException;
import com.todo.todolist.repository.UserRepository;
import com.todo.todolist.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;

    @Override
    @Transactional
    public void registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email đã được đăng ký trong hệ thống!");
        }

        String verificationCode = UUID.randomUUID().toString();
        User user = User.builder()
                .email(request.email().trim())
                .password(passwordEncoder.encode(request.password()))
                .enabled(false)
                .verificationCode(verificationCode)
                .verificationCodeSentAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        
        // Gửi email xác thực kích hoạt tài khoản bằng bản cũ
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);
        log.info("Registered user: {}. Verification email sent.", user.getEmail());
    }

    @Override
    @Transactional
    public void registerUser(RegisterRequest request, String backendUrl, String frontendUrl) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email đã được đăng ký trong hệ thống!");
        }

        String verificationCode = UUID.randomUUID().toString();
        User user = User.builder()
                .email(request.email().trim())
                .password(passwordEncoder.encode(request.password()))
                .enabled(false)
                .verificationCode(verificationCode)
                .verificationCodeSentAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
        
        // Gửi email xác thực kích hoạt tài khoản
        emailService.sendVerificationEmail(user.getEmail(), verificationCode, backendUrl, frontendUrl);
        log.info("Registered user: {}. Verification email sent.", user.getEmail());
    }

    @Override
    @Transactional
    public boolean verifyUser(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode)
                .orElse(null);

        if (user == null) {
            return false;
        }

        user.setEnabled(true);
        user.setVerificationCode(null); // Clear code để tránh re-use
        userRepository.save(user);
        log.info("Activated account for user: {}", user.getEmail());
        return true;
    }

    @Override
    @Transactional
    public JwtResponse loginUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("Tài khoản chưa được kích hoạt! Vui lòng kiểm tra email xác nhận.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Tài khoản hoặc mật khẩu không chính xác!");
        }

        // Tạo access token & refresh token
        String accessToken = tokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        // Lưu refresh token vào DB (thời hạn 7 ngày)
        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(user);

        return new JwtResponse(accessToken, refreshToken, user.getEmail());
    }

    @Override
    @Transactional
    public JwtResponse refreshAccessToken(String refreshToken) {
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token không hợp lệ hoặc đã bị thu hồi!"));

        if (user.getRefreshTokenExpiry().isBefore(LocalDateTime.now()) || !tokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Refresh token đã hết hạn! Vui lòng đăng nhập lại.");
        }

        // Tạo cặp token mới (Rotated Refresh Token)
        String newAccessToken = tokenProvider.generateAccessToken(user.getEmail());
        String newRefreshToken = tokenProvider.generateRefreshToken(user.getEmail());

        // Cập nhật refresh token mới
        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(user);

        return new JwtResponse(newAccessToken, newRefreshToken, user.getEmail());
    }

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email này!"));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Hạn 15 phút
        userRepository.save(user);

        // Gửi mail hướng dẫn reset password bằng bản cũ
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        log.info("Password reset request for: {}. Reset email sent.", user.getEmail());
    }

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request, String frontendUrl) {
        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email này!"));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(15)); // Hạn 15 phút
        userRepository.save(user);

        // Gửi mail hướng dẫn reset password
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken, frontendUrl);
        log.info("Password reset request for: {}. Reset email sent.", user.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Token đặt lại mật khẩu không hợp lệ!"));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token đặt lại mật khẩu đã hết hạn!");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
        
        log.info("Successfully reset password for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(ResendVerificationRequest request) {
        resendVerificationEmail(request, null, null);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(ResendVerificationRequest request, String backendUrl, String frontendUrl) {
        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản với email này!"));

        if (user.isEnabled()) {
            throw new IllegalArgumentException("Tài khoản đã được kích hoạt trước đó!");
        }

        // Kiểm tra giới hạn 1 tiếng
        if (user.getVerificationCodeSentAt() != null && 
            user.getVerificationCodeSentAt().plusHours(1).isAfter(LocalDateTime.now())) {
            
            java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), user.getVerificationCodeSentAt().plusHours(1));
            long minutesLeft = duration.toMinutes();
            throw new IllegalArgumentException("Bạn chỉ có thể gửi lại mã xác nhận sau " + minutesLeft + " phút nữa!");
        }

        // Tạo mã xác thực mới và cập nhật thời điểm gửi
        String verificationCode = UUID.randomUUID().toString();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeSentAt(LocalDateTime.now());
        userRepository.save(user);

        // Gửi email
        emailService.sendVerificationEmail(user.getEmail(), verificationCode, backendUrl, frontendUrl);
        log.info("Resent verification email to: {}", user.getEmail());
    }
}
