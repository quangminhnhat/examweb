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

    public List<ClassEntity> getAllClasses() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            User currentUser = (User) principal;
            if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"))) {
                return classRepository.findAll();
            } else if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher"))) {
                return classRepository.findByTeacher(currentUser);
            } else if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("student"))) {
                return classRepository.findByStudents_Id(currentUser.getId());
            }
        }
        return List.of();
    }

    public Optional<ClassEntity> getClassById(Long id) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            User currentUser = (User) principal;
            Optional<ClassEntity> classOptional = classRepository.findById(id);
            
            if (classOptional.isPresent()) {
                ClassEntity cls = classOptional.get();
                if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"))) {
                    return classOptional;
                }
                if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher")) 
                        && cls.getTeacher().getId().equals(currentUser.getId())) {
                    return classOptional;
                }
                if (currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("student"))
                        && cls.getStudents().stream().anyMatch(s -> s.getId().equals(currentUser.getId()))) {
                    return classOptional;
                }
            }
        }
        return Optional.empty();
    }

    @Transactional
    public ClassEntity createClass(ClassEntity newClass) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            User currentUser = (User) principal;
            
            boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"));
            boolean isTeacher = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher"));

            if (!isAdmin && !isTeacher) {
                 throw new AccessDeniedException("Only teachers and admins can create classes");
            }
            
            if (isTeacher) {
                User managedTeacher = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new IllegalStateException("Current teacher not found in database"));
                newClass.setTeacher(managedTeacher);
            } else { // isAdmin
                 if (newClass.getTeacher() == null || newClass.getTeacher().getId() == null) {
                    throw new IllegalArgumentException("Admin must specify a teacher ID when creating a class.");
                }
                User teacher = userRepository.findById(newClass.getTeacher().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Specified teacher with ID " + newClass.getTeacher().getId() + " not found."));
                newClass.setTeacher(teacher);
            }
            
            if (newClass.getInviteCode() == null || newClass.getInviteCode().isEmpty()) {
                newClass.setInviteCode(UUID.randomUUID().toString().substring(0, 8));
            }
            return classRepository.save(newClass);
        }
        throw new AccessDeniedException("User not authenticated");
    }

    @Transactional
    public ClassEntity updateClass(Long id, ClassEntity classDetails) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            User currentUser = (User) principal;
            Optional<ClassEntity> existingClassOpt = classRepository.findById(id);
            if (existingClassOpt.isPresent()) {
                ClassEntity existingClass = existingClassOpt.get();
                
                boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"));
                boolean isTeacher = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher"));
                boolean isOwner = existingClass.getTeacher().getId().equals(currentUser.getId());

                if (isAdmin || (isTeacher && isOwner)) {
                    existingClass.setClassName(classDetails.getClassName());
                    return classRepository.save(existingClass);
                }
                throw new AccessDeniedException("You do not have permission to update this class");
            }
            return null;
        }
        throw new AccessDeniedException("User not authenticated");
    }

    @Transactional
    public void deleteClass(Long id) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            User currentUser = (User) principal;
            Optional<ClassEntity> existingClassOpt = classRepository.findById(id);
            if (existingClassOpt.isPresent()) {
                ClassEntity existingClass = existingClassOpt.get();
                
                boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"));
                boolean isTeacher = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher"));
                boolean isOwner = existingClass.getTeacher().getId().equals(currentUser.getId());

                 if (isAdmin || (isTeacher && isOwner)) {
                    classRepository.deleteById(id);
                } else {
                     throw new AccessDeniedException("You do not have permission to delete this class");
                }
            }
        }
    }

    @Transactional
    public ClassEntity joinClass(String inviteCode) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            User currentUser = (User) principal;
            if (currentUser.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("student"))) {
                throw new AccessDeniedException("Only students can join classes.");
            }

            ClassEntity classToJoin = classRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code."));

            User managedStudent = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("Current student not found in database"));

            classToJoin.getStudents().add(managedStudent);
            return classRepository.save(classToJoin);
        }
        throw new AccessDeniedException("User not authenticated");
    }

    public Set<User> getStudentsInClass(Long classId) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof User) {
            User currentUser = (User) principal;
            ClassEntity classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found."));

            boolean isAdmin = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("admin"));
            boolean isTeacher = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("teacher"));
            boolean isOwner = classEntity.getTeacher().getId().equals(currentUser.getId());

            if (isAdmin || (isTeacher && isOwner)) {
                return classEntity.getStudents();
            }
            throw new AccessDeniedException("You do not have permission to view students in this class.");
        }
        throw new AccessDeniedException("User not authenticated");
    }
}
