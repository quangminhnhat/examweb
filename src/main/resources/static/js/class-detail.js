// ==========================================
// SỰ KIỆN KHI TRANG VỪA TẢI XONG
// ==========================================
document.addEventListener("DOMContentLoaded", function() {
    const hiddenInput = document.getElementById('hiddenClassId');
    
    // Kiểm tra an toàn: Đảm bảo có ID lớp học trước khi gọi API
    if (!hiddenInput || !hiddenInput.value || hiddenInput.value === 'null' || hiddenInput.value === '') {
        alert("Lỗi: Không tìm thấy ID lớp học. Đang quay lại trang chủ...");
        window.location.href = "/teacher/classes";
        return;
    }

    const classId = hiddenInput.value;
    console.log("Đang tải dữ liệu cho lớp ID:", classId);
    
    // Gọi hàm load dữ liệu
    loadDashboard(classId);
});

function loadDashboard(classId) {
    fetchClassInfo(classId);
    fetchStudents(classId);
    fetchExams(classId);
}

// ==========================================
// CÁC HÀM FETCH DỮ LIỆU TỪ BACKEND
// ==========================================

// 1. Tải thông tin lớp học
function fetchClassInfo(classId) {
    fetch('/api/classes/' + classId)
        .then(res => {
            if (!res.ok) throw new Error("Không thể tải thông tin lớp");
            return res.json();
        })
        .then(data => {
            document.getElementById('displayClassName').innerText = data.className || "Lớp học không tên";
            document.getElementById('displayInviteCode').innerText = data.inviteCode || "---";

            // Cập nhật số lượng sinh viên lên thẻ thống kê ở Header
            const sCount = data.students ? data.students.length : 0;
            const studentCountHeader = document.getElementById('studentCountHeader');
            if(studentCountHeader) studentCountHeader.innerText = sCount;
        })
        .catch(error => console.error("Lỗi tải thông tin lớp:", error));
}

// 2. Tải danh sách sinh viên
function fetchStudents(classId) {
    fetch('/api/classes/' + classId + '/students')
        .then(res => res.json())
        .then(data => {
            const tbody = document.getElementById('studentListBody');
            
            if (!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="3" class="text-center py-4 text-muted">Chưa có sinh viên nào tham gia lớp này.</td></tr>';
                return;
            }

            // Dùng chuỗi tích lũy để không bị đè dữ liệu
            let rows = '';
            data.forEach((student, index) => {
                const name = student.fullName || student.username || "Chưa cập nhật";
                const email = student.email || "Không có email";
                
                rows += `<tr>
                    <td class="fw-bold text-secondary">${index + 1}</td>
                    <td class="fw-bold text-dark">${name}</td>
                    <td class="text-secondary">${email}</td>
                </tr>`;
            });
            tbody.innerHTML = rows;
        })
        .catch(error => console.error("Lỗi tải danh sách sinh viên:", error));
}

