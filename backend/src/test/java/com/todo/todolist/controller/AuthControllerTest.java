package com.todo.todolist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.todolist.dto.*;
import com.todo.todolist.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;

@WebMvcTest(controllers = AuthController.class)
@Import(com.todo.todolist.security.SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private com.todo.todolist.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.todo.todolist.security.CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_Success_ShouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser@gmail.com", "password123");
        doNothing().when(userService).registerUser(any(RegisterRequest.class), any(), any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", is("Đăng ký thành công! Vui lòng kiểm tra email để xác nhận tài khoản.")));

        verify(userService, times(1)).registerUser(any(RegisterRequest.class), any(), any());
    }

    @Test
    void verifyEmail_Success_ShouldRedirectToLoginWithVerifiedTrue() throws Exception {
        when(userService.verifyUser("verify-code")).thenReturn(true);

        mockMvc.perform(get("/api/auth/verify").param("code", "verify-code"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/login.html?verified=true"));

        verify(userService, times(1)).verifyUser("verify-code");
    }

    @Test
    void verifyEmail_Failed_ShouldRedirectToLoginWithError() throws Exception {
        when(userService.verifyUser("invalid-code")).thenReturn(false);

        mockMvc.perform(get("/api/auth/verify").param("code", "invalid-code"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost:3000/login.html?error=verification_failed"));

        verify(userService, times(1)).verifyUser("invalid-code");
    }

    @Test
    void login_Success_ShouldReturnJwtResponse() throws Exception {
        LoginRequest request = new LoginRequest("zentask@gmail.com", "password123");
        JwtResponse response = new JwtResponse("access_token", "refresh_token", "zentask@gmail.com");

        when(userService.loginUser(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("access_token")))
                .andExpect(jsonPath("$.refreshToken", is("refresh_token")))
                .andExpect(jsonPath("$.email", is("zentask@gmail.com")));

        verify(userService, times(1)).loginUser(any(LoginRequest.class));
    }

    @Test
    void refresh_Success_ShouldReturnNewJwtResponse() throws Exception {
        RefreshRequest request = new RefreshRequest("refresh_token");
        JwtResponse response = new JwtResponse("new_access_token", "new_refresh_token", "zentask@gmail.com");

        when(userService.refreshAccessToken("refresh_token")).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("new_access_token")))
                .andExpect(jsonPath("$.refreshToken", is("new_refresh_token")));

        verify(userService, times(1)).refreshAccessToken("refresh_token");
    }

    @Test
    void forgotPassword_Success_ShouldReturn200() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("zentask@gmail.com");
        doNothing().when(userService).requestPasswordReset(any(ForgotPasswordRequest.class), any());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Liên kết khôi phục mật khẩu đã được gửi tới email của bạn!")));

        verify(userService, times(1)).requestPasswordReset(any(ForgotPasswordRequest.class), any());
    }

    @Test
    void resetPassword_Success_ShouldReturn200() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("reset-token", "newpassword123");
        doNothing().when(userService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Đặt lại mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.")));

        verify(userService, times(1)).resetPassword(any(ResetPasswordRequest.class));
    }
}
