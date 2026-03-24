let currentQuestionIndex = 0;
let questionsData = [];
let userAnswers = {};
let timeRemaining = 0;
let timerInterval = null;
let securityViolations = 0;
let isSubmitting = false; // Biến khóa thao tác khi đang nộp bài

const attemptId = document.getElementById('hiddenAttemptId').value;
let securityModal;

document.addEventListener("DOMContentLoaded", function() {
    if (!attemptId || attemptId === "null") return window.location.href = "/student/dashboard";
    securityModal = new bootstrap.Modal(document.getElementById('securityModal'), { backdrop: 'static', keyboard: false });
    securityModal.show();
});

window.startExamNow = function() {
    securityModal.hide();
    enableAntiCheat();
    fetchExamData();
};

function fetchExamData() {
    fetch(`/api/exams/attempts/${attemptId}`)
        .then(res => res.json())
        .then(data => {
            questionsData = data.exam.questions;
            timeRemaining = data.remainingSeconds;
            if (!questionsData || questionsData.length === 0) {
                document.getElementById('examContent').innerHTML = '<h4 class="text-danger text-center py-5">Đề thi trống!</h4>';
                return;
            }
            renderQuestions();
            renderSidebar();
            updateNavigationButtons();
            startTimer();
        })
        .catch(err => alert("Lỗi tải đề thi!"));
}

function renderQuestions() {
    const container = document.getElementById('examContent');
    let html = '';

    questionsData.forEach((q, index) => {
        html += `
        <div class="question-card ${index === 0 ? 'active' : ''}" id="question-card-${index}">
            <h4 class="fw-bold mb-4 text-primary">Câu ${index + 1}. <span class="text-dark fs-5 fw-normal">${q.content}</span></h4>

            <div class="answer-option mb-3 d-flex align-items-center" onclick="selectAnswer(${index}, ${q.id}, 'A')">
                <input class="form-check-input ms-3 me-2" type="radio" name="q_${q.id}" id="q${q.id}_A" value="A">
                <label class="form-check-label" for="q${q.id}_A"><strong>A.</strong> ${q.optionA}</label>
            </div>

            <div class="answer-option mb-3 d-flex align-items-center" onclick="selectAnswer(${index}, ${q.id}, 'B')">
                <input class="form-check-input ms-3 me-2" type="radio" name="q_${q.id}" id="q${q.id}_B" value="B">
                <label class="form-check-label" for="q${q.id}_B"><strong>B.</strong> ${q.optionB}</label>
            </div>

            <div class="answer-option mb-3 d-flex align-items-center" onclick="selectAnswer(${index}, ${q.id}, 'C')">
                <input class="form-check-input ms-3 me-2" type="radio" name="q_${q.id}" id="q${q.id}_C" value="C">
                <label class="form-check-label" for="q${q.id}_C"><strong>C.</strong> ${q.optionC}</label>
            </div>

            <div class="answer-option mb-3 d-flex align-items-center" onclick="selectAnswer(${index}, ${q.id}, 'D')">
                <input class="form-check-input ms-3 me-2" type="radio" name="q_${q.id}" id="q${q.id}_D" value="D">
                <label class="form-check-label" for="q${q.id}_D"><strong>D.</strong> ${q.optionD}</label>
            </div>
        </div>`;
    });
    container.innerHTML = html;
}

function renderSidebar() {
    const navList = document.getElementById('questionNavList');
    let html = '';
    for (let i = 0; i < questionsData.length; i++) {
        html += `<div class="q-number ${i === 0 ? 'current' : ''}" id="nav-btn-${i}" onclick="goToQuestion(${i})">${i + 1}</div>`;
    }
    navList.innerHTML = html;
}

window.selectAnswer = function(qIndex, qId, option) {
    if(isSubmitting) return; // Khóa nếu đang nộp bài
    document.getElementById(`q${qId}_${option}`).checked = true;
    userAnswers[qId] = option;
    document.getElementById(`nav-btn-${qIndex}`).classList.add('answered');
};

