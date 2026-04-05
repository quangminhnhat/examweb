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
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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

    private boolean isAdmin(User user) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"));
    }

    private boolean isTeacher(User user) {
        return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher"));
    }

    private boolean hasManagePermission(Exam exam) {
        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) return true;
        return exam.getTeacher().getId().equals(currentUser.getId());
    }

    @Transactional
    public Exam createExam(Exam exam) {
        User currentUser = getCurrentUser();
        boolean adminFlag = isAdmin(currentUser);
        boolean teacherFlag = isTeacher(currentUser);

        if (!adminFlag && !teacherFlag) {
            throw new AccessDeniedException("Only teachers or admins can create exams.");
        }

        if (adminFlag) {
            if (exam.getTeacher() == null || exam.getTeacher().getId() == null) {
                throw new IllegalArgumentException("Admin must specify a teacher for the exam.");
            }
            User teacher = userRepository.findById(exam.getTeacher().getId())
                .orElseThrow(() -> new IllegalArgumentException("Specified teacher not found."));
            exam.setTeacher(teacher);
        } else {
            User managedTeacher = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Current teacher not found in database"));
            exam.setTeacher(managedTeacher);
        }

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
    public void deleteExam(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        
        if (!hasManagePermission(exam)) {
            throw new AccessDeniedException("Permission denied.");
        }
        examRepository.delete(exam);
    }

    @Transactional
    public Question addQuestionToExam(Long examId, Question question) {
        Exam exam = examRepository.findById(examId)
            .orElseThrow(() -> new IllegalArgumentException("Exam not found."));

        if (!hasManagePermission(exam)) {
            throw new AccessDeniedException("You do not have permission to add questions to this exam.");
        }

        question.setExam(exam);
        return questionRepository.save(question);
    }

    @Transactional(readOnly = true)
    public List<Exam> getExamsForCurrentUser() {
        User currentUser = getCurrentUser();
        if (isAdmin(currentUser)) {
            return examRepository.findAll();
        } else if (isTeacher(currentUser)) {
            return examRepository.findByTeacher(currentUser);
        } else if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("student"))) {
            List<Exam> studentExams = examRepository.findByClassEntity_Students_Id(currentUser.getId());
            LocalDateTime now = LocalDateTime.now();
            return studentExams.stream().filter(exam -> {
                boolean isManuallyOpen = exam.getIsOpen();
                boolean isScheduled = exam.getStartTime() != null;
                boolean hasStarted = isScheduled && !now.isBefore(exam.getStartTime());
                boolean hasEnded = exam.getEndTime() != null && now.isAfter(exam.getEndTime());

                return isManuallyOpen || (isScheduled && hasStarted && !hasEnded);
            }).collect(java.util.stream.Collectors.toList());
        }
        return List.of();
    }

    @Transactional
    public ExamAttempt startExam(String examCode) {
        User currentUser = getCurrentUser();
        Exam exam = examRepository.findByExamCode(examCode)
            .orElseThrow(() -> new IllegalArgumentException("Invalid exam code."));

        LocalDateTime now = LocalDateTime.now();
        
        boolean isManuallyOpen = Boolean.TRUE.equals(exam.getIsOpen());
        boolean isScheduled = exam.getStartTime() != null;
        boolean hasStarted = isScheduled && !now.isBefore(exam.getStartTime());
        boolean hasEnded = exam.getEndTime() != null && now.isAfter(exam.getEndTime());

        boolean isActiveWindow = isManuallyOpen || (isScheduled && hasStarted && !hasEnded);

        if (!isActiveWindow) {
            if (hasEnded) throw new IllegalArgumentException("Kỳ thi đã kết thúc.");
            if (isScheduled && !hasStarted) throw new IllegalArgumentException("Chưa đến thời gian làm bài.");
            throw new IllegalArgumentException("Kỳ thi hiện đang bị khóa.");
        }

        boolean isEnrolled = exam.getClassEntity() == null || exam.getClassEntity().getStudents().stream()
            .anyMatch(s -> s.getId().equals(currentUser.getId()));

        if (!isEnrolled && !isAdmin(currentUser)) {
            throw new AccessDeniedException("You are not enrolled in the class for this exam.");
        }

        User managedStudent = userRepository.findById(currentUser.getId())
            .orElseThrow(() -> new IllegalStateException("Current student not found in database"));

        Optional<ExamAttempt> existingAttempt = examAttemptRepository.findByExamAndStudent(exam, managedStudent);
        
        if (existingAttempt.isPresent()) {
            ExamAttempt attempt = existingAttempt.get();
            if (isActiveWindow && attempt.getSubmittedAt() != null) {
                attempt.setSubmittedAt(null);
                attempt.setCreatedAt(now);
                attempt.setScore(0);
                attempt.getAnswers().clear();
                return examAttemptRepository.save(attempt);
            }
            if (isActiveWindow && attempt.getSubmittedAt() == null) {
                long durationSeconds = (long) exam.getDuration() * 60;
                long elapsedSeconds = java.time.Duration.between(attempt.getCreatedAt(), now).getSeconds();
                if (elapsedSeconds > durationSeconds) {
                    attempt.setCreatedAt(now);
                    return examAttemptRepository.save(attempt);
                }
            }
            return attempt;
        }

        ExamAttempt newAttempt = ExamAttempt.builder()
            .exam(exam)
            .student(managedStudent)
            .createdAt(now)
            .build();
        
        return examAttemptRepository.save(newAttempt);
    }

    @Transactional
    public ExamAttempt submitExam(Long attemptId, Map<String, String> answers) {
        User currentUser = getCurrentUser();
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
            .orElseThrow(() -> new IllegalArgumentException("Exam attempt not found."));

        if (!attempt.getStudent().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You can only submit your own exam attempt.");
        }

        if (attempt.getSubmittedAt() != null) return attempt;

        int correctCount = 0;
        attempt.getAnswers().clear();

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            try {
                Long questionId = Long.parseLong(entry.getKey());
                String selectedOptionStr = entry.getValue();
                if (selectedOptionStr == null || selectedOptionStr.isEmpty()) continue;
                
                char selectedOption = selectedOptionStr.charAt(0);

                Question question = questionRepository.findById(questionId).orElse(null);
                if (question == null) continue;

                char studentAnswer = Character.toUpperCase(selectedOption);
                char correctAnswer = Character.toUpperCase(question.getCorrectOption());
                
                boolean isCorrect = (studentAnswer == correctAnswer);
                if (isCorrect) {
                    correctCount++;
                }

                Answer answer = Answer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOption(studentAnswer)
                    .isCorrect(isCorrect)
                    .build();
                attempt.getAnswers().add(answer);
            } catch (NumberFormatException e) {
                // Ignore invalid IDs
            }
        }

        attempt.setScore(correctCount);
        attempt.setSubmittedAt(LocalDateTime.now());
        return examAttemptRepository.save(attempt);
    }

    @Transactional
    public Question updateQuestion(Long questionId, Question questionDetails) {
        Question existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));
        if (!hasManagePermission(existingQuestion.getExam())) throw new AccessDeniedException("Permission denied.");
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
        Question existingQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with ID: " + questionId));
        if (!hasManagePermission(existingQuestion.getExam())) throw new AccessDeniedException("Permission denied.");
        questionRepository.delete(existingQuestion);
    }

    public List<ExamAttempt> getAttemptsByExamId(Long examId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        if (!hasManagePermission(exam)) throw new AccessDeniedException("Permission denied.");
        return examAttemptRepository.findByExamId(examId);
    }

    @Transactional
    public boolean toggleExamStatus(Long examId) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kỳ thi"));
        if (!hasManagePermission(exam)) throw new AccessDeniedException("Permission denied.");
        boolean currentStatus = Boolean.TRUE.equals(exam.getIsOpen());
        exam.setIsOpen(!currentStatus);
        examRepository.save(exam);
        return exam.getIsOpen();
    }

    @Transactional
    public void updateExamTitle(Long examId, String newTitle) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        if (!hasManagePermission(exam)) throw new AccessDeniedException("Permission denied.");
        exam.setExamTitle(newTitle);
        examRepository.save(exam);
    }

    public byte[] exportToExcel(Long examId) throws java.io.IOException {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        if (!hasManagePermission(exam)) throw new AccessDeniedException("Permission denied.");
        List<ExamAttempt> attempts = examAttemptRepository.findByExamId(examId);
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Kết quả thi");
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("STT");
            headerRow.createCell(1).setCellValue("Tên Sinh Viên");
            headerRow.createCell(2).setCellValue("Điểm Số");
            headerRow.createCell(3).setCellValue("Thời Gian Nộp");
            int rowIdx = 1;
            for (ExamAttempt attempt : attempts) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(attempt.getStudent().getFullName());
                row.createCell(2).setCellValue(attempt.getScore());
                row.createCell(3).setCellValue(attempt.getSubmittedAt() != null ? attempt.getSubmittedAt().toString() : "N/A");
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional
    public Map<String, Object> importEntireExamFromExcel(InputStream excelInputStream, Long classId) throws IOException {
        User currentUser = getCurrentUser();
        if (!isTeacher(currentUser) && !isAdmin(currentUser)) {
            throw new AccessDeniedException("Only teachers or admins can import exams.");
        }

        try (Workbook workbook = new XSSFWorkbook(excelInputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            // Read exam metadata from first few rows
            String examTitle = getCellValueAsString(sheet.getRow(0).getCell(0)); // A1: Exam title
            String durationStr = getCellValueAsString(sheet.getRow(0).getCell(1)); // B1: Duration
            String totalScoreStr = getCellValueAsString(sheet.getRow(0).getCell(2)); // C1: Total score

            // Validate exam metadata
            if (examTitle == null || examTitle.trim().isEmpty()) {
                throw new IllegalArgumentException("Exam title is required in cell A1");
            }

            int duration = 45; // default
            try {
                if (durationStr != null && !durationStr.trim().isEmpty()) {
                    duration = Integer.parseInt(durationStr.trim());
                }
            } catch (NumberFormatException e) {
                // Use default duration
            }

            int totalScore = 100; // default
            try {
                if (totalScoreStr != null && !totalScoreStr.trim().isEmpty()) {
                    totalScore = Integer.parseInt(totalScoreStr.trim());
                }
            } catch (NumberFormatException e) {
                // Use default total score
            }

            // Create the exam
            Exam exam = Exam.builder()
                    .examTitle(examTitle.trim())
                    .duration(duration)
                    .totalScore(totalScore)
                    .open(false)
                    .build();

            // Set teacher
            if (isAdmin(currentUser)) {
                exam.setTeacher(currentUser); // Admin can import for themselves
            } else {
                exam.setTeacher(currentUser);
            }

            // Set class if provided
            if (classId != null) {
                ClassEntity classEntity = classRepository.findById(classId)
                        .orElseThrow(() -> new IllegalArgumentException("Class not found"));
                exam.setClassEntity(classEntity);
            }

            if (exam.getExamCode() == null || exam.getExamCode().trim().isEmpty()) {
                exam.setExamCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            }

            Exam savedExam = examRepository.save(exam);

            // Import questions starting from row 3 (skip metadata and header)
            int questionCount = 0;
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Read question data
                String content = getCellValueAsString(row.getCell(0)); // Question content
                String optionA = getCellValueAsString(row.getCell(1)); // Option A
                String optionB = getCellValueAsString(row.getCell(2)); // Option B
                String optionC = getCellValueAsString(row.getCell(3)); // Option C
                String optionD = getCellValueAsString(row.getCell(4)); // Option D
                String correctOption = getCellValueAsString(row.getCell(5)); // Correct option (A, B, C, D)
                String scoreStr = getCellValueAsString(row.getCell(6)); // Score

                // Validate required fields
                if (content == null || content.trim().isEmpty()) continue;
                if (optionA == null || optionA.trim().isEmpty()) continue;
                if (optionB == null || optionB.trim().isEmpty()) continue;
                if (correctOption == null || correctOption.trim().isEmpty() || correctOption.trim().length() != 1) continue;

                // Parse score (default to 1 if not provided or invalid)
                int score = 1;
                try {
                    if (scoreStr != null && !scoreStr.trim().isEmpty()) {
                        score = Integer.parseInt(scoreStr.trim());
                    }
                } catch (NumberFormatException e) {
                    // Use default score of 1
                }

                // Create question
                Question question = Question.builder()
                        .exam(savedExam)
                        .content(content.trim())
                        .optionA(optionA.trim())
                        .optionB(optionB.trim())
                        .optionC(optionC != null ? optionC.trim() : null)
                        .optionD(optionD != null ? optionD.trim() : null)
                        .correctOption(correctOption.trim().toUpperCase().charAt(0))
                        .score(score)
                        .build();

                questionRepository.save(question);
                questionCount++;
            }

            return Map.of(
                "examId", savedExam.getId(),
                "examTitle", savedExam.getExamTitle(),
                "questionCount", questionCount
            );
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    @Transactional
    public void updateSchedule(Long examId, String startTimeStr, String endTimeStr) {
        Exam exam = examRepository.findById(examId).orElseThrow(() -> new IllegalArgumentException("Exam not found"));
        if (!hasManagePermission(exam)) throw new AccessDeniedException("Permission denied.");
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        if (startTimeStr != null && !startTimeStr.isEmpty()) exam.setStartTime(LocalDateTime.parse(startTimeStr, formatter));
        if (endTimeStr != null && !endTimeStr.isEmpty()) exam.setEndTime(LocalDateTime.parse(endTimeStr, formatter));
        examRepository.save(exam);
    }

    public Map<String, Object> getAttemptDetails(Long attemptId) {
        ExamAttempt attempt = examAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lượt thi"));
        User currentUser = getCurrentUser();
        Exam exam = attempt.getExam();
        
        if (!isAdmin(currentUser) && !exam.getTeacher().getId().equals(currentUser.getId()) && !attempt.getStudent().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Permission denied.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = attempt.getCreatedAt();
        if (startTime == null) startTime = now;
        
        long durationSeconds = (long) exam.getDuration() * 60;
        if (durationSeconds <= 0) durationSeconds = 45 * 60;

        LocalDateTime durationEndTime = startTime.plusSeconds(durationSeconds);
        LocalDateTime finalEndTime = durationEndTime;
        
        if (!exam.getIsOpen() && exam.getEndTime() != null && exam.getEndTime().isBefore(finalEndTime)) {
            finalEndTime = exam.getEndTime();
        }

        long remainingSeconds = java.time.Duration.between(now, finalEndTime).getSeconds();

        if (attempt.getSubmittedAt() == null && remainingSeconds < 30 && (exam.getIsOpen() || (exam.getEndTime() != null && now.isBefore(exam.getEndTime())))) {
             remainingSeconds = durationSeconds;
        }

        int totalQuestions = (exam.getQuestions() != null) ? exam.getQuestions().size() : 0;
        int score = attempt.getScore();
        double finalScore10 = 0.0;
        if (totalQuestions > 0) {
            finalScore10 = ((double) score / totalQuestions) * 10.0;
        }
        finalScore10 = Math.round(finalScore10 * 100.0) / 100.0;

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("exam", exam);
        response.put("attempt", attempt);
        response.put("remainingSeconds", Math.max(0, remainingSeconds));
        response.put("isSubmitted", attempt.getSubmittedAt() != null);
        response.put("correctCount", score);
        response.put("finalScore", finalScore10);

        return response;
    }
}
