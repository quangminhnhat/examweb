package com.exam.examweb.controllers;

import com.exam.examweb.entities.User;
import com.exam.examweb.services.MailService;
import com.exam.examweb.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final UserService userService;
    private final MailService mailService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "user/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email, HttpServletRequest request, Model model) {
        try {
            String token = userService.createResetPasswordToken(email);
            String resetLink = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/reset-password?token=" + token;
            
            mailService.sendResetPasswordEmail(email, resetLink);
            model.addAttribute("message", "Chúng tôi đã gửi một liên kết đặt lại mật khẩu đến email của bạn.");
        } catch (UsernameNotFoundException e) {
            model.addAttribute("error", "Không tìm thấy người dùng với email này.");
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi gửi email: " + e.getMessage() + ". Vui lòng kiểm tra cấu hình SMTP.");
        }
        return "user/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        Optional<User> user = userService.findByResetPasswordToken(token);
        if (user.isEmpty()) {
            model.addAttribute("error", "Token không hợp lệ hoặc đã hết hạn.");
            return "user/forgot-password";
        }
        model.addAttribute("token", token);
        return "user/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token, @RequestParam("password") String password, Model model) {
        Optional<User> user = userService.findByResetPasswordToken(token);
        if (user.isEmpty()) {
            model.addAttribute("error", "Token không hợp lệ hoặc đã hết hạn.");
            return "user/forgot-password";
        }
        
        userService.updatePassword(user.get(), password);
        model.addAttribute("message", "Bạn đã đổi mật khẩu thành công. Vui lòng đăng nhập lại.");
        return "user/login";
    }
}