window.changeQuestion = function(step) {
    let newIndex = currentQuestionIndex + step;
    if (newIndex >= 0 && newIndex < questionsData.length) goToQuestion(newIndex);
};

window.goToQuestion = function(index) {
    document.getElementById(`question-card-${currentQuestionIndex}`).classList.remove('active');
    document.getElementById(`nav-btn-${currentQuestionIndex}`).classList.remove('current');

    currentQuestionIndex = index;
    document.getElementById(`question-card-${currentQuestionIndex}`).classList.add('active');
    document.getElementById(`nav-btn-${currentQuestionIndex}`).classList.add('current');

    updateNavigationButtons();
};

function updateNavigationButtons() {
    document.getElementById('prevBtn').disabled = (currentQuestionIndex === 0);
    document.getElementById('nextBtn').disabled = (currentQuestionIndex === questionsData.length - 1);
}

function startTimer() {
    const timerDisplay = document.getElementById('examTimer');

    timerInterval = setInterval(() => {
        if (timeRemaining <= 0) {
            clearInterval(timerInterval);
            timerDisplay.innerText = "HẾT GIỜ";
            alert("Đã hết thời gian làm bài. Hệ thống tự động thu bài!");
            submitExam(true); // ÉP BUỘC NỘP BÀI KHÔNG CẦN HỎI
            return;
        }
        timeRemaining--;
        let m = Math.floor(timeRemaining / 60);
        let s = timeRemaining % 60;

        if (timeRemaining < 60) timerDisplay.classList.add('text-blink'); // Báo động 1 phút cuối
        timerDisplay.innerText = `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }, 1000);
}

// FIX LỖI "TỤC": THÊM THAM SỐ isForced
window.submitExam = function(isForced = false) {
    if (isSubmitting) return;

    // Chỉ hỏi ý kiến nếu sinh viên TỰ BẤM nộp bài và vẫn còn thời gian
    if (!isForced && timeRemaining > 0) {
        if (!confirm("Bạn có chắc chắn muốn nộp bài sớm không?")) {
            return; // Nếu bấm Cancel thì dừng lại
        }
    }

    // Bắt đầu quá trình nộp bài (Khóa toàn bộ màn hình)
    isSubmitting = true;
    clearInterval(timerInterval);
    document.body.style.pointerEvents = 'none'; // Chặn click toàn trang
    document.body.style.opacity = '0.6'; // Làm mờ trang

    // Tắt Fullscreen nhẹ nhàng
    if (document.fullscreenElement) {
        document.exitFullscreen().catch(err => console.log(err));
    }

    fetch(`/api/exams/submit/${attemptId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(userAnswers)
    })
    .then(res => {
        if (res.ok) {
            // Chuyển sang trang xem điểm
            window.location.href = `/student/result/${attemptId}`;
        } else {
            alert("Lỗi khi nộp bài. Vui lòng báo Giám thị!");
            window.location.reload();
        }
    });
};

function enableAntiCheat() {
    let elem = document.documentElement;
    if (elem.requestFullscreen) elem.requestFullscreen().catch(err => {});

    document.addEventListener('contextmenu', e => e.preventDefault());
    document.addEventListener('copy', e => { e.preventDefault(); alert("Không được sao chép!"); });

    document.addEventListener("visibilitychange", () => {
        if (document.hidden && !isSubmitting) {
            securityViolations++;
            if (securityViolations >= 3) {
                alert("🚫 BẠN ĐÃ VI PHẠM QUY CHẾ THI 3 LẦN. HỆ THỐNG ĐANG THU BÀI!");
                submitExam(true); // ÉP BUỘC NỘP BÀI (isForced = true)
            } else {
                alert(`⚠️ CẢNH BÁO VI PHẠM (${securityViolations}/3): Bạn vừa rời khỏi màn hình bài thi!\nNếu vi phạm 3 lần, hệ thống sẽ tự động thu bài.`);
            }
        }
    });
}