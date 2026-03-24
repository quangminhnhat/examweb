package com.exam.examweb.controllers;

import com.exam.examweb.entities.ClassEntity;
import com.exam.examweb.entities.User;
import com.exam.examweb.services.ClassService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Slf4j
public class ClassApiController {

    // Chỉ cần dùng Service là đủ, không cần gọi trực tiếp Repository ở đây
    private final ClassService classService;

    @GetMapping
    public List<ClassEntity> getAllClasses() {
        return classService.getAllClasses();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassEntity> getClassById(@PathVariable Long id) {
        return classService.getClassById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClassEntity> createClass(@RequestBody ClassEntity newClass) {
        log.info("Yêu cầu tạo lớp học mới: {}", newClass.getClassName());
        return ResponseEntity.ok(classService.createClass(newClass));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassEntity> updateClass(@PathVariable Long id, @RequestBody ClassEntity classDetails) {
        ClassEntity updatedClass = classService.updateClass(id, classDetails);
        if (updatedClass != null) {
            return ResponseEntity.ok(updatedClass);
        }
        return ResponseEntity.notFound().build();
    }

    // GIỮ LẠI MỘT HÀM XÓA DUY NHẤT VÀ GỌI QUA SERVICE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClass(@PathVariable Long id) {
        log.warn("Đang thực hiện xóa lớp học ID: {}", id);
        classService.deleteClass(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/join")
    public ResponseEntity<ClassEntity> joinClass(@RequestBody Map<String, String> payload) {
        String inviteCode = payload.get("inviteCode");
        if (inviteCode == null) return ResponseEntity.badRequest().build();

        try {
            return ResponseEntity.ok(classService.joinClass(inviteCode));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{classId}/students")
    public ResponseEntity<Set<User>> getStudentsInClass(@PathVariable Long classId) {
        try {
            return ResponseEntity.ok(classService.getStudentsInClass(classId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{classId}/leave")
    public ResponseEntity<Void> leaveClass(@PathVariable Long classId) {
        classService.leaveClass(classId);
        return ResponseEntity.ok().build();
    }
}