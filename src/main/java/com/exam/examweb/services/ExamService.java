package com.exam.examweb.services;

import com.exam.examweb.entities.*;
import com.exam.examweb.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class ExamService {
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final IUserRepository userRepository;
    private final ClassRepository classRepository;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new AccessDeniedException("User not authenticated");
    }

    @Transactional
    public Exam createExam(Exam exam) {
        User currentUser = getCurrentUser();
        boolean isTeacher = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher"));

        if (!isTeacher) {
            throw new AccessDeniedException("Only teachers can create exams.");
        }

        User managedTeacher = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new IllegalStateException("Current teacher not found in database"));
        exam.setTeacher(managedTeacher);

        if (exam.getExamCode() == null || exam.getExamCode().isEmpty()) {
            exam.setExamCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        if (exam.getClassEntity() != null && exam.getClassEntity().getId() != null) {
            ClassEntity classEntity = classRepository.findById(exam.getClassEntity().getId())
                .orElseThrow(() -> new IllegalArgumentException("Class not found."));
            exam.setClassEntity(classEntity);
        }

        return examRepository.save(exam);
    }

    @Transactional
    public Question addQuestionToExam(Long examId, Question question) {
        User currentUser = getCurrentUser();
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("Exam not found."));

        if (!exam.getTeacher().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only add questions to your own exams.");
        }

        question.setExam(exam);
        return questionRepository.save(question);
    }

    @Transactional(readOnly = true)
    public List<Exam> getExamsForCurrentUser() {
        User currentUser = getCurrentUser();
        if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher"))) {
            return examRepository.findByTeacher(currentUser);
        } else if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("student"))) {
            return examRepository.findByClassEntity_Students_Id(currentUser.getId());
        }
        return List.of();
    }

    @Transactional
    public ExamAttempt startExam(String examCode) {
        User currentUser = getCurrentUser();
        Exam exam = examRepository.findByExamCode(examCode)
            .orElseThrow(() -> new IllegalArgumentException("Invalid exam code."));

        if (exam.getIsOpen() == null || !exam.getIsOpen()) {
            throw new IllegalArgumentException("Kỳ thi hiện đang bị khóa bởi Giáo viên.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())) {
            throw new IllegalArgumentException("Chưa đến thời gian làm bài.");
        }
        if (exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new IllegalArgumentException("Kỳ thi đã kết thúc.");
        }

        boolean isEnrolled = exam.getClassEntity() == null || exam.getClassEntity().getStudents().stream()
            .anyMatch(s -> s.getId().equals(currentUser.getId()));

        if (!isEnrolled) {
            throw new AccessDeniedException("You are not enrolled in the class for this exam.");
        }
        
        User managedStudent = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new IllegalStateException("Current student not found in database"));

        Optional<ExamAttempt> existingAttempt = examAttemptRepository.findByExamAndStudent(exam, managedStudent);
        if (existingAttempt.isPresent()) {
            return existingAttempt.get(); // Or throw exception if re-attempts are not allowed
        }

        ExamAttempt newAttempt = ExamAttempt.builder()
            .exam(exam)
            .student(managedStudent)
            .build();
        
        return examAttemptRepository.save(newAttempt);
    }

    @Transactional
    public ExamAttempt submitExam(Long attemptId, Map<Long, Character> answers) {
        User currentUser = getCurrentUser();
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new IllegalArgumentException("Exam attempt not found."));

        if (!attempt.getStudent().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only submit your own exam attempt.");
        }

        int totalScore = 0;
        for (Map.Entry<Long, Character> entry : answers.entrySet()) {
            Long questionId = entry.getKey();
            Character selectedOption = entry.getValue();

            Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + questionId));

            boolean isCorrect = question.getCorrectOption() == selectedOption;
            if (isCorrect) {
                totalScore += question.getScore();
            }

            Answer answer = Answer.builder()
                .attempt(attempt)
                .question(question)
                .selectedOption(selectedOption)
                .isCorrect(isCorrect)
                .build();
            attempt.getAnswers().add(answer);
        }

        attempt.setScore(totalScore);
        attempt.setSubmittedAt(java.time.LocalDateTime.now());
        return examAttemptRepository.save(attempt);
    }

    @Transactional
    public Question updateQuestion(Long questionId, Question questionDetails) {
        User currentUser = getCurrentUser();
        Question existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));

        // Kiểm tra bảo mật: Chỉ giáo viên tạo kỳ thi mới được sửa câu hỏi
        if (!existingQuestion.getExam().getTeacher().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only update questions in your own exams.");
        }

        // Cập nhật các trường dữ liệu
        existingQuestion.setContent(questionDetails.getContent());
        existingQuestion.setOptionA(questionDetails.getOptionA());
        existingQuestion.setOptionB(questionDetails.getOptionB());
        existingQuestion.setOptionC(questionDetails.getOptionC());
        existingQuestion.setOptionD(questionDetails.getOptionD());
        existingQuestion.setCorrectOption(questionDetails.getCorrectOption());

        return questionRepository.save(existingQuestion);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        User currentUser = getCurrentUser();
        Question existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));

        // Kiểm tra bảo mật
        if (!existingQuestion.getExam().getTeacher().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only delete questions in your own exams.");
        }

        questionRepository.delete(existingQuestion);
    }

    public List<ExamAttempt> getAttemptsByExamId(Long examId) {
        return examAttemptRepository.findByExamId(examId);
    }

    @Transactional
    public boolean toggleExamStatus(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kỳ thi"));
        boolean currentStatus = Boolean.TRUE.equals(exam.getIsOpen());
        exam.setIsOpen(!currentStatus);

        examRepository.save(exam);
        return exam.getIsOpen();
    }
    //excel
    public byte[] exportToExcel(Long examId) throws java.io.IOException {
        List<ExamAttempt> attempts = examAttemptRepository.findByExamId(examId);

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Kết quả thi");

            // Tạo dòng tiêu đề (Header)
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("STT");
            headerRow.createCell(1).setCellValue("Tên Sinh Viên");
            headerRow.createCell(2).setCellValue("Điểm Số");
            headerRow.createCell(3).setCellValue("Thời Gian Nộp");

            // Đổ dữ liệu từ danh sách vào các dòng
            int rowIdx = 1;
            for (ExamAttempt attempt : attempts) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(attempt.getStudent().getFullName()); // Hoặc getUsername()
                row.createCell(2).setCellValue(attempt.getScore());
                row.createCell(3).setCellValue(attempt.getSubmittedAt().toString());
            }

            // Xuất ra mảng byte
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional
    public void updateSchedule(Long examId, String startTimeStr, String endTimeStr) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        if (startTimeStr != null && !startTimeStr.isEmpty()) {
            exam.setStartTime(LocalDateTime.parse(startTimeStr, formatter));
        }
        if (endTimeStr != null && !endTimeStr.isEmpty()) {
            exam.setEndTime(LocalDateTime.parse(endTimeStr, formatter));
        }

        examRepository.save(exam);
    }

    public Map<String, Object> getAttemptDetails(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt thi"));

        Exam exam = attempt.getExam();
        LocalDateTime startTime = attempt.getCreatedAt();
        LocalDateTime endTime = startTime.plusMinutes(exam.getDuration());
        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();

        // THUẬT TOÁN TÍNH ĐIỂM
        int totalQuestions = exam.getQuestions().size();

        // Đếm số câu trả lời đúng từ List<Answer>
        long correctAnswersCount = attempt.getAnswers().stream()
                .filter(answer -> answer.isCorrect())
                .count();

        // Quy đổi ra thang điểm 10 (Ví dụ: 3/4 câu -> 7.5 điểm)
        double finalScore10 = 0.0;
        if (totalQuestions > 0) {
            finalScore10 = ((double) correctAnswersCount / totalQuestions) * 10.0;
        }

        // Làm tròn đến 2 chữ số thập phân (Ví dụ: 8.3333 -> 8.33)
        finalScore10 = Math.round(finalScore10 * 100.0) / 100.0;

        // Đóng gói dữ liệu gửi về Frontend
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("exam", exam);
        response.put("attempt", attempt);
        response.put("remainingSeconds", Math.max(0, remainingSeconds));

        // Gửi kèm kết quả đã tính toán chính xác 100%
        response.put("correctCount", correctAnswersCount);
        response.put("finalScore", finalScore10);

        return response;
    }
}
