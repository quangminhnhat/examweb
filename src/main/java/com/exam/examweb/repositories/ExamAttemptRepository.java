package com.exam.examweb.repositories;

import com.exam.examweb.entities.Exam;
import com.exam.examweb.entities.ExamAttempt;
import com.exam.examweb.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {
    Optional<ExamAttempt> findByExamAndStudent(Exam exam, User student);
    List<ExamAttempt> findByStudent(User student);
    List<ExamAttempt> findByExam(Exam exam);
}
