CREATE DATABASE IF NOT EXISTS online_exam_system
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE online_exam_system;

/* ================= USERS ================= */
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(250) NOT NULL,
  email VARCHAR(50) NOT NULL UNIQUE,
  phone VARCHAR(10) UNIQUE,
  full_name VARCHAR(255),
  provider VARCHAR(50),
  google_id VARCHAR(255) UNIQUE,
  avatar VARCHAR(500),
  dob DATE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

/* ================= ROLES ================= */
CREATE TABLE roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(250)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (id, role_name, description) VALUES
(1, 'admin', 'Administrator'),
(2, 'teacher', 'Teacher'),
(3, 'student', 'Student');

/* ================= USER_ROLE ================= */
CREATE TABLE user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB
DEFAULT CHARSET=utf8mb4
COLLATE=utf8mb4_unicode_ci;

ALTER TABLE user_role
  ADD KEY user_id (user_id),
  ADD KEY role_id (role_id);

ALTER TABLE user_role
  ADD CONSTRAINT user_role_ibfk_1
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT user_role_ibfk_2
    FOREIGN KEY (role_id) REFERENCES roles(id);

/* ================= CLASSES ================= */
CREATE TABLE classes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_name VARCHAR(255) NOT NULL,
  teacher_id BIGINT NOT NULL,
  invite_code VARCHAR(20) UNIQUE NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (teacher_id) REFERENCES users(id)
) ENGINE=InnoDB;

/* ================= CLASS STUDENTS ================= */
CREATE TABLE class_students (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (class_id, student_id),
  FOREIGN KEY (class_id) REFERENCES classes(id),
  FOREIGN KEY (student_id) REFERENCES users(id)
) ENGINE=InnoDB;

/* ================= EXAMS (WITH EXAM CODE) ================= */
CREATE TABLE exams (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_title VARCHAR(255) NOT NULL,
  exam_code VARCHAR(20) NOT NULL UNIQUE,
  teacher_id BIGINT NOT NULL,
  class_id BIGINT,
  duration INT NOT NULL,
  total_score INT DEFAULT 100,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (teacher_id) REFERENCES users(id),
  FOREIGN KEY (class_id) REFERENCES classes(id)
) ENGINE=InnoDB;

/* ================= QUESTIONS ================= */
CREATE TABLE questions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  content TEXT NOT NULL,
  option_a TEXT,
  option_b TEXT,
  option_c TEXT,
  option_d TEXT,
  correct_option CHAR(1),
  score INT DEFAULT 1,
  FOREIGN KEY (exam_id) REFERENCES exams(id)
) ENGINE=InnoDB;

/* ================= EXAM ATTEMPTS ================= */
CREATE TABLE exam_attempts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  score INT DEFAULT 0,
  submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (exam_id, student_id),
  FOREIGN KEY (exam_id) REFERENCES exams(id),
  FOREIGN KEY (student_id) REFERENCES users(id)
) ENGINE=InnoDB;

/* ================= ANSWERS ================= */
CREATE TABLE answers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  selected_option CHAR(1),
  is_correct BOOLEAN,
  FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id),
  FOREIGN KEY (question_id) REFERENCES questions(id)
) ENGINE=InnoDB;

/* ================= CHEATING LOGS ================= */
CREATE TABLE exam_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  attempt_id BIGINT NOT NULL,
  tab_switch INT DEFAULT 0,
  copy_count INT DEFAULT 0,
  screenshot_count INT DEFAULT 0,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (attempt_id) REFERENCES exam_attempts(id)
) ENGINE=InnoDB;

/* ================= VIEW ================= */
CREATE VIEW v_exam_results AS
SELECT
  u.full_name,
  e.exam_title,
  e.exam_code,
  a.score,
  l.tab_switch,
  l.copy_count,
  l.screenshot_count
FROM exam_attempts a
JOIN users u ON a.student_id = u.id
JOIN exams e ON a.exam_id = e.id
LEFT JOIN exam_logs l ON a.id = l.attempt_id;
