document.addEventListener("DOMContentLoaded", function() {
    // 1. Lấy ID phiên thi từ thẻ input ẩn trong HTML
    const attemptIdInput = document.getElementById('hiddenAttemptId');

    if (!attemptIdInput || !attemptIdInput.value) {
        alert("Lỗi: Không tìm thấy ID bài làm!");
        window.location.href = "/student/dashboard";
        return;
    }

    const attemptId = attemptIdInput.value;

    // 2. Gọi API để lấy kết quả
    fetch(`/api/exams/attempts/${attemptId}`)
        .then(res => {
            if(!res.ok) throw new Error("Lỗi tải kết quả từ máy chủ");
            return res.json();
        })
        .then(data => {
            // Lấy dữ liệu từ Map trả về của Backend
            const exam = data.exam;
            const attempt = data.attempt;

            // Gán Điểm số (Thang điểm 10)
            document.getElementById('displayScore').innerText = data.finalScore || 0;

            // Gán Tên và Tổng số câu
            document.getElementById('examTitle').innerText = exam.examTitle || "Kỳ thi không tên";
            document.getElementById('totalQuestions').innerText = exam.questions ? exam.questions.length : 0;

            // Gán Số câu trả lời đúng
            document.getElementById('correctAnswers').innerText = data.correctCount || 0;

            // Định dạng thời gian nộp bài
            if (attempt.submittedAt) {
                const date = new Date(attempt.submittedAt);
                document.getElementById('submitTime').innerHTML = `<i class="bi bi-clock-history me-1"></i> Nộp lúc: ${date.toLocaleString('vi-VN')}`;
            }
        })
        .catch(err => {
            console.error("Lỗi:", err);
            document.getElementById('examTitle').innerText = "Không thể tải kết quả. Vui lòng thử lại!";
        });
});