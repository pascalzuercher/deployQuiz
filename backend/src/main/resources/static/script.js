let ws;
let playerName = "";

// Spiel beitreten
function joinGame() {
    playerName = document.getElementById("username").value.trim();
    if (!playerName) {
        alert("Bitte gib deinen Namen ein!");
        return;
    }

    ws = new WebSocket("ws://localhost:8080/quiz");

    ws.onopen = () => {
        console.log("Verbunden mit WebSocket-Server");
        ws.send(JSON.stringify({ action: "join", name: playerName }));
        document.getElementById("lobby").style.display = "none";
        document.getElementById("quiz").style.display = "block";
    };

    ws.onmessage = (event) => {
        let msg = JSON.parse(event.data);

        if (msg.action === "question") {
            showQuestion(msg);
        } else if (msg.action === "score") {
            showScore(msg);
        }
    };
}

// Frage anzeigen
function showQuestion(msg) {
    document.getElementById("question").innerText = msg.question;
    let answersDiv = document.getElementById("answers");
    answersDiv.innerHTML = "";

    msg.answers.forEach(answer => {
        let btn = document.createElement("button");
        btn.innerText = answer;
        btn.onclick = () => sendAnswer(answer);
        answersDiv.appendChild(btn);
    });
}

// Antwort senden
function sendAnswer(answer) {
    ws.send(JSON.stringify({ action: "answer", name: playerName, answer: answer }));
}

// Score anzeigen
function showScore(msg) {
    document.getElementById("quiz").style.display = "none";
    document.getElementById("scoreboard").style.display = "block";
    document.getElementById("final-score").innerText = `Dein Punktestand: ${msg.score}`;
}
