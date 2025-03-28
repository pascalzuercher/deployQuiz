// script.js
let ws;
let playerName = "";
let currentScore = 0;
let countdown;
let nextQuestionCountdown;
let timeLeft = 15;
let answered = false;
let currentIndex = 0;
let totalQuestions = 1; // Wird vom Server gesetzt

function joinGame() {
    playerName = document.getElementById("username").value.trim();
    if (!playerName) {
        alert("Bitte gib deinen Namen ein!");
        return;
    }

    ws = new WebSocket("ws://localhost:8080/quiz");

    ws.onopen = () => {
        console.log("✅ Verbunden mit WebSocket-Server");
        ws.send(JSON.stringify({ action: "join", name: playerName }));
        document.getElementById("lobby").style.display = "none";
        document.getElementById("quiz").style.display = "block";
    };

    ws.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        console.log("📨", msg);

        if (msg.action === "question") {
            showQuestion(msg);
        } else if (msg.action === "score") {
            currentScore = msg.score;
            document.getElementById("score").innerText = "SCORE: " + currentScore;
        } else if (msg.action === "gameOver") {
            showScore();
        } else if (msg.action === "allAnswered") {
            // Spielerfeedback: alle haben geantwortet
            enterWaitingPhase(msg.delay);
        }
    };

}


function enterWaitingPhase(delayInSeconds) {
    // Alles außer Status ausblenden
    document.getElementById("question-card").style.display = "none";
    document.getElementById("answers").style.display = "none";
    document.querySelector(".progress-wrapper").style.display = "none";
    document.getElementById("score").style.display = "none";

    const status = document.getElementById("status-text");
    status.style.display = "block";

    let secondsLeft = delayInSeconds;
    status.innerHTML = `Alle bereit!<br>Nächste Frage in <span class="countdown-number">${secondsLeft}</span> Sekunden…`;

    nextQuestionCountdown = setInterval(() => {
        secondsLeft--;
        status.innerHTML = `Alle bereit!<br>Nächste Frage in <span class="countdown-number">${secondsLeft}</span> Sekunden…`;

        if (secondsLeft <= 0) {
            clearInterval(nextQuestionCountdown);
            // warten auf nächste Frage vom Server
        }
    }, 1000);
}

function showQuestion(msg) {

    // Vorherigen Warte-Timer abbrechen
    clearInterval(nextQuestionCountdown);

    // Alles wieder sichtbar machen
    document.getElementById("question-card").style.display = "block";
    document.getElementById("answers").style.display = "grid";
    document.querySelector(".progress-wrapper").style.display = "block";
    document.getElementById("score").style.display = "block";
    document.getElementById("status-text").style.display = "none";

    answered = false;
    timeLeft = 15;

    const progressBar = document.getElementById("progress-bar");
    progressBar.style.transition = "none";
    progressBar.style.width = "0%"; // sofort zurücksetzen

    // nach einem Frame Transition wieder aktivieren
    requestAnimationFrame(() => {
        progressBar.style.transition = "width 1s linear";
    });


    updateTimerDisplay();
    clearInterval(countdown);

    // Fade-In Animation für Fragekarte neu starten
    const card = document.getElementById("question-card");
    card.classList.remove("question-card");
    void card.offsetWidth;
    card.classList.add("question-card");

    document.getElementById("question").innerText = msg.question;
    document.getElementById("question-number").innerText = `Frage ${currentIndex} von ${totalQuestions}`;
    document.getElementById("answers").innerHTML = "";
    document.getElementById("score").innerText = "SCORE: " + currentScore;

    msg.answers.forEach((answer, index) => {
        const btn = document.createElement("button");
        btn.classList.add("answer-btn");
        const label = String.fromCharCode(65 + index);
        btn.innerText = `${label}. ${answer}`;
        btn.onclick = () => sendAnswer(answer, btn, msg.correctAnswer);
        document.getElementById("answers").appendChild(btn);
    });

    countdown = setInterval(() => {
        timeLeft--;
        updateTimerDisplay();
        if (timeLeft <= 0) {
            clearInterval(countdown);
            disableAnswers(msg.correctAnswer);
        }
    }, 1000);
}

function updateTimerDisplay() {
    const timerText = document.getElementById("timer-text");
    const progressBar = document.getElementById("progress-bar");

    timerText.innerText = timeLeft;
    const progress = (15 - timeLeft) / 15 * 100;
    document.getElementById("progress-bar").style.width = `${progress}%`;

    if (timeLeft > 7) {
        progressBar.style.backgroundColor = "#4caf50";
    } else if (timeLeft > 3) {
        progressBar.style.backgroundColor = "#ffc107";
    } else {
        progressBar.style.backgroundColor = "#f44336";
    }

    if (timeLeft <= 3) {
        timerText.classList.remove("pulse");
        void timerText.offsetWidth;
        timerText.classList.add("pulse");
    } else {
        timerText.classList.remove("pulse");
    }
}

function sendAnswer(answer, button, correctAnswer) {
    if (answered) return;
    answered = true;
    clearInterval(countdown);

    ws.send(JSON.stringify({ action: "answer", name: playerName, answer: answer }));

    const buttons = document.querySelectorAll(".answer-btn");
    buttons.forEach(btn => {
        const answerText = btn.innerText.split(". ").slice(1).join(". ");
        const isCorrect = answerText === correctAnswer;
        const isSelected = btn === button;

        if (isCorrect && isSelected) {
            btn.style.backgroundColor = "rgba(0, 255, 0, 0.5)";
        } else if (!isCorrect && isSelected) {
            btn.style.backgroundColor = "rgba(255, 0, 0, 0.5)";
        } else if (isCorrect) {
            btn.style.backgroundColor = "rgba(0, 255, 0, 0.2)";
        }

        btn.disabled = true;
    });

    // Statusanzeige aktivieren
    document.getElementById("status-text").innerText = "Warte auf andere Spieler…";
    document.getElementById("status-text").style.display = "block";
}

function showScore() {
    document.getElementById("quiz").style.display = "none";
    document.getElementById("scoreboard").style.display = "block";
    document.getElementById("final-score").innerText = `Dein Punktestand: ${currentScore}`;
}