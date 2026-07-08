package com.todo.todolist.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${app.backend.url}")
    private String backendUrl;

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    @org.springframework.beans.factory.annotation.Value("${app.mail.provider:smtp}")
    private String mailProvider;

    @org.springframework.beans.factory.annotation.Value("${app.mail.google-script.url:}")
    private String googleScriptUrl;

    @org.springframework.beans.factory.annotation.Value("${app.mail.google-script.secret:}")
    private String googleScriptSecret;

    public void sendVerificationEmail(String toEmail, String verificationCode) {
        String verifyUrl = backendUrl + "/api/auth/verify?code=" + verificationCode;
        String subject = "[ZenTask] Xác nhận kích hoạt tài khoản của bạn";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;'>"
                + "<h2 style='color: #7c3aed; text-align: center;'>Chào mừng bạn đến với ZenTask!</h2>"
                + "<p>Cảm ơn bạn đã đăng ký tài khoản tại ZenTask. Để hoàn tất việc kích hoạt tài khoản của mình, vui lòng nhấn vào nút bên dưới:</p>"
                + "<div style='text-align: center; margin: 30px 0;'>"
                + "  <a href='" + verifyUrl + "' style='background: linear-gradient(135deg, #8b5cf6 0%, #a78bfa 100%); color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block; box-shadow: 0 4px 10px rgba(139, 92, 246, 0.2);'>Kích hoạt tài khoản</a>"
                + "</div>"
                + "<p style='color: #64748b; font-size: 0.9em;'>Nếu nút bấm ở trên không hoạt động, bạn có thể copy link sau và dán vào trình duyệt:</p>"
                + "<p style='word-break: break-all; color: #3b82f6;'><a href='" + verifyUrl + "'>" + verifyUrl + "</a></p>"
                + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;'>"
                + "<p style='font-size: 0.8em; color: #94a3b8; text-align: center;'>ZenTask - Ứng dụng Quản lý công việc thông minh</p>"
                + "</div>";

        sendHtmlEmail(toEmail, subject, content);
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = frontendUrl + "/reset-password.html?token=" + resetToken;
        String subject = "[ZenTask] Yêu cầu khôi phục mật khẩu";
        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;'>"
                + "<h2 style='color: #f43f5e; text-align: center;'>Khôi phục mật khẩu ZenTask</h2>"
                + "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn. Vui lòng nhấn vào nút dưới đây để đặt lại mật khẩu mới:</p>"
                + "<div style='text-align: center; margin: 30px 0;'>"
                + "  <a href='" + resetUrl + "' style='background: #f43f5e; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block; box-shadow: 0 4px 10px rgba(244, 63, 94, 0.2);'>Đặt lại mật khẩu</a>"
                + "</div>"
                + "<p style='color: #64748b; font-size: 0.9em;'>Lưu ý: Liên kết khôi phục này có hiệu lực trong vòng 15 phút. Nếu bạn không yêu cầu đặt lại mật khẩu, bạn có thể bỏ qua email này.</p>"
                + "<p style='color: #64748b; font-size: 0.9em;'>Nếu nút bấm ở trên không hoạt động, bạn có thể copy link sau và dán vào trình duyệt:</p>"
                + "<p style='word-break: break-all; color: #3b82f6;'><a href='" + resetUrl + "'>" + resetUrl + "</a></p>"
                + "<hr style='border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;'>"
                + "<p style='font-size: 0.8em; color: #94a3b8; text-align: center;'>ZenTask - Ứng dụng Quản lý công việc thông minh</p>"
                + "</div>";

        sendHtmlEmail(toEmail, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        if ("google_script".equalsIgnoreCase(mailProvider)) {
            sendEmailViaGoogleScript(to, subject, htmlContent);
        } else {
            sendEmailViaSmtp(to, subject, htmlContent);
        }
    }

    private void sendEmailViaSmtp(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("ZenTask <vmkhang2072005@gmail.com>");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Successfully sent SMTP HTML email to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send SMTP email to: {}", to, e);
            throw new RuntimeException("Gửi mail qua SMTP thất bại: " + e.getMessage());
        }
    }

    private void sendEmailViaGoogleScript(String to, String subject, String htmlContent) {
        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            java.util.Map<String, String> payload = new java.util.HashMap<>();
            payload.put("secret", googleScriptSecret);
            payload.put("to", to);
            payload.put("subject", subject);
            payload.put("text", "");
            payload.put("html", htmlContent);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            org.springframework.http.HttpEntity<java.util.Map<String, String>> requestEntity = new org.springframework.http.HttpEntity<>(payload, headers);

            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(googleScriptUrl, requestEntity, String.class);
            int statusCode = response.getStatusCode().value();
            if (!(statusCode >= 200 && statusCode < 300) && !(statusCode >= 300 && statusCode < 400)) {
                throw new RuntimeException("HTTP response code: " + response.getStatusCode());
            }
            log.info("Successfully sent Google Apps Script email to: {} (Status: {})", to, statusCode);
        } catch (Exception e) {
            log.error("Failed to send Google Apps Script email to: {}", to, e);
            throw new RuntimeException("Gửi mail qua Google Apps Script thất bại: " + e.getMessage());
        }
    }
}
