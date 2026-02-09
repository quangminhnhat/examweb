package com.exam.examweb.controllers;

import com.exam.examweb.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/role-selection")
@RequiredArgsConstructor
public class RoleSelectionController {

    private final UserService userService;

    @GetMapping
    public String showRoleSelectionPage() {
        return "user/role-selection";
    }

    @PostMapping
    public String assignRole(@RequestParam("role") String role, @AuthenticationPrincipal Object principal) {
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else if (principal instanceof OAuth2User) {
            // For Google OAuth, the email is usually the username we saved
            username = ((OAuth2User) principal).getAttribute("email");
        } else {
            return "redirect:/login?error";
        }

        userService.assignRoleToUser(username, role);
        return "redirect:/";
    }
}
