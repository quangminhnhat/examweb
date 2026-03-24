let currentQuestionsList = [];
let totalQuestionsCount = 0; // Biến lưu tổng số câu hỏi để tính thang điểm 10

document.addEventListener("DOMContentLoaded", function() {
    fetchExamInfo();
    fetchQuestions();
    fetchResults(); // GỌI THÊM HÀM LẤY DANH SÁCH ĐIỂM
});

// Hàm hỗ trợ copy nhanh
function copyText(element) {
    const text = element.innerText;
    navigator.clipboard.writeText(text).then(() => {
        const originalText = element.innerText;
        element.innerText = "COPIED!";
        element.classList.replace('text-dark', 'text-success');
        setTimeout(() => {
            element.innerText = originalText;
            element.classList.replace('text-success', 'text-dark');
        }, 1000);
    });
}

// 1. Lấy thông tin Header và Settings
function fetchExamInfo() {
    const examId = document.getElementById('hiddenExamId').value;
    fetch('/api/exams')
        .then(res => res.json())
        .then(data => {
            const exam = data.find(e => e.id == examId);
            if(exam) {
                document.getElementById('examTitleHeader').innerText = exam.examTitle || "Kỳ thi không tên";
                document.getElementById('examCodeDisplay').innerText = exam.examCode;
                totalQuestionsCount = exam.questions ? exam.questions.length : 0;

                // Cập nhật trạng thái Toggle
                const toggle = document.getElementById('examStatusToggle');
                const label = document.getElementById('statusLabel');
                toggle.checked = exam.isOpen;
                label.innerText = exam.isOpen ? "Đang mở cho sinh viên" : "Đang đóng hoàn toàn";
                if(exam.isOpen) label.classList.add('text-success');
                else label.classList.remove('text-success');
            }
        });
}

// 2. Lấy danh sách câu hỏi
function fetchQuestions() {
    const examId = document.getElementById('hiddenExamId').value;
    fetch('/api/exams')
        .then(response => response.json())
        .then(data => {
            const tbody = document.getElementById('questionListBody');
            const currentExam = data.find(exam => exam.id == examId);

            if (!currentExam || !currentExam.questions || currentExam.questions.length === 0) {
                tbody.innerHTML = '<tr><td colspan="4" class="text-center py-5 text-muted">Chưa có câu hỏi nào. Nhấn "Thêm câu hỏi" để bắt đầu.</td></tr>';
                return;
            }

            currentQuestionsList = currentExam.questions;
            totalQuestionsCount = currentQuestionsList.length; // Cập nhật lại số câu hỏi
            tbody.innerHTML = '';

            currentQuestionsList.forEach((q, index) => {
                const row = `<tr>
                    <td class="fw-bold text-secondary">#${index + 1}</td>
                    <td><div class="fw-bold text-dark">${q.content}</div></td>
                    <td>
                        <div class="small">
                            <span class="badge ${q.correctOption==='A'?'bg-success':'bg-light text-dark'}">A</span> ${q.optionA} <br>
                            <span class="badge ${q.correctOption==='B'?'bg-success':'bg-light text-dark'}">B</span> ${q.optionB}
                        </div>
                    </td>
                    <td class="text-end">
                        <button class="btn btn-sm btn-outline-primary border-0" onclick="openModalForEditQuestion(${q.id})">Sửa</button>
                        <button class="btn btn-sm btn-outline-danger border-0" onclick="deleteQuestion(${q.id})">Xóa</button>
                    </td>
                </tr>`;
                tbody.innerHTML += row;
            });
            fetchResults(); // Tải lại điểm sau khi đã có tổng số câu
        });
}

