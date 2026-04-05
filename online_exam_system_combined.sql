-- Combined SQL Dump
-- Merged from online_exam_system.sql and online_exam_system (1).sql

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

CREATE DATABASE IF NOT EXISTS `online_exam_system` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `online_exam_system`;

-- --------------------------------------------------------
-- Table structure for table `roles`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `roles`;
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(250) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `role_name` (`role_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `roles` (`id`, `role_name`, `description`) VALUES
(1, 'admin', 'Administrator'),
(2, 'teacher', 'Teacher'),
(3, 'student', 'Student');

-- --------------------------------------------------------
-- Table structure for table `users`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provider` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `google_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `avatar` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `dob` date DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `reset_password_token` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reset_password_token_expiry` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`),
  UNIQUE KEY `phone` (`phone`),
  UNIQUE KEY `google_id` (`google_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Merging users from both files (IDs adjusted to prevent collision where necessary)
INSERT INTO `users` (`id`, `username`, `password`, `email`, `phone`, `full_name`, `provider`, `google_id`, `avatar`, `dob`, `created_at`) VALUES
(1, 'taile', '$2a$10$6mMIoJEtMXvDyh7PjTLpnuzlj8i.Qm6i7gCv808a1EPDWZ8wDmePu', 'taile@mail.com', '1231231233', NULL, 'Local', NULL, NULL, NULL, '2026-03-24 08:22:10'),
(2, 'tailer', '$2a$10$eARYEfSPsZIwZTCnxtfE0e9YmeeygwdUSPjQXz5V1iMhBnA0rE6yG', 'taile@gmail.com', '0792346444', NULL, 'Local', NULL, NULL, NULL, '2026-03-24 08:33:20'),
(3, 'letai', '$2a$10$ZTtQ2fOmmp9.xRvuMyipSeDgI51F7gTQssyF.Y80tTOjy16pIGJwi', 'letai@mail.com', '0123456789', NULL, 'Local', NULL, NULL, NULL, '2026-03-24 09:48:08'),
(4, 'teacher_user', '$2a$10$LR8CNyC25KtXDwfFiYuUG.GaTUWyrjThrRt81izFKinRyS2Cv/ke2', 'teacher@example.com', '1234567890', NULL, 'Local', NULL, NULL, NULL, '2026-03-11 07:00:02'),
(5, 'student_user', '$2a$10$7rx8Ry/ntPN2pBj1ymiENezwQhIpd2oMJYvvFmZLQmXM5mIYwl/DO', 'student@example.com', '0987654321', NULL, 'Local', NULL, NULL, NULL, '2026-03-11 07:00:02'),
(7, '123', '$2a$10$Ofd.6JVEt2bBYrWzoF.0z.j/oHogYMsKTLJRCBgROo7vbC3UU/CWC', 'test@gmail.com', '0822488288', NULL, 'Local', NULL, NULL, NULL, '2026-03-16 16:52:52'),
(8, 'malockdiablo@gmail.com', '$2a$10$gVZHsgqmrNmlCRf0loi4leuAgjjlpEX6MKqjcRc2kfo4WgZEZ/Osa', 'malockdiablo@gmail.com', NULL, '1755_Lê Kinh Hoàng Long', 'Google', '117739321818928090780', 'https://lh3.googleusercontent.com/a/ACg8ocK0Bx2-7Jxo3BpY_YOs1fL5meXdOvtk1L6lOIUPg4g8dWQHOg=s96-c', NULL, '2026-03-30 02:10:39'),
(9, 'hoanglonglekinh01@gmail.com', '$2a$10$LR8CNyC25KtXDwfFiYuUG.GaTUWyrjThrRt81izFKinRyS2Cv/ke2', 'hoanglonglekinh01@gmail.com', NULL, 'le long', 'Google', '101814434798029928480', 'https://lh3.googleusercontent.com/a/ACg8ocLyn8xzi0_sv873U_b7Fg8uV6G-k8NsBr5pSsLYKNwm148qaw=s96-c', NULL, '2026-03-30 02:11:15');

-- --------------------------------------------------------
-- Table structure for table `user_role`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `user_role`;
CREATE TABLE `user_role` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  CONSTRAINT `user_role_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `user_role_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `user_role` (`user_id`, `role_id`) VALUES
(1, 2), (2, 2), (3, 3), (4, 2), (5, 3), (7, 2), (7, 3), (8, 2), (9, 3);

-- --------------------------------------------------------
-- Table structure for table `classes`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `classes`;
CREATE TABLE `classes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `teacher_id` bigint NOT NULL,
  `invite_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `invite_code` (`invite_code`),
  KEY `teacher_id` (`teacher_id`),
  CONSTRAINT `classes_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `classes` (`id`, `class_name`, `teacher_id`, `invite_code`, `created_at`) VALUES
(1, 'Toán', 2, '1fc2cec8', '2026-03-24 09:46:33'),
(2, 'văn', 2, '454a2f79', '2026-03-24 11:47:23'),
(6, 'adada', 2, '5561d6fe', '2026-03-24 12:24:05'),
(8, 'tt', 1, '1F9733F1', '2026-03-24 13:31:44'),
(9, 'adac', 1, 'C5225859', '2026-03-24 13:58:29'),
(10, 'vcav', 1, 'F817B055', '2026-03-24 13:58:33'),
(11, 'ávdaav', 1, '6AC73C6B', '2026-03-24 13:58:37'),
(13, 'sgesdg', 8, '6F302D2F', '2026-03-30 02:18:18');

-- --------------------------------------------------------
-- Table structure for table `class_students`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `class_students`;
CREATE TABLE `class_students` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `joined_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `class_id` (`class_id`,`student_id`),
  KEY `student_id` (`student_id`),
  CONSTRAINT `class_students_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `class_students_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `class_students` (`id`, `class_id`, `student_id`, `joined_at`) VALUES
(5, 11, 3, '2026-03-24 14:37:54'),
(6, 13, 9, '2026-04-05 01:38:11');

-- --------------------------------------------------------
-- Table structure for table `exams`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `exams`;
CREATE TABLE `exams` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `exam_code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `teacher_id` bigint NOT NULL,
  `class_id` bigint DEFAULT NULL,
  `duration` int NOT NULL,
  `total_score` int DEFAULT '100',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time` datetime(6) DEFAULT NULL,
  `is_open` bit(1) DEFAULT NULL,
  `start_time` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `exam_code` (`exam_code`),
  KEY `teacher_id` (`teacher_id`),
  KEY `class_id` (`class_id`),
  CONSTRAINT `exams_ibfk_1` FOREIGN KEY (`teacher_id`) REFERENCES `users` (`id`),
  CONSTRAINT `exams_ibfk_2` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `exams` (`id`, `exam_title`, `exam_code`, `teacher_id`, `class_id`, `duration`, `total_score`, `created_at`, `end_time`, `is_open`, `start_time`) VALUES
(1, 'kt1', 'EXAM-4477', 2, 1, 45, 100, '2026-03-24 10:02:44', '2026-03-24 18:54:00.000000', b'1', '2026-03-24 18:49:00.000000'),
(2, 'kt2', 'EXAM-8241', 2, 1, 45, 100, '2026-03-24 12:04:36', NULL, b'0', NULL),
(3, 'kt3', 'EXAM-3260', 2, 1, 45, 100, '2026-03-24 12:05:09', NULL, b'0', NULL),
(4, 'kt4', 'EXAM-9842', 2, 1, 45, 100, '2026-03-24 12:05:37', NULL, b'0', NULL),
(5, 'kt5', 'EXAM-9243', 2, 1, 45, 100, '2026-03-24 12:06:06', NULL, b'0', NULL),
(9, 'kt33', 'EXAM-6641', 2, 1, 45, 100, '2026-03-24 12:17:34', NULL, b'0', NULL),
(10, 'mjg', 'EXAM-7004', 2, 6, 45, 100, '2026-03-24 12:24:25', NULL, b'0', NULL),
(11, 'ttt', 'EXAM-7595', 1, 8, 45, 100, '2026-03-24 13:32:07', NULL, b'1', NULL),
(12, 'â', 'EXAM-5610', 1, 8, 45, 100, '2026-03-24 13:32:12', NULL, b'0', NULL),
(13, 'kt11', 'EXAM-7839', 1, 11, 45, 100, '2026-03-24 14:12:25', NULL, b'0', NULL),
(14, 'kt22', 'EXAM-3245', 1, 11, 45, 100, '2026-03-24 14:12:33', NULL, b'1', NULL),
(15, '2', 'EXAM-9466', 1, 11, 45, 100, '2026-03-24 14:16:01', NULL, b'1', NULL),
(16, 'a', 'EXAM-2314', 1, 11, 45, 100, '2026-03-24 14:21:26', NULL, b'0', NULL),
(17, 'ưte', 'EXAM-3399', 8, 13, 45, 100, '2026-03-30 02:18:28', NULL, b'0', NULL);

-- --------------------------------------------------------
-- Table structure for table `questions`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `questions`;
CREATE TABLE `questions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_id` bigint NOT NULL,
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `option_a` text COLLATE utf8mb4_unicode_ci,
  `option_b` text COLLATE utf8mb4_unicode_ci,
  `option_c` text COLLATE utf8mb4_unicode_ci,
  `option_d` text COLLATE utf8mb4_unicode_ci,
  `correct_option` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `score` int DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `exam_id` (`exam_id`),
  CONSTRAINT `questions_ibfk_1` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `questions` (`id`, `exam_id`, `content`, `option_a`, `option_b`, `option_c`, `option_d`, `correct_option`, `score`) VALUES
(1, 1, 'hhvkhcbd', 'halblhvbs', 'avds', 'a', 'c', 'C', 1),
(2, 1, 'avzx x', 'aa', 'âvvav', 'acxczc', 'avdx', 'D', 1),
(3, 1, 'svsb', 'sb', 'sb', 'sbs', 'sb', 'C', 1),
(4, 1, 'svd', 'cv', 'cvx', 'z', 'x', 'B', 1),
(5, 1, 'avs', 'av', 'ávsasdv', 'av', 'avsd', 'A', 1),
(6, 10, 'àvd', 'fbsb', 'bsdbf', 'dbvxc', 'sdbv', 'C', 1),
(7, 10, 'avas', 'avf', 'avf', 'av', 'av', 'B', 1),
(9, 11, 'sbsbds', 'sbdbs', 'sbsbd', 'ffbsb', 'sb', 'B', 1),
(11, 11, 'ácdv', 'avsav', 'avsd', 'avsd', 'avsd', 'A', 1),
(12, 11, 'avsd1`1321', 'avds', 'av', 'vad', '', 'D', 1),
(13, 14, 'ávf', 'àv', 'ádv2', 'av', 'vsa', 'C', 1),
(14, 14, 'fdbab', 'bfd', 'abd', 'acv', 'abf', 'C', 1),
(15, 14, 'àv', 'a', '1', '12', 'ffds', 'A', 1),
(16, 15, '1', 'ge', 'gwe', '2', '1r', 'A', 1),
(17, 15, 'qgr', 'gqre', 'qg', 'sdf', 'df', 'A', 1),
(18, 15, 'qgr', 'qgedf', 'gr', 'sdfb', 'gfq3qg', 'A', 1),
(19, 15, 'qgfbsdv', 'qewrb', 'bdfb', 'bsvas2', 'xcv3ge', 'A', 1),
(20, 15, 'ewbsd', 'dvb', 'b', 'fssdb', 'dv', 'A', 1),
(21, 15, 'wbsd', 'bf', 'sdb', 'fsbwb', 'db', 'A', 1),
(22, 17, '', 'gsg', 'gsge', 'gsggw', 'jtehw', 'A', 1),
(23, 17, '', '', '', '', '', 'A', 1);

-- --------------------------------------------------------
-- Table structure for table `exam_attempts`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `exam_attempts`;
CREATE TABLE `exam_attempts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `score` int DEFAULT '0',
  `submitted_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `exam_id` (`exam_id`,`student_id`),
  KEY `student_id` (`student_id`),
  CONSTRAINT `exam_attempts_ibfk_1` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`),
  CONSTRAINT `exam_attempts_ibfk_2` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `exam_attempts` (`id`, `exam_id`, `student_id`, `score`, `submitted_at`, `created_at`) VALUES
(1, 1, 3, 0, '2026-03-24 12:03:13', '2026-03-24 18:49:50.153605'),
(2, 11, 3, 0, NULL, '2026-03-24 20:33:38.766225'),
(3, 14, 3, 3, '2026-03-24 15:09:01', '2026-03-24 21:38:02.216085'),
(4, 15, 3, 4, '2026-03-24 15:21:30', '2026-03-24 22:21:14.331533');

-- --------------------------------------------------------
-- Table structure for table `answers`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `answers`;
CREATE TABLE `answers` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_id` bigint NOT NULL,
  `question_id` bigint NOT NULL,
  `selected_option` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_correct` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `attempt_id` (`attempt_id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `answers_ibfk_1` FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempts` (`id`),
  CONSTRAINT `answers_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `answers` (`id`, `attempt_id`, `question_id`, `selected_option`, `is_correct`) VALUES
(1, 3, 13, 'C', 1), (2, 3, 14, 'C', 1), (3, 3, 15, 'A', 1), (4, 3, 13, 'C', 1), (5, 3, 13, 'C', 1), (6, 3, 14, 'C', 1), (7, 3, 15, 'D', 0), (8, 3, 13, 'C', 1), (9, 3, 14, 'B', 0), (10, 3, 15, 'A', 1), (11, 3, 13, 'C', 1), (12, 3, 14, 'C', 1), (13, 3, 15, 'A', 1), (14, 4, 16, 'A', 1), (15, 4, 17, 'A', 1), (16, 4, 18, 'A', 1), (17, 4, 19, 'A', 1), (18, 4, 20, 'B', 0), (19, 4, 21, 'D', 0);

-- --------------------------------------------------------
-- Table structure for table `exam_logs`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `exam_logs`;
CREATE TABLE `exam_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attempt_id` bigint NOT NULL,
  `tab_switch` int DEFAULT '0',
  `copy_count` int DEFAULT '0',
  `screenshot_count` int DEFAULT '0',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `attempt_id` (`attempt_id`),
  CONSTRAINT `exam_logs_ibfk_1` FOREIGN KEY (`attempt_id`) REFERENCES `exam_attempts` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------
-- Table structure for table `notes`
-- --------------------------------------------------------
DROP TABLE IF EXISTS `notes`;
CREATE TABLE `notes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `content` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime(6) DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `class_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKechaouoa6kus6k1dpix1u91c` (`user_id`),
  KEY `FK4suh5rnuy4tg0h8ofk0jnrf9u` (`class_id`),
  CONSTRAINT `FK4suh5rnuy4tg0h8ofk0jnrf9u` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `FKechaouoa6kus6k1dpix1u91c` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `notes` (`id`, `content`, `created_at`, `title`, `updated_at`, `user_id`, `class_id`) VALUES
(14, 'ebry', '2026-04-05 08:47:02.167301', ' ẻby', '2026-04-05 08:47:02.167301', 8, 13),
(15, 'etheth', '2026-04-05 08:47:33.229925', 'hrethet', '2026-04-05 08:47:33.229925', 9, 13),
(16, 'segseg', '2026-04-05 08:54:41.133143', 'fe', '2026-04-05 08:54:41.133143', 8, 13);

-- --------------------------------------------------------
-- View structure for `v_exam_results`
-- --------------------------------------------------------
DROP VIEW IF EXISTS `v_exam_results`;
CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_exam_results` AS SELECT `u`.`full_name` AS `full_name`, `e`.`exam_title` AS `exam_title`, `e`.`exam_code` AS `exam_code`, `a`.`score` AS `score`, `l`.`tab_switch` AS `tab_switch`, `l`.`copy_count` AS `copy_count`, `l`.`screenshot_count` AS `screenshot_count` FROM (((`exam_attempts` `a` join `users` `u` on((`a`.`student_id` = `u`.`id`))) join `exams` `e` on((`a`.`exam_id` = `e`.`id`))) left join `exam_logs` `l` on((`a`.`id` = `l`.`attempt_id`))) ;

COMMIT;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
