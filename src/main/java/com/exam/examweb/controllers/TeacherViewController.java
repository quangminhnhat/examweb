package com.exam.examweb.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/teacher")
public class TeacherViewController {

    @GetMapping("/classes")
    public String manageClassesPage() {
        return "teacher/classes";
    }

    // Trang chi tiết của 1 lớp học
    @GetMapping("/classes/{id}")
    public String classDetailPage(@PathVariable("id") Long id, Model model) {
        // Truyền classId sang giao diện HTML để Javascript lấy ra dùng gọi API
        model.addAttribute("classId", id);
        return "teacher/class-detail";
    }
    // Trang quản lý câu hỏi của 1 kỳ thi cụ thể
    @GetMapping("/exams/{id}")
    public String manageQuestionsPage(@PathVariable("id") Long id, Model model) {
        // Truyền examId sang giao diện HTML
        model.addAttribute("examId", id);
        return "teacher/exam-detail";
    }
}