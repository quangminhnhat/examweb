package com.exam.examweb.controllers;

import com.exam.examweb.entities.Exam;
import com.exam.examweb.entities.User;
import com.exam.examweb.services.ExamService;
import com.exam.examweb.services.UserService;
import com.exam.examweb.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('admin')")
public class AdminController {
    private final ExamService examService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("exams", examService.getExamsForCurrentUser());
        return "admin/dashboard";
    }

    @GetMapping("/exams/manage")
    public String manageExams(Model model) {
        model.addAttribute("exams", examService.getExamsForCurrentUser());
        return "admin/manage-exams";
    }

    @GetMapping("/users/manage")
    public String manageUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/manage-users";
    }
}
