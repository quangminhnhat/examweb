// ==========================================
// KHỞI TẠO KHI TẢI TRANG
// ==========================================
document.addEventListener("DOMContentLoaded", function() {
    window.fetchStudentData();
});

// ==========================================
// 1. TẢI DỮ LIỆU TỔNG QUAN CỦA SINH VIÊN
// ==========================================
window.fetchStudentData = function() {
    // Tải danh sách lớp
    fetch('/api/classes')
        .then(res => res.json())
        .then(classes => {
            const list = document.getElementById('studentClassesList');
            list.innerHTML = '';

            if (classes.length === 0) {
                list.innerHTML = `<div class="col-12 text-center py-5 bg-white rounded-4 shadow-sm">
                    <i class="bi bi-emoji-frown display-4 text-muted"></i>
                    <p class="mt-2 text-secondary">Bạn chưa tham gia lớp học nào.</p>
                </div>`;
                return;
            }

            let html = '';
            classes.forEach(cls => {
                const teacherName = cls.teacher ? (cls.teacher.fullName || cls.teacher.username) : "Hệ thống";
                html += `
                <div class="col-md-6 position-relative">
                    <div class="card class-card shadow-sm h-100 border-start border-4 border-primary">
                        <div class="card-body p-4">
                            <h5 class="fw-bold text-dark mb-1">${cls.className}</h5>
                            <p class="text-secondary small mb-3"><i class="bi bi-person-badge me-1"></i> GV: ${teacherName}</p>
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="badge bg-light text-primary border border-primary px-3 rounded-pill">Mã lớp: ${cls.inviteCode}</span>
                                <div>
                                    <button class="btn btn-sm btn-link text-decoration-none fw-bold me-2" onclick="viewClassNotes(${cls.id}, '${cls.className}')">Xem ghi chú</button>
                                    <button class="btn btn-sm btn-link text-decoration-none fw-bold" onclick="viewClassExams(${cls.id})">Xem bài thi &rarr;</button>
                                </div>
                            </div>
                        </div>
                        <button class="btn btn-danger btn-sm btn-leave rounded-circle shadow"
                                onclick="leaveClass(${cls.id}, '${cls.className}')" title="Thoát khỏi lớp">
                            <i class="bi bi-x-lg"></i>
                        </button>
                    </div>
                </div>`;
            });
            list.innerHTML = html;
        });

    // Tải danh sách kỳ thi đang mở
    fetch('/api/exams')
        .then(res => res.json())
        .then(exams => {
            const list = document.getElementById('studentExamsList');
            list.innerHTML = '';

            const now = new Date();
            // Lọc ra kỳ thi đang mở: (isOpen = true) HOẶC (đang trong thời gian hẹn giờ)
            const openExams = exams.filter(e => {
                // Kiểm tra cả 'isOpen' và 'open' để tránh lỗi JSON Mapping
                const isExplicitlyOpen = (e.isOpen === true || e.open === true);
                if (isExplicitlyOpen) return true;

                if (e.startTime) {
                    const start = new Date(e.startTime);
                    const end = e.endTime ? new Date(e.endTime) : null;
                    return now >= start && (!end || now <= end);
                }
                return false;
            });

            if (openExams.length === 0) {
                list.innerHTML = '<div class="text-center py-4"><i class="bi bi-cup-hot fs-1 text-muted"></i><p class="text-muted small mt-2">Hiện không có kỳ thi nào đang mở.</p></div>';
                return;
            }

            let html = '';
            openExams.forEach(exam => {
                html += `
                <div class="list-group-item exam-item p-3 border-0 shadow-sm d-flex justify-content-between align-items-center">
                    <div>
                        <div class="fw-bold text-dark">${exam.examTitle}</div>
                        <div class="small text-secondary">Mã thi: <span class="text-success fw-bold">${exam.examCode}</span></div>
                    </div>
                    <button class="btn btn-sm btn-success rounded-pill px-3 fw-bold shadow-sm"
                            onclick="quickStart('${exam.examCode}')">VÀO THI</button>
                </div>`;
            });
            list.innerHTML = html;
        });
};

// ==========================================
// 2. TÍNH NĂNG: GHI DANH LỚP HỌC MỚI
// ==========================================
window.joinClass = function() {
    const inviteCode = document.getElementById('inviteCodeInput').value.trim();
    
    if (!inviteCode) {
        alert("Vui lòng nhập mã mời (Invite Code) để tham gia!");
        return;
    }

    fetch('/api/classes/join', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ inviteCode: inviteCode })
    })
    .then(res => {
        if (res.ok) {
            alert("Chúc mừng! Bạn đã ghi danh vào lớp thành công.");
            // Đóng Modal an toàn
            const modalEl = document.getElementById('joinClassModal');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();
            
            // Xóa input và tải lại danh sách lớp
            document.getElementById('inviteCodeInput').value = '';
            window.fetchStudentData();
        } else {
            alert("Mã mời không tồn tại hoặc bạn đã ở trong lớp này rồi!");
        }
    })
    .catch(err => console.error("Lỗi khi tham gia lớp:", err));
};

