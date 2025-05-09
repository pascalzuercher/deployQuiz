// UI Management Module
const UIManager = (() => {
    // Screen elements
    const screens = {
        login: document.getElementById('login-screen'),
        waiting: document.getElementById('waiting-room'),
        game: document.getElementById('game-screen'),
        results: document.getElementById('results-screen')
    };

    // Common UI elements
    const elements = {
        playerList: document.getElementById('player-list'),
        playerScores: document.getElementById('player-scores'),
        questionCounter: document.getElementById('question-counter'),
        questionText: document.getElementById('question-text'),
        answersContainer: document.getElementById('answers-container'),
        timerBar: document.getElementById('timer-bar'),
        timerText: document.getElementById('timer-text'),
        answerFeedback: document.getElementById('answer-feedback'),
        feedbackText: document.getElementById('feedback-text'),
        hostControls: document.getElementById('host-controls'),
        winnerAnnouncement: document.getElementById('winner-announcement'),
        finalScores: document.getElementById('final-scores')
    };

    // Show a specific screen, hide others
    function showScreen(screenName) {
        Object.keys(screens).forEach(name => {
            screens[name].classList.toggle('hidden', name !== screenName);
        });
    }

    // Update player list in waiting room
    function updatePlayerList(players) {
        elements.playerList.innerHTML = '';

        players.forEach(player => {
            const li = document.createElement('li');
            li.innerHTML = `<i class="fas fa-user"></i> ${player}`;
            elements.playerList.appendChild(li);
        });
    }

    // Update scores display during the game
    function updateScores(scores) {
        elements.playerScores.innerHTML = '';

        Object.entries(scores).forEach(([name, score]) => {
            const scoreElement = document.createElement('div');
            scoreElement.className = 'player-score';
            scoreElement.innerHTML = `
                <span><i class="fas fa-user"></i> ${name}</span>
                <span class="score">${score} points</span>
            `;
            elements.playerScores.appendChild(scoreElement);
        });
    }

    // Display a question and its answers
    function displayQuestion(questionData) {
        // Update question counter
        elements.questionCounter.textContent = `Question ${questionData.questionNumber} of ${questionData.totalQuestions}`;

        // Set question text
        elements.questionText.textContent = questionData.question;

        // Clear previous answers
        elements.answersContainer.innerHTML = '';

        // Create answer buttons
        questionData.answers.forEach(answer => {
            const button = document.createElement('button');
            button.className = 'answer-btn';
            button.textContent = answer;

            // Add data attribute for easier identification
            button.dataset.answer = answer;

            elements.answersContainer.appendChild(button);
        });

        // Reset and hide feedback
        elements.answerFeedback.classList.add('hidden');
    }

    // Start timer animation
    function startTimer(seconds) {
        // Reset timer bar
        elements.timerBar.style.width = '100%';
        elements.timerBar.style.backgroundColor = 'var(--secondary-color)';
        elements.timerText.textContent = seconds;

        // Change color as time runs out
        setTimeout(() => {
            elements.timerBar.style.width = '0%';
        }, 50); // Small delay to ensure the transition works

        // Set timer text countdown
        let timeLeft = seconds;
        const timerInterval = setInterval(() => {
            timeLeft--;

            if (timeLeft <= 0) {
                clearInterval(timerInterval);
                timeLeft = 0;
                elements.timerBar.style.backgroundColor = 'var(--danger-color)';
            } else if (timeLeft <= 5) {
                elements.timerBar.style.backgroundColor = 'var(--danger-color)';
            } else if (timeLeft <= 10) {
                elements.timerBar.style.backgroundColor = 'var(--warning-color)';
            }

            elements.timerText.textContent = timeLeft;
        }, 1000);

        return timerInterval;
    }

    // Show answer feedback
    function showAnswerFeedback(isCorrect, message) {
        elements.answerFeedback.classList.remove('hidden', 'correct', 'incorrect');

        if (isCorrect) {
            elements.feedbackText.textContent = message || 'Correct!';
            elements.answerFeedback.classList.add('correct');
        } else {
            elements.feedbackText.textContent = message || 'Incorrect!';
            elements.answerFeedback.classList.add('incorrect');
        }
    }

    // Highlight selected answer
    function selectAnswer(answerElement) {
        // Clear previous selections
        document.querySelectorAll('.answer-btn').forEach(btn => {
            btn.classList.remove('selected');
        });

        // Mark as selected
        answerElement.classList.add('selected');
    }

    // Highlight the correct answer
    function highlightCorrectAnswer(correctAnswer) {
        const answerButtons = document.querySelectorAll('.answer-btn');

        answerButtons.forEach(button => {
            button.disabled = true;

            if (button.textContent === correctAnswer) {
                button.classList.add('correct');
            } else if (button.classList.contains('selected')) {
                button.classList.add('incorrect');
            }
        });
    }

    // Show game results
    function showGameResults(results) {
        showScreen('results');

        if (results.winner) {
            elements.winnerAnnouncement.innerHTML =
                `<i class="fas fa-trophy"></i> ${results.winner} wins with ${results.winnerScore} points! <i class="fas fa-trophy"></i>`;
        } else {
            elements.winnerAnnouncement.textContent = 'Game Over!';
        }

        // Display final scores
        elements.finalScores.innerHTML = '';

        const sortedScores = Object.entries(results.scores)
            .sort((a, b) => b[1] - a[1]);

        sortedScores.forEach(([name, score], index) => {
            const scoreElement = document.createElement('div');
            scoreElement.className = 'player-score';

            if (name === results.winner) {
                scoreElement.classList.add('winner');
            }

            let medal = '';
            if (index === 0) medal = '<i class="fas fa-medal" style="color: gold;"></i> ';
            else if (index === 1) medal = '<i class="fas fa-medal" style="color: silver;"></i> ';
            else if (index === 2) medal = '<i class="fas fa-medal" style="color: #cd7f32;"></i> ';
            else medal = `<span style="display: inline-block; width: 14px; text-align: right;">${index + 1}.</span> `;

            scoreElement.innerHTML = `
                <span>${medal} ${name}</span>
                <span class="score">${score} points</span>
            `;

            elements.finalScores.appendChild(scoreElement);}