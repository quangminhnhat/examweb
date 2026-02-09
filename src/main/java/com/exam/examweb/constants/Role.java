package com.exam.examweb.constants;
import lombok.AllArgsConstructor;
@AllArgsConstructor
public enum Role {
    admin(1),
    teacher(2),
    student(3);
    public final long value;
}