// ==========================================
// 3. TÍNH NĂNG: VÀO THI NGAY
// ==========================================
window.startExam = function() {
    const examCode = document.getElementById('quickExamCode').value.trim();
    
    if (!examCode) {
        alert("Vui lòng nhập mã đề thi (Exam Code)!");
        return;
    }

    // Gửi yêu cầu bắt đầu kỳ thi
    fetch('/api/exams/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ examCode: examCode })
    })
    .then(async res => {
        if (res.ok) {
            const attempt = await res.json();
            alert("Truy cập phòng thi thành công! Chuẩn bị bắt đầu...");
            // CHÚ Ý: Điều hướng sang trang làm bài thi.
            // URL này phải khớp với Controller của bạn (Thường là truyền attemptId)
            window.location.href = `/student/take-exam/${attempt.id}`;
        } else {
            alert("Không thể vào thi! Có thể đề thi đang bị khóa, chưa tới giờ, hoặc bạn nhập sai mã.");
        }
    })
    .catch(err => console.error("Lỗi khi vào thi:", err));
};

// ==========================================
// 4. CÁC TÍNH NĂNG HỖ TRỢ KHÁC
// ==========================================
window.leaveClass = function(classId, className) {
    if (confirm(`CẢNH BÁO: Bạn có chắc chắn muốn rời khỏi lớp "${className}"? Bạn sẽ không thể làm các bài thi của lớp này nữa.`)) {
        fetch(`/api/classes/${classId}/leave`, { method: 'DELETE' })
            .then(res => {
                if (res.ok) {
                    alert("Đã rời khỏi lớp.");
                    window.fetchStudentData();
                } else {
                    alert("Lỗi hệ thống khi rời lớp.");
                }
            });
    }
};

window.quickStart = function(code) {
    document.getElementById('quickExamCode').value = code;
    window.startExam(); // Gọi thẳng hàm startExam
};

window.viewClassExams = function(classId) {
    // Focus vào ô nhập mã thi để hướng dẫn sinh viên
    document.getElementById('quickExamCode').focus();
    alert("Vui lòng xem các kỳ thi đang mở ở cột bên phải, hoặc nhập Mã thi để làm bài!");
};

window.viewClassNotes = function(classId, className) {
    document.getElementById('notesClassName').innerText = className;
    $('#viewNotesModal').modal('show');
    fetchNotesForClass(classId);
};

window.fetchNotesForClass = function(classId) {
    const container = document.getElementById('notesContainer');
    container.innerHTML = `
        <div class="col-12 text-center py-4">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-2 text-muted">Đang tải ghi chú...</p>
        </div>
    `;

    fetch('/api/notes/class/' + classId)
        .then(res => res.json())
        .then(data => {
            if (!data || data.length === 0) {
                container.innerHTML = '<div class="col-12 text-center py-5 text-muted">Chưa có ghi chú nào trong lớp này.</div>';
                return;
            }

            let html = '';
            data.forEach((note, index) => {
                const title = note.title || "Ghi chú không tiêu đề";
                const content = note.content || "";
                const createdAt = note.createdAt ? new Date(note.createdAt).toLocaleDateString('vi-VN') : "N/A";
                const updatedAt = note.updatedAt ? new Date(note.updatedAt).toLocaleDateString('vi-VN') : "N/A";
                const author = note.user ? (note.user.fullName || note.user.username) : "Ẩn danh";

                html += `
                    <div class="col-12">
                        <div class="card shadow-sm">
                            <div class="card-header bg-light d-flex justify-content-between align-items-center">
                                <h6 class="mb-0 fw-bold">${title}</h6>
                                <small class="text-muted">Tác giả: ${author}</small>
                            </div>
                            <div class="card-body">
                                <p class="mb-2">${content.replace(/\n/g, '<br>')}</p>
                                <small class="text-muted">Tạo: ${createdAt} | Cập nhật: ${updatedAt}</small>
                            </div>
                        </div>
                    </div>
                `;
            });
            container.innerHTML = html;
        })
        .catch(error => {
            container.innerHTML = '<div class="col-12 text-center py-4 text-danger">Lỗi khi tải ghi chú. Vui lòng thử lại.</div>';
            console.error("Lỗi tải ghi chú:", error);
        });
};
