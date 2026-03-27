package com.exam.examweb.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "exams")
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_title", nullable = false)
    private String examTitle;

    @Column(name = "exam_code", unique = true, nullable = false, length = 20)
    private String examCode;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    @JsonIgnoreProperties({"exams", "classes", "password", "authorities"})
    private User teacher;

    @ManyToOne
    @JoinColumn(name = "class_id")
    @JsonIgnoreProperties({"exams", "students", "teacher"})
    private ClassEntity classEntity;

    @Column(nullable = false)
    private int duration;

    @Builder.Default
    private int totalScore = 100;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "exam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties("exam")
    private List<Question> questions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Column(name = "start_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    @Builder.Default
    @Column(name = "is_open")
    @JsonProperty("isOpen")
    private boolean open = false;

    // Explicit getters/setters to handle "is" naming issues with Jackson/Hibernate
    public boolean getIsOpen() {
        return open;
    }

    public void setIsOpen(boolean open) {
        this.open = open;
    }
}
