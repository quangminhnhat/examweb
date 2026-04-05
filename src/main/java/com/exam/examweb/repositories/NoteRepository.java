package com.exam.examweb.repositories;

import com.exam.examweb.entities.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByClassEntityId(Long classId);
    List<Note> findByClassEntityIdAndUserId(Long classId, Long userId);
}