// 3. TÍNH NĂNG MỚI: LẤY DANH SÁCH KẾT QUẢ
function fetchResults() {
    const examId = document.getElementById('hiddenExamId').value;
    fetch(`/api/exams/${examId}/results`)
        .then(res => res.json())
        .then(data => {
            const tbody = document.getElementById('resultsTableBody');

            if (!data || data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="3" class="text-center py-5 text-muted">Chưa có sinh viên nào nộp bài.</td></tr>';
                return;
            }

            let html = '';
            data.forEach(attempt => {
                const studentName = attempt.student ? (attempt.student.fullName || attempt.student.username) : "Ẩn danh";

                // Thuật toán tính thang điểm 10
                let finalScore = 0;
                if (totalQuestionsCount > 0) {
                    finalScore = ((attempt.score / totalQuestionsCount) * 10).toFixed(2);
                }

                // Định dạng ngày nộp
                let submitDate = "Chưa nộp / Đang làm";
                if(attempt.submittedAt) {
                    submitDate = new Date(attempt.submittedAt).toLocaleString('vi-VN');
                }

                html += `<tr>
                    <td class="fw-bold text-dark">${studentName}</td>
                    <td><span class="badge bg-success fs-6">${finalScore} / 10</span> <br> <small class="text-muted">Đúng: ${attempt.score} câu</small></td>
                    <td class="text-secondary small">${submitDate}</td>
                </tr>`;
            });
            tbody.innerHTML = html;
        })
        .catch(err => console.error("Lỗi tải kết quả:", err));
}

// =====================================
// CÁC HÀM THAO TÁC CÂU HỎI & CÀI ĐẶT
// =====================================
function openModalForCreateQuestion() {
    document.getElementById('hiddenQuestionId').value = '';
    document.getElementById('questionContent').value = '';
    document.getElementById('optionA').value = '';
    document.getElementById('optionB').value = '';
    document.getElementById('optionC').value = '';
    document.getElementById('optionD').value = '';
    document.getElementById('correctAnswer').value = 'A';
    document.getElementById('questionModalTitle').innerText = 'Thêm Câu Hỏi Mới';
    new bootstrap.Modal(document.getElementById('questionModal')).show();
}

function openModalForEditQuestion(questionId) {
    const q = currentQuestionsList.find(item => item.id == questionId);
    if(q) {
        document.getElementById('hiddenQuestionId').value = q.id;
        document.getElementById('questionContent').value = q.content;
        document.getElementById('optionA').value = q.optionA;
        document.getElementById('optionB').value = q.optionB;
        document.getElementById('optionC').value = q.optionC;
        document.getElementById('optionD').value = q.optionD;
        document.getElementById('correctAnswer').value = q.correctOption;
        document.getElementById('questionModalTitle').innerText = 'Chỉnh Sửa Câu Hỏi';
        new bootstrap.Modal(document.getElementById('questionModal')).show();
    }
}

function saveQuestion() {
    const examId = document.getElementById('hiddenExamId').value;
    const questionId = document.getElementById('hiddenQuestionId').value;

    const payload = {
        content: document.getElementById('questionContent').value,
        optionA: document.getElementById('optionA').value,
        optionB: document.getElementById('optionB').value,
        optionC: document.getElementById('optionC').value,
        optionD: document.getElementById('optionD').value,
        correctOption: document.getElementById('correctAnswer').value
    };

    const url = questionId ? `/api/exams/questions/${questionId}` : `/api/exams/${examId}/questions`;
    const method = questionId ? 'PUT' : 'POST';

    fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    }).then(res => {
        if(res.ok) {
            bootstrap.Modal.getInstance(document.getElementById('questionModal')).hide();
            fetchQuestions(); // Tải lại danh sách câu hỏi
        } else {
            alert("Lỗi khi lưu câu hỏi!");
        }
    });
}

function deleteQuestion(id) {
    if(confirm("Xóa câu hỏi này? Hành động này sẽ ảnh hưởng đến điểm số của sinh viên đã thi.")) {
        fetch(`/api/exams/questions/${id}`, { method: 'DELETE' })
            .then(res => { if(res.ok) fetchQuestions(); });
    }
}

function toggleStatus() {
    const examId = document.getElementById('hiddenExamId').value;
    fetch(`/api/exams/${examId}/toggle-status`, { method: 'PATCH' })
        .then(res => res.json())
        .then(isOpen => {
            fetchExamInfo(); // Cập nhật lại Label
        });
}

function saveSchedule() {
    const examId = document.getElementById('hiddenExamId').value;
    fetch(`/api/exams/${examId}/schedule`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            startTime: document.getElementById('startTimeInput').value,
            endTime: document.getElementById('endTimeInput').value
        })
    }).then(res => { if(res.ok) alert("Đã cập nhật lịch thi thành công!"); });
}