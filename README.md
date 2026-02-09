# ExamWeb - Hệ Thống Thi Trực Tuyến

Một REST API toàn diện cho nền tảng thi trực tuyến được xây dựng bằng Spring Boot. Ứng dụng này hỗ trợ xác thực người dùng, quản lý lớp học và kỳ thi, cũng như phân quyền dựa trên vai trò cho admin, giáo viên và sinh viên.

## Tính Năng

*   **Xác thực**: Đăng ký người dùng cục bộ và xác thực dựa trên JWT.
*   **Vai trò người dùng**: Các vai trò Admin, Teacher (Giáo viên), và Student (Sinh viên) với các quyền riêng biệt.
*   **Quản lý Lớp học**: Giáo viên có thể tạo và quản lý các lớp học với mã mời duy nhất.
*   **Ghi danh**: Sinh viên có thể tham gia lớp học bằng mã mời.
*   **Quản lý Kỳ thi**: Giáo viên có thể tạo các kỳ thi cho lớp học của mình và thêm câu hỏi.
*   **Làm bài thi**: Sinh viên đã ghi danh có thể bắt đầu và nộp bài thi.
*   **Trang quản trị**: Admin có thể quản lý tất cả người dùng và vai trò của họ.

## Yêu Cầu Cần Có

*   **Java JDK 21** hoặc mới hơn.
*   **Maven 3.x** hoặc mới hơn.
*   **MySQL Server 8.x** hoặc mới hơn.
*   Một trình kliên API như **Postman**.
*   Một IDE như **IntelliJ IDEA** hoặc **VS Code**.

## 1. Cài Đặt & Cấu Hình

### Cài Đặt Cơ Sở Dữ Liệu

1.  **Khởi động MySQL server của bạn.**
2.  Tạo cơ sở dữ liệu cho dự án. Bạn có thể sử dụng lệnh sau trong trình kliên MySQL:
    ```sql
    CREATE DATABASE online_exam_system;
    ```
3.  Thực thi toàn bộ kịch bản `examweb.sql` nằm ở thư mục gốc của dự án. Thao tác này sẽ tạo tất cả các bảng cần thiết và chèn các vai trò mặc định (admin, teacher, student).

### Cấu Hình Ứng Dụng

1.  Mở tệp `src/main/resources/application.properties`.
2.  Cập nhật các thuộc tính kết nối cơ sở dữ liệu để khớp với cài đặt MySQL cục bộ của bạn:
    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/online_exam_system
    spring.datasource.username=root
    spring.datasource.password=MAT_KHAU_MYSQL_CUA_BAN
    ```
3.  (Tùy chọn) Để đăng nhập bằng Google OAuth2 hoạt động, bạn phải thay thế thông tin xác thực giữ chỗ bằng thông tin của riêng bạn từ Google Cloud Console:
    ```properties
    spring.security.oauth2.client.registration.google.client-id=ID_KHACH_GOOGLE_CUA_BAN
    spring.security.oauth2.client.registration.google.client-secret=MA_BI_MAT_KHACH_GOOGLE_CUA_BAN
    ```

## 2. Chạy Ứng Dụng

### Từ IDE Của Bạn

1.  Mở dự án trong IDE của bạn (ví dụ: IntelliJ IDEA).
2.  Tìm tệp `ExamwebApplication.java`.
3.  Chạy phương thức `main`.

### Từ Dòng Lệnh

1.  Mở một terminal trong thư mục gốc của dự án.
2.  Chạy lệnh Maven sau:
    ```bash
    mvn spring-boot:run
    ```

Ứng dụng sẽ khởi động tại `http://localhost:8080`.

## 3. Kiểm Thử API với Postman

Một bộ sưu tập Postman (Postman collection) toàn diện được cung cấp dưới đây để kiểm thử toàn bộ luồng API.

### Yêu Cầu Bắt Buộc: Tạo Người Dùng Admin

API quản lý người dùng chỉ dành cho admin. Bạn phải tự tạo một người dùng admin trong cơ sở dữ liệu của mình trước khi có thể kiểm thử các tính năng này.

1.  Đăng ký một người dùng mới thông qua API hoặc giao diện web của ứng dụng (ví dụ: với tên người dùng là `admin_user`).
2.  Tìm `id` của người dùng mới này trong bảng `users` của bạn.
3.  Chạy lệnh SQL sau, thay thế `USER_ID_HERE` bằng ID bạn đã tìm thấy:
    ```sql
    -- Vai trò 'admin' có ID là 1, được định nghĩa trong examweb.sql
    INSERT INTO user_role (user_id, role_id) VALUES (USER_ID_HERE, 1);
    ```

### Bộ Sưu Tập Postman

import từ nhửng json trong folder "postman api test" để chạy thử api
