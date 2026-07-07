package com.todo.todolist.service;

import com.todo.todolist.dto.*;
import com.todo.todolist.entity.User;
import com.todo.todolist.exception.ResourceNotFoundException;
import com.todo.todolist.repository.UserRepository;
import com.todo.todolist.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .email("zentask@gmail.com")
                .password("hashed_password")
                .enabled(true)
                .verificationCode("verify-code")
                .passwordResetToken("reset-token")
                .passwordResetTokenExpiry(LocalDateTime.now().plusMinutes(10))
                .refreshToken("refresh-token")
                .refreshTokenExpiry(LocalDateTime.now().plusDays(5))
                .build();
    }

    @Test
    void registerUser_Success_ShouldSaveAndSendEmail() {
        RegisterRequest request = new RegisterRequest("newuser@gmail.com", "password123");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        userService.registerUser(request);

        verify(userRepository, times(1)).findByEmail(request.email());
        verify(passwordEncoder, times(1)).encode(request.password());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendVerificationEmail(eq("newuser@gmail.com"), anyString());
    }

    @Test
    void registerUser_EmailAlreadyExists_ShouldThrowException() {
        RegisterRequest request = new RegisterRequest("zentask@gmail.com", "password123");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(sampleUser));

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(request));

        verify(userRepository, times(1)).findByEmail(request.email());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void verifyUser_Success_ShouldEnableUser() {
        sampleUser.setEnabled(false);
        when(userRepository.findByVerificationCode("verify-code")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        boolean result = userService.verifyUser("verify-code");

        assertTrue(result);
        assertTrue(sampleUser.isEnabled());
        assertNull(sampleUser.getVerificationCode());
        verify(userRepository, times(1)).findByVerificationCode("verify-code");
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void verifyUser_InvalidCode_ShouldReturnFalse() {
        when(userRepository.findByVerificationCode("invalid-code")).thenReturn(Optional.empty());

        boolean result = userService.verifyUser("invalid-code");

        assertFalse(result);
        verify(userRepository, times(1)).findByVerificationCode("invalid-code");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginUser_Success_ShouldReturnTokens() {
        LoginRequest request = new LoginRequest("zentask@gmail.com", "password123");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(request.password(), sampleUser.getPassword())).thenReturn(true);
        when(tokenProvider.generateAccessToken(sampleUser.getEmail())).thenReturn("access_token");
        when(tokenProvider.generateRefreshToken(sampleUser.getEmail())).thenReturn("refresh_token");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        JwtResponse response = userService.loginUser(request);

        assertNotNull(response);
        assertEquals("access_token", response.accessToken());
        assertEquals("refresh_token", response.refreshToken());
        assertEquals("zentask@gmail.com", response.email());
        verify(userRepository, times(1)).findByEmail(request.email());
        verify(passwordEncoder, times(1)).matches(request.password(), sampleUser.getPassword());
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void loginUser_UserNotEnabled_ShouldThrowException() {
        LoginRequest request = new LoginRequest("zentask@gmail.com", "password123");
        sampleUser.setEnabled(false);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(sampleUser));

        assertThrows(IllegalArgumentException.class, () -> userService.loginUser(request));

        verify(userRepository, times(1)).findByEmail(request.email());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void loginUser_WrongPassword_ShouldThrowException() {
        LoginRequest request = new LoginRequest("zentask@gmail.com", "wrong_pass");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(request.password(), sampleUser.getPassword())).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.loginUser(request));

        verify(userRepository, times(1)).findByEmail(request.email());
        verify(passwordEncoder, times(1)).matches(request.password(), sampleUser.getPassword());
        verify(tokenProvider, never()).generateAccessToken(anyString());
    }

    @Test
    void refreshAccessToken_Success_ShouldReturnNewTokens() {
        when(userRepository.findByRefreshToken("refresh-token")).thenReturn(Optional.of(sampleUser));
        when(tokenProvider.validateToken("refresh-token")).thenReturn(true);
        when(tokenProvider.generateAccessToken(sampleUser.getEmail())).thenReturn("new_access_token");
        when(tokenProvider.generateRefreshToken(sampleUser.getEmail())).thenReturn("new_refresh_token");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        JwtResponse response = userService.refreshAccessToken("refresh-token");

        assertNotNull(response);
        assertEquals("new_access_token", response.accessToken());
        assertEquals("new_refresh_token", response.refreshToken());
        verify(userRepository, times(1)).findByRefreshToken("refresh-token");
        verify(tokenProvider, times(1)).validateToken("refresh-token");
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void refreshAccessToken_ExpiredToken_ShouldThrowException() {
        sampleUser.setRefreshTokenExpiry(LocalDateTime.now().minusMinutes(1)); // Đã hết hạn

        when(userRepository.findByRefreshToken("refresh-token")).thenReturn(Optional.of(sampleUser));

        assertThrows(IllegalArgumentException.class, () -> userService.refreshAccessToken("refresh-token"));

        verify(userRepository, times(1)).findByRefreshToken("refresh-token");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void requestPasswordReset_Success_ShouldSaveAndSendEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("zentask@gmail.com");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        userService.requestPasswordReset(request);

        assertNotNull(sampleUser.getPasswordResetToken());
        assertNotNull(sampleUser.getPasswordResetTokenExpiry());
        verify(userRepository, times(1)).findByEmail(request.email());
        verify(userRepository, times(1)).save(sampleUser);
        verify(emailService, times(1)).sendPasswordResetEmail(eq("zentask@gmail.com"), anyString());
    }

    @Test
    void requestPasswordReset_UserNotFound_ShouldThrowException() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("notfound@gmail.com");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.requestPasswordReset(request));

        verify(userRepository, times(1)).findByEmail(request.email());
        verify(userRepository, never()).save(any(User.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_Success_ShouldUpdatePasswordAndClearToken() {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "new_password_123");

        when(userRepository.findByPasswordResetToken("reset-token")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("new_password_123")).thenReturn("new_hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        userService.resetPassword(request);

        assertEquals("new_hashed_password", sampleUser.getPassword());
        assertNull(sampleUser.getPasswordResetToken());
        assertNull(sampleUser.getPasswordResetTokenExpiry());
        verify(userRepository, times(1)).findByPasswordResetToken("reset-token");
        verify(passwordEncoder, times(1)).encode("new_password_123");
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    void resetPassword_TokenExpired_ShouldThrowException() {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "new_password_123");
        sampleUser.setPasswordResetTokenExpiry(LocalDateTime.now().minusMinutes(1)); // Đã hết hạn

        when(userRepository.findByPasswordResetToken("reset-token")).thenReturn(Optional.of(sampleUser));

        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword(request));

        verify(userRepository, times(1)).findByPasswordResetToken("reset-token");
        verify(userRepository, never()).save(any(User.class));
    }
}
