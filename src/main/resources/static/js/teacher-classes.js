// Khai báo biến modal toàn cục để đóng/mở dễ dàng
let classModal;

document.addEventListener("DOMContentLoaded", function() {
    // Khởi tạo Bootstrap Modal
    classModal = new bootstrap.Modal(document.getElementById('classModal'));
    fetchClasses();
});

// 1. Lấy danh sách lớp và hiển thị
function fetchClasses() {
    fetch('/api/classes')
        .then(res => res.json())
        .then(data => {
            const tbody = document.getElementById('classListBody');
            tbody.innerHTML = '';

            if (data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center py-4 text-muted">Bạn chưa có lớp học nào.</td></tr>';
                return;
            }

            data.forEach((cls, index) => {
                const row = `
                <tr>
                    <td class="py-3 px-4 fw-bold text-secondary">#${index + 1}</td>
                    <td class="py-3 px-4 fw-bold text-dark">${cls.className}</td>
                    <td class="py-3 px-4">
                        <span class="badge bg-white text-primary border border-primary" style="cursor:pointer" onclick="copyInviteCode('${cls.inviteCode}')">
                            ${cls.inviteCode} <i class="bi bi-clipboard ms-1"></i>
                        </span>
                    </td>
                    <td class="py-3 px-4 text-end">
                        <button class="btn btn-sm btn-outline-primary rounded-pill px-3 me-1" onclick="window.location.href='/teacher/classes/${cls.id}'">Chi tiết</button>
                        <button class="btn btn-sm btn-outline-danger rounded-pill px-3" onclick="deleteClass(${cls.id})">Xóa</button>
                    </td>
                </tr>`;
                tbody.innerHTML += row;
            });
        });
}

// 2. Mở Modal tạo lớp (Đây là hàm mà nút của bạn đang gọi)
function openModalForCreate() {
    document.getElementById('hiddenClassId').value = '';
    document.getElementById('classNameInput').value = '';
    document.getElementById('modalTitle').innerText = 'Tạo Lớp Học Mới';
    classModal.show();
}

// 3. Lưu lớp học (POST hoặc PUT)
function saveClass() {
    const className = document.getElementById('classNameInput').value;
    const classId = document.getElementById('hiddenClassId').value;

    if (!className.trim()) {
        alert("Vui lòng nhập tên lớp học!");
        return;
    }

    const payload = { className: className };
    const method = classId ? 'PUT' : 'POST';
    const url = classId ? `/api/classes/${classId}` : '/api/classes';

    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json'
            // Nếu bạn dùng Spring Security, có thể cần thêm CSRF token ở đây
        },
        body: JSON.stringify(payload)
    })
    .then(response => {
        if (response.ok) {
            classModal.hide();
            fetchClasses(); // Tải lại danh sách ngay lập tức
            alert("Đã lưu lớp học thành công!");
        } else {
            alert("Lỗi khi lưu lớp học. Hãy kiểm tra Console!");
        }
    })
    .catch(error => console.error('Lỗi:', error));
}

// 4. Xóa lớp học
function deleteClass(id) {
    if (confirm("Xóa lớp sẽ xóa toàn bộ đề thi bên trong. Bạn chắc chứ?")) {
        fetch(`/api/classes/${id}`, { method: 'DELETE' })
            .then(res => {
                if (res.ok) fetchClasses();
                else alert("Xóa thất bại!");
            });
    }
}

function copyInviteCode(code) {
    navigator.clipboard.writeText(code).then(() => alert("Đã copy mã mời: " + code));
}