package com.exam.examweb.services;

import com.exam.examweb.entities.*;
import com.exam.examweb.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
}
