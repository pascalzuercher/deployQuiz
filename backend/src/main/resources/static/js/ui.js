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

    // Validate that all needed elements exist
    function validateElements() {
        // Check screens
        for (const [name, element] of Object.entries(screens)) {
            if (!element) {
                console.error(`Missing screen element: ${name}-screen`);
            }
        }

        // Check UI elements
        for (const [name, element] of Object.entries(elements)) {
            if (!element) {
                console.error(`Missing UI element: ${name}`);
            }
        }
    }

    // Initialize UI
    function init() {
        console.log("Initializing UI Manager");
        validateElements();
    }

    // Show a specific screen, hide others
    function showScreen(screenName) {
        console.log(`Showing screen: ${screenName}`);

        if (!screens[screenName]) {
            console.error(`Unknown screen: ${screenName}`);
            return;
        }

        Object.keys(screens).forEach(name => {
            if (screens[name]) {
                screens[name].classList.toggle('hidden', name !== screenName);
            }
        });
    }

    // Update player list in waiting room
    function updatePlayerList(players) {
        console.log('Updating player list:', players);

        if (!elements.playerList) {
            console.error("Player list element not found");
            return;
        }

        elements.playerList.innerHTML = '';

        if (!Array.isArray(players)) {
            console.error("Expected players to be an array, got:", players);
            return;
        }

        players.forEach(player => {
            const li = document.createElement('li');
            li.innerHTML = `<i class="fas fa-user"></i> ${player}`;
            elements.playerList.appendChild(li);
        });
    }

    // Update scores display during the game
    function updateScores(scores) {
        console.log('Updating scores:', scores);

        if (!elements.playerScores) {
            console.error("Player scores element not found");
            return;
        }

        elements.playerScores.innerHTML = '';

        if (typeof scores !== 'object' || scores === null) {
            console.error("Expected scores to be an object, got:", scores);
            return;
        }

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
        console.log('Displaying question:', questionData);

        if (!elements.questionText || !elements.answersContainer || !elements.questionCounter) {
            console.error("Question elements not found");
            return;
        }

        // Update question counter
        if (questionData.questionNumber !== undefined && questionData.totalQuestions !== undefined) {
            elements.questionCounter.textContent = `Question ${questionData.questionNumber} of ${questionData.totalQuestions}`;
        }

        // Set question text
        if (questionData.question) {
            elements.questionText.textContent = questionData.question;
        } else {
            console.error("Question text is missing from data");
        }

        // Clear previous answers
        elements.answersContainer.innerHTML = '';

        // Create answer buttons
        if (Array.isArray(questionData.answers)) {
            questionData.answers.forEach(answer => {
                const button = document.createElement('button');
                button.className = 'answer-btn';
                button.textContent = answer;

                // Add data attribute for easier identification
                button.dataset.answer = answer;

                elements.answersContainer.appendChild(button);
            });
        } else {
            console.error("Question answers are missing or not an array");
        }

        // Reset and hide feedback
        if (elements.answerFeedback) {
            elements.answerFeedback.classList.add('hidden');
        }
    }

    // Start timer animation
    function startTimer(seconds) {
        console.log(`Starting timer for ${seconds} seconds`);

        if (!elements.timerBar || !elements.timerText) {
            console.error("Timer elements not found");
            return null;
        }

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
        console.log(`Showing answer feedback: ${isCorrect ? 'Correct' : 'Incorrect'}`);

        if (!elements.answerFeedback || !elements.feedbackText) {
            console.error("Feedback elements not found");
            return;
        }

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
        console.log('Selecting answer:', answerElement.textContent);

        if (!answerElement) {
            console.error("Answer element is null");
            return;
        }

        // Clear previous selections
        document.querySelectorAll('.answer-btn').forEach(btn => {
            btn.classList.remove('selected');
        });

        // Mark as selected
        answerElement.classList.add('selected');
    }

    // Highlight the correct answer
    function highlightCorrectAnswer(correctAnswer) {
        console.log('Highlighting correct answer:', correctAnswer);

        if (!correctAnswer) {
            console.error("Correct answer is missing");
            return;
        }

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
        console.log('Showing game results:', results);

        if (!elements.winnerAnnouncement || !elements.finalScores) {
            console.error("Results elements not found");
            return;
        }

        showScreen('results');

        if (results.winner) {
            elements.winnerAnnouncement.innerHTML =
                `<i class="fas fa-trophy"></i> ${results.winner} wins with ${results.winnerScore} points! <i class="fas fa-trophy"></i>`;
        } else {
            elements.winnerAnnouncement.textContent = 'Game Over!';
        }

        // Display final scores
        elements.finalScores.innerHTML = '';

        if (typeof results.scores !== 'object' || results.scores === null) {
            console.error("Expected scores to be an object, got:", results.scores);
            return;
        }

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

            elements.finalScores.appendChild(scoreElement);
        });
    }

    // Initialize the UI on load
    init();

    // Public API
    return {
        showScreen,
        updatePlayerList,
        updateScores,
        displayQuestion,
        startTimer,
        showAnswerFeedback,
        selectAnswer,
        highlightCorrectAnswer,
        showGameResults
    };
})();

// Make UIManager globally available
window.UIManager = UIManager;