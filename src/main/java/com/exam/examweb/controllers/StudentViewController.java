package com.exam.examweb.controllers;


import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student")
public class StudentViewController {

    @GetMapping("/dashboard")
    public String studentDashboard() {
        return "student/dashboard";
    }

    // 2. CHỈ GIỮ LẠI MỘT HÀM DUY NHẤT:
    @GetMapping("/take-exam/{attemptId}")
    public String takeExamPage(@PathVariable("attemptId") Long attemptId, Model model) {
        model.addAttribute("attemptId", attemptId);
        return "student/take-exam";
    }

    // 3. THÊM ROUTE CHO TRANG KẾT QUẢ (Để nộp bài xong có chỗ mà xem):
    @GetMapping("/result/{attemptId}")
    public String examResultPage(@PathVariable("attemptId") Long attemptId, Model model) {
        model.addAttribute("attemptId", attemptId);
        return "student/result";
    }
}