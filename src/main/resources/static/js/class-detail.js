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
    fetchNotes(classId);
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
                        <div class="btn-group" role="group">
                            <button class="btn btn-sm btn-outline-primary rounded-pill px-3 me-1" onclick="editExamName(${exam.id}, '${title.replace(/'/g, "\\'")}')">
                                <i class="bi bi-pencil"></i> Sửa tên
                            </button>
                            <button class="btn btn-sm btn-outline-danger rounded-pill px-3 me-1" onclick="deleteExam(${exam.id}, '${title.replace(/'/g, "\\'")}')">
                                <i class="bi bi-trash"></i> Xóa
                            </button>
                            <button class="btn btn-sm btn-primary rounded-pill px-3" onclick="viewExamDetails(${exam.id})">
                                Quản lý câu hỏi
                            </button>
                        </div>
                    </td>
                </tr>`;
            });
            tbody.innerHTML = rows;
        })
        .catch(error => console.error("Lỗi tải danh sách kỳ thi:", error));
}

// 4. Tải danh sách ghi chú
function fetchNotes(classId) {
    fetch('/api/notes/class/' + classId)
        .then(res => res.json())
        .then(data => {
            const container = document.getElementById('notesContainer');

            if (!data || data.length === 0) {
                container.innerHTML = '<div class="text-center py-5 text-muted">Chưa có ghi chú nào. Hãy nhấn "Tạo ghi chú mới" để bắt đầu!</div>';
                return;
            }

            let html = '';
            data.forEach((note, index) => {
                const title = note.title || "Ghi chú không tiêu đề";
                const content = note.content || "";
                const createdAt = note.createdAt ? new Date(note.createdAt).toLocaleDateString('vi-VN') : "N/A";
                const updatedAt = note.updatedAt ? new Date(note.updatedAt).toLocaleDateString('vi-VN') : "N/A";

                html += `
                    <div class="card mb-3 shadow-sm">
                        <div class="card-header bg-light d-flex justify-content-between align-items-center">
                            <h6 class="mb-0 fw-bold">${title}</h6>
                            <div>
                                <button class="btn btn-sm btn-outline-primary me-1" onclick="editNote(${note.id}, '${title.replace(/'/g, "\\'")}', '${content.replace(/'/g, "\\'").replace(/\n/g, '\\n')}')">Sửa</button>
                                <button class="btn btn-sm btn-outline-danger" onclick="deleteNote(${note.id})">Xóa</button>
                            </div>
                        </div>
                        <div class="card-body">
                            <p class="mb-2">${content.replace(/\n/g, '<br>')}</p>
                            <small class="text-muted">Tạo: ${createdAt} | Cập nhật: ${updatedAt}</small>
                        </div>
                    </div>
                `;
            });
            container.innerHTML = html;
        })
        .catch(error => console.error("Lỗi tải danh sách ghi chú:", error));
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

// 7. Tạo ghi chú mới
window.createNote = function() {
    const titleInput = document.getElementById('noteTitleInput');
    const contentInput = document.getElementById('noteContentInput');
    const title = titleInput.value;
    const content = contentInput.value;
    const classId = document.getElementById('hiddenClassId').value;

    if (!title.trim()) {
        alert("Vui lòng nhập tiêu đề ghi chú!");
        return;
    }

    const payload = {
        title: title,
        content: content,
        classId: parseInt(classId)
    };

    fetch('/api/notes', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    }).then(res => {
        if (res.ok) {
            alert("Tạo ghi chú thành công!");
            $('#createNoteModal').modal('hide');
            titleInput.value = '';
            contentInput.value = '';
            fetchNotes(classId);
        } else {
            alert("Lỗi khi tạo ghi chú!");
        }
    }).catch(error => console.error("Lỗi tạo ghi chú:", error));
};

// 8. Chỉnh sửa ghi chú
window.editNote = function(id, title, content) {
    document.getElementById('editNoteId').value = id;
    document.getElementById('editNoteTitleInput').value = title;
    document.getElementById('editNoteContentInput').value = content.replace(/\\n/g, '\n').replace(/\\'/g, "'");
    $('#editNoteModal').modal('show');
};

// 9. Cập nhật ghi chú
window.updateNote = function() {
    const id = document.getElementById('editNoteId').value;
    const title = document.getElementById('editNoteTitleInput').value;
    const content = document.getElementById('editNoteContentInput').value;
    const classId = document.getElementById('hiddenClassId').value;

    if (!title.trim()) {
        alert("Vui lòng nhập tiêu đề ghi chú!");
        return;
    }

    const payload = {
        title: title,
        content: content
    };

    fetch('/api/notes/' + id, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    }).then(res => {
        if (res.ok) {
            alert("Cập nhật ghi chú thành công!");
            $('#editNoteModal').modal('hide');
            fetchNotes(classId);
        } else {
            alert("Lỗi khi cập nhật ghi chú!");
        }
    }).catch(error => console.error("Lỗi cập nhật ghi chú:", error));
};

// 10. Xóa ghi chú
window.deleteNote = function(id) {
    const classId = document.getElementById('hiddenClassId').value;

    if (confirm("Bạn có chắc chắn muốn xóa ghi chú này?")) {
        fetch('/api/notes/' + id, {
            method: 'DELETE'
        }).then(res => {
            if (res.ok) {
                alert("Xóa ghi chú thành công!");
                fetchNotes(classId);
            } else {
                alert("Lỗi khi xóa ghi chú!");
            }
        }).catch(error => console.error("Lỗi xóa ghi chú:", error));
    }
};

// 11. Mở modal chỉnh sửa tên kỳ thi
window.editExamName = function(examId, currentName) {
    document.getElementById('editExamIdInput').value = examId;
    document.getElementById('editExamTitleInput').value = currentName;
    const modalEl = document.getElementById('editExamModal');
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
};

window.saveEditedExamName = function() {
    const examId = document.getElementById('editExamIdInput').value;
    const newName = document.getElementById('editExamTitleInput').value;
    const classId = document.getElementById('hiddenClassId').value;

    if (!newName || !newName.trim()) {
        alert("Vui lòng nhập tên kỳ thi mới!");
        return;
    }

    fetch('/api/exams/' + examId + '/title', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ examTitle: newName.trim() })
    }).then(res => {
        if (res.ok) {
            alert("Cập nhật tên kỳ thi thành công!");
            const modalEl = document.getElementById('editExamModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            fetchExams(classId);
        } else {
            alert("Lỗi khi cập nhật tên kỳ thi!");
        }
    }).catch(error => console.error("Lỗi cập nhật tên kỳ thi:", error));
};

// 12. Xóa kỳ thi
window.deleteExam = function(examId, examName) {
    const classId = document.getElementById('hiddenClassId').value;

    if (confirm(`Bạn có chắc chắn muốn xóa kỳ thi "${examName}"?\n\nLưu ý: Tất cả câu hỏi và kết quả thi sẽ bị xóa vĩnh viễn!`)) {
        fetch('/api/exams/' + examId, {
            method: 'DELETE'
        }).then(res => {
            if (res.ok) {
                alert("Xóa kỳ thi thành công!");
                fetchExams(classId);
            } else {
                alert("Lỗi khi xóa kỳ thi!");
            }
        }).catch(error => console.error("Lỗi xóa kỳ thi:", error));
    }
};

// Import entire exam from Excel
window.importEntireExam = function() {
    const fileInput = document.getElementById('excelFileInput');
    const file = fileInput.files[0];
    const classId = document.getElementById('hiddenClassId').value;

    if (!file) {
        alert("Vui lòng chọn file Excel!");
        return;
    }

    const formData = new FormData();
    formData.append('file', file);
    if (classId) {
        formData.append('classId', classId);
    }

    // Show loading
    const importBtn = document.querySelector('#importExamModal .btn-success');
    const originalText = importBtn.textContent;
    importBtn.textContent = 'Đang import...';
    importBtn.disabled = true;

    fetch('/api/exams/import-exam', {
        method: 'POST',
        body: formData
    })
    .then(res => res.json())
    .then(data => {
        if (data.success) {
            alert(data.message);
            $('#importExamModal').modal('hide');
            fileInput.value = '';
            // Reload exams to show the new exam
            fetchExams(classId);
        } else {
            alert('Lỗi: ' + data.message);
        }
    })
    .catch(error => {
        console.error("Lỗi import:", error);
        alert("Lỗi khi import file. Vui lòng kiểm tra định dạng file.");
    })
    .finally(() => {
        // Reset button
        importBtn.textContent = originalText;
        importBtn.disabled = false;
    });
};

// Download Excel template
window.downloadTemplate = function() {
    window.location.href = '/api/exams/template';
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