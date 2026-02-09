package com.exam.examweb.repositories;

import com.exam.examweb.entities.ClassEntity;
import com.exam.examweb.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
    Optional<ClassEntity> findByInviteCode(String inviteCode);
    List<ClassEntity> findByTeacher(User teacher);
    List<ClassEntity> findByStudents_Id(Long studentId);
}
