package com.exam.examweb.controllers;

import com.exam.examweb.entities.Note;
import com.exam.examweb.entities.User;
import com.exam.examweb.entities.ClassEntity;
import com.exam.examweb.repositories.NoteRepository;
import com.exam.examweb.repositories.ClassRepository;
import com.exam.examweb.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@Slf4j
public class NoteApiController {

    private final NoteRepository noteRepository;
    private final ClassRepository classRepository;
    private final IUserRepository userRepository;

    @GetMapping("/class/{classId}")
    public ResponseEntity<List<Note>> getNotesByClass(@PathVariable Long classId, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        ClassEntity classEntity = classRepository.findById(classId).orElseThrow();

        // Check if user is the teacher or a student in the class
        boolean isTeacher = classEntity.getTeacher().getId().equals(user.getId());
        boolean isStudent = classEntity.getStudents().stream().anyMatch(s -> s.getId().equals(user.getId()));

        if (!isTeacher && !isStudent) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(noteRepository.findByClassEntityId(classId));
    }

    @PostMapping
    public ResponseEntity<Note> createNote(@RequestBody Map<String, Object> payload, @AuthenticationPrincipal UserDetails userDetails) {
        String title = (String) payload.get("title");
        String content = (String) payload.get("content");
        Long classId = Long.valueOf(payload.get("classId").toString());

        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        ClassEntity classEntity = classRepository.findById(classId).orElseThrow();

        Note note = Note.builder()
                .title(title)
                .content(content)
                .user(user)
                .classEntity(classEntity)
                .build();

        return ResponseEntity.ok(noteRepository.save(note));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Note note = noteRepository.findById(id).orElseThrow();
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        // Check if user owns the note or is the teacher of the class
        if (!note.getUser().getId().equals(user.getId()) && !note.getClassEntity().getTeacher().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        noteRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Note> updateNote(@PathVariable Long id, @RequestBody Map<String, Object> payload, @AuthenticationPrincipal UserDetails userDetails) {
        Note note = noteRepository.findById(id).orElseThrow();
        User user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();

        // Check if user owns the note or is the teacher of the class
        if (!note.getUser().getId().equals(user.getId()) && !note.getClassEntity().getTeacher().getId().equals(user.getId())) {
            return ResponseEntity.status(403).build();
        }

        String title = (String) payload.get("title");
        String content = (String) payload.get("content");

        if (title != null) note.setTitle(title);
        if (content != null) note.setContent(content);

        return ResponseEntity.ok(noteRepository.save(note));
    }
}
