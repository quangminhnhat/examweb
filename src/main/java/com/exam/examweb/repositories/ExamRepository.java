package com.exam.examweb.repositories;

import com.exam.examweb.entities.ClassEntity;
import com.exam.examweb.entities.Exam;
import com.exam.examweb.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.exam.examweb.entities.ExamAttempt;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByTeacher(User teacher);
    List<Exam> findByClassEntity_Students_Id(Long studentId);
    Optional<Exam> findByExamCode(String examCode);
}
