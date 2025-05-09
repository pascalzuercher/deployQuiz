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
        fastestPlayerText: document.getElementById('fastest-player-text'),
        hostControls: document.getElementById('host-controls'),
        winnerAnnouncement: document.getElementById('winner-announcement'),
        finalScores: document.getElementById('final-scores'),
        availableGamesList: document.getElementById('available-games-list'),
        waitingRoomGameName: document.getElementById('waiting-room-game-name'),
        waitingRoomGameId: document.getElementById('waiting-room-game-id'),
        // Tab elements
        tabButtons: document.querySelectorAll('.tab-button'),
        tabContents: document.querySelectorAll('.tab-content'),
        joinSelectedGameBtn: document.getElementById('join-selected-game-btn')
    };

    let activeTimer = null; // aktuell laufender Timer
    let selectedGameId = null; // Selected game ID for joining

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
            if (!element && !Array.isArray(element)) {
                console.error(`Missing UI element: ${name}`);
            }
        }
    }

    // Initialize UI
    function init() {
        console.log("Initializing UI Manager");
        validateElements();

        // Set up tabs
        elements.tabButtons.forEach(button => {
            button.addEventListener('click', () => {
                // Remove active class from all buttons and contents
                elements.tabButtons.forEach(btn => btn.classList.remove('active'));
                elements.tabContents.forEach(content => content.classList.remove('active'));

                // Add active class to clicked button and corresponding content
                button.classList.add('active');
                const tabId = button.dataset.tab;
                document.getElementById(`${tabId}-tab`).classList.add('active');
            });
        });

        // Set up game selection
        document.getElementById('available-games-list').addEventListener('click', (event) => {
            const gameItem = event.target.closest('.game-item');
            if (gameItem) {
                // Deselect all games
                document.querySelectorAll('.game-item').forEach(item => {
                    item.classList.remove('selected');
                });

                // Select this game
                gameItem.classList.add('selected');
                selectedGameId = gameItem.dataset.gameId;
                console.log('Selected game:', selectedGameId);

                // Enable the join button
                if (elements.joinSelectedGameBtn) {
                    elements.joinSelectedGameBtn.disabled = false;
                }
            }
        });

        // Ensure we're starting at the login screen
        showScreen('login');
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

    // Update available games list
    function updateAvailableGames(games) {
        console.log('Updating available games:', games);

        if (!elements.availableGamesList) {
            console.error("Available games list element not found");
            return;
        }

        elements.availableGamesList.innerHTML = '';

        if (!Array.isArray(games) || games.length === 0) {
            const li = document.createElement('li');
            li.className = 'no-games';
            li.innerHTML = 'No games available. Create a new one!';
            elements.availableGamesList.appendChild(li);

            // Reset selection
            selectedGameId = null;
            if (elements.joinSelectedGameBtn) {
                elements.joinSelectedGameBtn.disabled = true;
            }
            return;
        }

        games.forEach(game => {
            const li = document.createElement('li');
            li.className = 'game-item';
            li.dataset.gameId = game.id;

            const statusClass = game.inProgress ? 'game-in-progress' : 'game-waiting';
            const statusIcon = game.inProgress ?
                '<i class="fas fa-play-circle"></i>' :
                '<i class="fas fa-hourglass-half"></i>';

            li.innerHTML = `
                <div class="game-item-name">${game.name}</div>
                <div class="game-item-details">
                    <span class="game-status ${statusClass}">
                        ${statusIcon} ${game.inProgress ? 'In Progress' : 'Waiting'}
                    </span>
                    <span class="game-players">
                        <i class="fas fa-users"></i> ${game.playerCount} player${game.playerCount !== 1 ? 's' : ''}
                    </span>
                </div>
            `;

            elements.availableGamesList.appendChild(li);
        });

        // Reset selection
        selectedGameId = null;
        if (elements.joinSelectedGameBtn) {
            elements.joinSelectedGameBtn.disabled = true;
        }
    }

    // Set waiting room game info
    function setWaitingRoomGameInfo(gameId, gameName) {
        console.log('Setting waiting room game info:', gameId, gameName);

        if (elements.waitingRoomGameName) {
            elements.waitingRoomGameName.textContent = gameName || 'Unknown Game';
        }

        if (elements.waitingRoomGameId) {
            elements.waitingRoomGameId.textContent = `Game ID: ${gameId ? gameId.substring(0, 8) : 'Unknown'}`;
        }
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

        if (elements.fastestPlayerText) {
            elements.fastestPlayerText.classList.add('hidden');
        }
    }

    function startTimer(seconds) {
        console.log(`Starting timer for ${seconds} seconds`);

        if (!elements.timerBar || !elements.timerText) {
            console.error("Timer elements not found");
            return null;
        }

        // Reset
        elements.timerBar.classList.remove('paused');
        elements.timerBar.style.transition = 'none';
        elements.timerBar.style.width = '0%';
        elements.timerBar.style.backgroundColor = 'var(--secondary-color)';

        // Reflow
        void elements.timerBar.offsetWidth;

        // Animate
        elements.timerBar.style.transition = `width ${seconds}s linear, background-color 0.5s ease`;
        elements.timerBar.style.width = '100%';

        // Set text
        elements.timerText.textContent = seconds;
        let timeLeft = seconds;

        // Start countdown
        activeTimer = setInterval(() => {
            timeLeft--;
            if (timeLeft <= 0) {
                clearInterval(activeTimer);
                activeTimer = null;
                timeLeft = 0;
                elements.timerBar.style.backgroundColor = 'var(--danger-color)';
            } else if (timeLeft <= 5) {
                elements.timerBar.style.backgroundColor = 'var(--danger-color)';
            } else if (timeLeft <= 10) {
                elements.timerBar.style.backgroundColor = 'var(--warning-color)';
            }

            elements.timerText.textContent = timeLeft;
        }, 1000);

        return activeTimer;
    }

    function stopTimer() {
        console.log('Stopping timer');

        if (activeTimer) {
            clearInterval(activeTimer);
            activeTimer = null;
        }

        if (elements.timerBar) {
            const computedWidth = getComputedStyle(elements.timerBar).width;

            // Stop animation and freeze current state
            elements.timerBar.style.transition = 'none';
            elements.timerBar.style.width = computedWidth;
            elements.timerBar.classList.add('paused');
            elements.timerBar.style.backgroundColor = 'gray';
        }

        if (elements.timerText) {
            elements.timerText.textContent = '⏸'; // Pause-Symbol
        }
    }

    // Show answer feedback
    function showAnswerFeedback(isCorrect, message, isFastest) {
        console.log(`Showing answer feedback: ${isCorrect ? 'Correct' : 'Incorrect'}, Fastest: ${isFastest}`);

        if (!elements.answerFeedback || !elements.feedbackText) {
            console.error("Feedback elements not found");
            return;
        }

        elements.answerFeedback.classList.remove('hidden', 'correct', 'incorrect');

        if (isCorrect) {
            let feedbackMessage = message || 'Correct!';
            if (isFastest) {
                feedbackMessage += ' You were the fastest!';
            }
            elements.feedbackText.textContent = feedbackMessage;
            elements.answerFeedback.classList.add('correct');
        } else {
            elements.feedbackText.textContent = message || 'Incorrect!';
            elements.answerFeedback.classList.add('incorrect');
        }
    }

    // Show fastest player
    function showFastestPlayer(playerName) {
        if (!elements.fastestPlayerText) {
            console.error("Fastest player text element not found");
            return;
        }

        if (playerName) {
            elements.fastestPlayerText.textContent = `Fastest correct answer: ${playerName}`;
            elements.fastestPlayerText.classList.remove('hidden');
        } else {
            elements.fastestPlayerText.classList.add('hidden');
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

    // Get the selected game ID
    function getSelectedGameId() {
        return selectedGameId;
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
        stopTimer,
        highlightCorrectAnswer,
        showGameResults,
        updateAvailableGames,
        getSelectedGameId,
        setWaitingRoomGameInfo,
        showFastestPlayer
    };
})();

// Make UIManager globally available
window.UIManager = UIManager;