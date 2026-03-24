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

    // Tìm bài làm của 1 sinh viên cụ thể trong 1 kỳ thi cụ thể
    Optional<ExamAttempt> findByExamAndStudent(Exam exam, User student);

    // Tìm tất cả bài làm của 1 sinh viên
    List<ExamAttempt> findByStudent(User student);

    // Tìm tất cả bài làm của 1 kỳ thi (truyền vào cả Object Exam)
    List<ExamAttempt> findByExam(Exam exam);

    // Tìm tất cả bài làm của 1 kỳ thi theo ID (Dùng cho Xuất Excel và Xem điểm)
    List<ExamAttempt> findByExamId(Long examId);
}