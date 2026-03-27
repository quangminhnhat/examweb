package com.exam.examweb.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    public void sendResetPasswordEmail(String to, String resetLink) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject("Yêu cầu đặt lại mật khẩu - HUTECH Exam");

        String content = "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;'>" +
                "<h2 style='color: #0d6efd;'>Đặt lại mật khẩu của bạn</h2>" +
                "<p>Chào bạn,</p>" +
                "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản HUTECH Exam của bạn. Vui lòng nhấn vào nút bên dưới để tiến hành thay đổi mật khẩu:</p>" +
                "<div style='text-align: center; margin: 30px 0;'>" +
                "<a href='" + resetLink + "' style='background-color: #0d6efd; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;'>ĐẶT LẠI MẬT KHẨU</a>" +
                "</div>" +
                "<p>Link này sẽ hết hạn sau 30 phút.</p>" +
                "<p>Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.</p>" +
                "<hr style='border: none; border-top: 1px solid #eee;'>" +
                "<p style='color: #888; font-size: 12px;'>Đây là email tự động, vui lòng không trả lời.</p>" +
                "</div>";

        helper.setText(content, true);
        mailSender.send(message);
    }
}
