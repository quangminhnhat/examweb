package com.exam.examweb.controllers;

import com.exam.examweb.entities.Exam;
import com.exam.examweb.entities.ExamAttempt;
import com.exam.examweb.entities.Question;
import com.exam.examweb.services.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamApiController {
    private final ExamService examService;

    @PostMapping
    public ResponseEntity<Exam> createExam(@RequestBody Exam exam) {
        return ResponseEntity.ok(examService.createExam(exam));
    }

    @PostMapping("/{examId}/questions")
    public ResponseEntity<Question> addQuestionToExam(@PathVariable Long examId, @RequestBody Question question) {
        return ResponseEntity.ok(examService.addQuestionToExam(examId, question));
    }

    @GetMapping
    public ResponseEntity<List<Exam>> getExams() {
        return ResponseEntity.ok(examService.getExamsForCurrentUser());
    }

    @PostMapping("/start")
    public ResponseEntity<ExamAttempt> startExam(@RequestBody Map<String, String> payload) {
        String examCode = payload.get("examCode");
        if (examCode == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.ok(examService.startExam(examCode));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/submit/{attemptId}")
    public ResponseEntity<ExamAttempt> submitExam(@PathVariable Long attemptId, @RequestBody Map<String, String> answers) {
        try {
            return ResponseEntity.ok(examService.submitExam(attemptId, answers));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{examId}")
    public ResponseEntity<Void> deleteExam(@PathVariable Long examId) {
        try {
            examService.deleteExam(examId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // API Cập nhật câu hỏi
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<Void> updateQuestion(@PathVariable Long questionId, @RequestBody Question questionDetails) {
        examService.updateQuestion(questionId, questionDetails);
        return ResponseEntity.ok().build();
    }

    // API Xóa câu hỏi
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long questionId) {
        examService.deleteQuestion(questionId);
        return ResponseEntity.ok().build();
    }

    // API Lấy danh sách điểm của học sinh trong 1 kỳ thi
    @GetMapping("/{examId}/results")
    public ResponseEntity<List<ExamAttempt>> getExamResults(@PathVariable Long examId) {
        return ResponseEntity.ok(examService.getAttemptsByExamId(examId));
    }

    // API Đổi trạng thái Đóng/Mở thủ công
    @PatchMapping("/{examId}/toggle-status")
    public ResponseEntity<Boolean> toggleExamStatus(@PathVariable Long examId) {
        return ResponseEntity.ok(examService.toggleExamStatus(examId));
    }

    // API Xuất dữ liệu ra file Excel
    @GetMapping("/{examId}/export")
    public ResponseEntity<byte[]> exportResultsToExcel(@PathVariable Long examId) {
        try {
            byte[] excelContent = examService.exportToExcel(examId);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "Danh_Sach_Diem_Thi.xlsx");
            return new ResponseEntity<>(excelContent, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    // API Hẹn giờ
    @PutMapping("/{examId}/schedule")
    public ResponseEntity<Void> updateSchedule(
            @PathVariable Long examId,
            @RequestBody Map<String, String> schedule) {
        examService.updateSchedule(examId, schedule.get("startTime"), schedule.get("endTime"));
        return ResponseEntity.ok().build();
    }
    // time nộp bài học sinh
    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<Map<String, Object>> getAttempt(@PathVariable Long attemptId) {
        return ResponseEntity.ok(examService.getAttemptDetails(attemptId));
    }
}
