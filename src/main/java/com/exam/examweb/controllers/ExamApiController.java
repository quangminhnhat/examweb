package com.exam.examweb.controllers;

import com.exam.examweb.entities.Exam;
import com.exam.examweb.entities.ExamAttempt;
import com.exam.examweb.entities.Question;
import com.exam.examweb.services.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<ExamAttempt> submitExam(@PathVariable Long attemptId, @RequestBody Map<Long, Character> answers) {
        try {
            return ResponseEntity.ok(examService.submitExam(attemptId, answers));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
