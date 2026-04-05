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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

    // API Cập nhật tên kỳ thi
    @PutMapping("/{examId}/title")
    public ResponseEntity<Void> updateExamTitle(@PathVariable Long examId, @RequestBody Map<String, String> payload) {
        String newTitle = payload.get("examTitle");
        if (newTitle == null || newTitle.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // We'll need to add this method to ExamService
            examService.updateExamTitle(examId, newTitle.trim());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
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

    // API Import entire exam from Excel
    @PostMapping("/import-exam")
    public ResponseEntity<Map<String, Object>> importEntireExamFromExcel(@RequestParam("file") MultipartFile file, @RequestParam(value = "classId", required = false) Long classId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File is empty"));
            }

            String contentType = file.getContentType();
            if (!"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(contentType) &&
                !"application/vnd.ms-excel".equals(contentType)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid file type. Please upload an Excel file."));
            }

            Map<String, Object> result = examService.importEntireExamFromExcel(file.getInputStream(), classId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Successfully imported exam: " + result.get("examTitle") + " with " + result.get("questionCount") + " questions",
                "examId", result.get("examId"),
                "questionCount", result.get("questionCount")
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Import failed: " + e.getMessage()));
        }
    }

    // API Download Excel template
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadExamTemplate() {
        try {
            byte[] templateContent = loadExamImportTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "exam_template.xlsx");
            return new ResponseEntity<>(templateContent, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private byte[] loadExamImportTemplate() throws IOException {
        Path templatePath = Paths.get("exam_template.xlsx");
        if (Files.exists(templatePath)) {
            return Files.readAllBytes(templatePath);
        }

        Path staticTemplatePath = Paths.get("src", "main", "resources", "static", "exam_template.xlsx");
        if (Files.exists(staticTemplatePath)) {
            return Files.readAllBytes(staticTemplatePath);
        }

        return createExamImportTemplate();
    }

    private byte[] createExamImportTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Đề thi");

            // Exam metadata row (row 0)
            Row metadataRow = sheet.createRow(0);
            metadataRow.createCell(0).setCellValue("Tên đề thi");
            metadataRow.createCell(1).setCellValue("Thời gian (phút)");
            metadataRow.createCell(2).setCellValue("Tổng điểm");

            // Sample exam metadata (row 1)
            Row sampleMetadataRow = sheet.createRow(1);
            sampleMetadataRow.createCell(0).setCellValue("Kiểm tra cuối kỳ Toán cao cấp");
            sampleMetadataRow.createCell(1).setCellValue("90");
            sampleMetadataRow.createCell(2).setCellValue("100");

            // Empty row (row 2)
            sheet.createRow(2);

            // Question headers (row 3)
            Row headerRow = sheet.createRow(3);
            headerRow.createCell(0).setCellValue("Nội dung câu hỏi");
            headerRow.createCell(1).setCellValue("Đáp án A");
            headerRow.createCell(2).setCellValue("Đáp án B");
            headerRow.createCell(3).setCellValue("Đáp án C");
            headerRow.createCell(4).setCellValue("Đáp án D");
            headerRow.createCell(5).setCellValue("Đáp án đúng (A/B/C/D)");
            headerRow.createCell(6).setCellValue("Điểm số");

            // Sample questions starting from row 4
            Row sampleRow1 = sheet.createRow(4);
            sampleRow1.createCell(0).setCellValue("1 + 1 bằng bao nhiêu?");
            sampleRow1.createCell(1).setCellValue("1");
            sampleRow1.createCell(2).setCellValue("2");
            sampleRow1.createCell(3).setCellValue("3");
            sampleRow1.createCell(4).setCellValue("4");
            sampleRow1.createCell(5).setCellValue("B");
            sampleRow1.createCell(6).setCellValue("1");

            Row sampleRow2 = sheet.createRow(5);
            sampleRow2.createCell(0).setCellValue("Thủ đô của Việt Nam là?");
            sampleRow2.createCell(1).setCellValue("Hà Nội");
            sampleRow2.createCell(2).setCellValue("TP.HCM");
            sampleRow2.createCell(3).setCellValue("Đà Nẵng");
            sampleRow2.createCell(4).setCellValue("Cần Thơ");
            sampleRow2.createCell(5).setCellValue("A");
            sampleRow2.createCell(6).setCellValue("2");

            // Auto-size columns
            for (int i = 0; i < 7; i++) {
                sheet.autoSizeColumn(i);
            }

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
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