// 3. Tải danh sách kỳ thi
function fetchExams(classId) {
    // Thêm ?t=... để chống trình duyệt lưu cache (giúp hiện đề mới ngay lập tức)
    fetch('/api/exams?t=' + new Date().getTime())
        .then(res => res.json())
        .then(data => {
            const tbody = document.getElementById('examListBody');

            // LỌC DỮ LIỆU ĐÃ ĐƯỢC FIX LỖI @JsonIdentityInfo CỦA SPRING BOOT
            const classExams = data.filter(e => {
                if (!e.classEntity) return false;

                // Nếu Spring Boot trả về dạng Object {id: 8}
                if (typeof e.classEntity === 'object' && e.classEntity.id == classId) {
                    return true;
                }

                // Nếu Spring Boot rút gọn thành dạng số 8
                if (e.classEntity == classId) {
                    return true;
                }

                return false;
            });
            
            // Cập nhật số lượng bài thi lên thẻ thống kê ở Header
            const examCountHeader = document.getElementById('examCountHeader');
            if(examCountHeader) examCountHeader.innerText = classExams.length;

            if (classExams.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-muted">Chưa có bài tập nào. Hãy nhấn "Tạo đề mới" để bắt đầu!</td></tr>';
                return;
            }

            // Dùng chuỗi tích lũy để đảm bảo hiển thị TẤT CẢ các bài tập
            let rows = '';
            classExams.forEach((exam, index) => {
                const title = exam.examTitle || "Kỳ thi không tên";
                const code = exam.examCode || "N/A";
                
                rows += `<tr>
                    <td class="fw-bold text-secondary ps-3">${index + 1}</td>
                    <td>
                        <div class="fw-bold text-dark">${title}</div>
                    </td>
                    <td><span class="badge bg-danger">${code}</span></td>
                    <td class="text-end pe-3">
                        <button class="btn btn-sm btn-primary rounded-pill px-3 fw-bold shadow-sm" onclick="viewExamDetails(${exam.id})">
                            Quản lý câu hỏi
                        </button>
                    </td>
                </tr>`;
            });
            tbody.innerHTML = rows;
        })
        .catch(error => console.error("Lỗi tải danh sách kỳ thi:", error));
}

// ==========================================
// CÁC HÀM THAO TÁC (Được gắn vào đối tượng window để chống lỗi Scope)
// ==========================================

// 4. Tạo kỳ thi mới
window.createExam = function() {
    const titleInput = document.getElementById('examTitleInput');
    const title = titleInput.value;
    const classId = document.getElementById('hiddenClassId').value;

    if (!title.trim()) {
        alert("Vui lòng nhập tên kỳ thi!");
        return;
    }

    // Tạo mã đề ngẫu nhiên (Ví dụ: EXAM-6821)
    const randomExamCode = "EXAM-" + Math.floor(1000 + Math.random() * 9000);
    
    // Gói dữ liệu gửi xuống Backend
    const payload = {
        examTitle: title,
        examCode: randomExamCode,
        duration: 45,        // Thời gian làm bài mặc định 45 phút
        totalScore: 100,     // Tổng điểm mặc định 100
        isOpen: false,       // Mặc định khóa đề khi mới tạo
        classEntity: { id: parseInt(classId) }
    };

    fetch('/api/exams', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    }).then(res => {
        if (res.ok) {
            alert("Tạo đề thi thành công!");
            // Cách tốt nhất để Modal đóng sạch sẽ và dữ liệu mới được tải lên
            window.location.reload(); 
        } else {
            alert("Lỗi khi tạo đề. Vui lòng kiểm tra quyền giáo viên hoặc Console!");
        }
    }).catch(error => console.error("Lỗi tạo đề:", error));
};

// 5. Xóa lớp hiện tại
window.deleteCurrentClass = function() {
    const classId = document.getElementById('hiddenClassId').value;
    
    if (confirm("CẢNH BÁO: Bạn có chắc chắn muốn xóa lớp học này? Toàn bộ đề thi, sinh viên và kết quả bên trong sẽ bị mất vĩnh viễn!")) {
        fetch('/api/classes/' + classId, { method: 'DELETE' })
            .then(res => {
                if (res.ok) {
                    alert("Đã xóa lớp học thành công!");
                    window.location.href = "/teacher/classes"; // Trở về danh sách lớp
                } else {
                    alert("Lỗi xóa lớp! Có thể do ràng buộc dữ liệu từ phía Database.");
                }
            })
            .catch(error => console.error("Lỗi xóa lớp:", error));
    }
};

// 6. Điều hướng sang chi tiết kỳ thi
window.viewExamDetails = function(examId) {
    window.location.href = '/teacher/exams/' + examId;
};

// 7. Tiện ích Copy mã mời
window.copyToClipboard = function(text) {
    navigator.clipboard.writeText(text).then(() => {
        alert("Đã copy mã mời: " + text);
    }).catch(err => {
        console.error('Không thể copy', err);
        alert("Trình duyệt không hỗ trợ copy tự động!");
    });
};