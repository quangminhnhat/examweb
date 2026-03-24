package com.exam.examweb.services;

import com.exam.examweb.entities.ClassEntity;
import com.exam.examweb.entities.User;
import com.exam.examweb.repositories.ClassRepository;
import com.exam.examweb.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClassService {
    private final ClassRepository classRepository;
    private final IUserRepository userRepository;

    // Hàm tiện ích để lấy User hiện tại nhanh hơn
    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new AccessDeniedException("User chưa đăng nhập hoặc không hợp lệ");
    }

    public List<ClassEntity> getAllClasses() {
        User currentUser = getCurrentUser();
        if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"))) {
            return classRepository.findAll();
        } else if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher"))) {
            return classRepository.findByTeacher(currentUser);
        } else if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("student"))) {
            return classRepository.findByStudents_Id(currentUser.getId());
        }
        return List.of();
    }

    public Optional<ClassEntity> getClassById(Long id) {
        User currentUser = getCurrentUser();
        ClassEntity cls = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học với ID: " + id));

        // Kiểm tra quyền xem chi tiết
        boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"));
        boolean isTeacherOwner = cls.getTeacher().getId().equals(currentUser.getId());
        boolean isEnrolledStudent = cls.getStudents().stream().anyMatch(s -> s.getId().equals(currentUser.getId()));

        if (isAdmin || isTeacherOwner || isEnrolledStudent) {
            return Optional.of(cls);
        }
        throw new AccessDeniedException("Bạn không có quyền xem thông tin lớp này");
    }

    @Transactional
    public ClassEntity createClass(ClassEntity newClass) {
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"));
        boolean isTeacher = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher"));

        if (!isAdmin && !isTeacher) {
            throw new AccessDeniedException("Chỉ Giáo viên hoặc Admin mới được tạo lớp");
        }

        if (isTeacher) {
            User managedTeacher = userRepository.findById(currentUser.getId()).orElseThrow();
            newClass.setTeacher(managedTeacher);
        } else {
            if (newClass.getTeacher() == null || newClass.getTeacher().getId() == null) {
                throw new IllegalArgumentException("Admin phải chỉ định ID Giáo viên cho lớp học.");
            }
            User teacher = userRepository.findById(newClass.getTeacher().getId()).orElseThrow();
            newClass.setTeacher(teacher);
        }

        if (newClass.getInviteCode() == null || newClass.getInviteCode().isEmpty()) {
            newClass.setInviteCode(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        newClass.setCreatedAt(java.time.LocalDateTime.now());
        return classRepository.save(newClass);
    }

    @Transactional
    public ClassEntity updateClass(Long id, ClassEntity classDetails) {
        User currentUser = getCurrentUser();
        ClassEntity existingClass = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lớp không tồn tại"));

        boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"));
        boolean isOwner = existingClass.getTeacher().getId().equals(currentUser.getId());

        if (isAdmin || isOwner) {
            existingClass.setClassName(classDetails.getClassName());
            return classRepository.save(existingClass);
        }
        throw new AccessDeniedException("Bạn không có quyền sửa lớp này");
    }

    @Transactional
    public void deleteClass(Long id) {
        User currentUser = getCurrentUser();
        ClassEntity existingClass = classRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp để xóa"));

        boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"));
        boolean isOwner = existingClass.getTeacher().getId().equals(currentUser.getId());

        if (isAdmin || isOwner) {
            // Lưu ý: Đảm bảo ClassEntity có cascade = CascadeType.ALL cho list Exams
            classRepository.delete(existingClass);
        } else {
            throw new AccessDeniedException("Bạn không có quyền xóa lớp này");
        }
    }

    @Transactional
    public ClassEntity joinClass(String inviteCode) {
        User currentUser = getCurrentUser();
        if (currentUser.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("student"))) {
            throw new AccessDeniedException("Chỉ sinh viên mới được tham gia lớp học.");
        }

        ClassEntity classToJoin = classRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("Mã mời không chính xác."));

        User managedStudent = userRepository.findById(currentUser.getId()).orElseThrow();
        classToJoin.getStudents().add(managedStudent);
        return classRepository.save(classToJoin);
    }

    public Set<User> getStudentsInClass(Long classId) {
        User currentUser = getCurrentUser();
        ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Lớp không tồn tại."));

        boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"));
        boolean isOwner = classEntity.getTeacher().getId().equals(currentUser.getId());

        if (isAdmin || isOwner) {
            return classEntity.getStudents();
        }
        throw new AccessDeniedException("Bạn không có quyền xem danh sách sinh viên lớp này.");
    }

    @Transactional
    public void leaveClass(Long classId) {
        User currentUser = getCurrentUser(); // Hàm tiện ích lấy user đang đăng nhập
        ClassEntity cls = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Lớp không tồn tại"));

        // Xóa sinh viên khỏi Set students của lớp
        cls.getStudents().removeIf(s -> s.getId().equals(currentUser.getId()));
        classRepository.save(cls);
    }
}