// Main Game Logic Module
const GameManager = (() => {
    // DOM Elements
    const playerNameInput = document.getElementById('player-name');
    const joinBtn = document.getElementById('join-btn');
    const startGameBtn = document.getElementById('start-game-btn');
    const playAgainBtn = document.getElementById('play-again-btn');

    // Game state
    let playerName = '';
    let isHost = false;
    let hasAnswered = false;
    let timerInterval = null;

    // Initialize the game
    function init() {
        // Set up event listeners
        joinBtn.addEventListener('click', joinGame);
        startGameBtn.addEventListener('click', startGame);
        playAgainBtn.addEventListener('click', playAgain);

        // Set up answer click delegation
        document.getElementById('answers-container').addEventListener('click', (event) => {
            if (event.target.classList.contains('answer-btn') && !hasAnswered) {
                const answer = event.target.dataset.answer;
                UIManager.selectAnswer(event.target);
                UIManager.stopTimer();
                submitAnswer(answer);
            }
        });

        // Set up WebSocket message handlers
        setupMessageHandlers();

        // Show login screen initially
        UIManager.showScreen('login');
    }

    // Join the game
    function joinGame() {
        playerName = playerNameInput.value.trim();

        if (!playerName) {
            alert('Please enter your name');
            return;
        }

        // Connect to WebSocket
        WebSocketManager.connect();

        // Set a short timeout to ensure WebSocket is connected
        setTimeout(() => {
            if (WebSocketManager.isConnected()) {
                // Send join message
                WebSocketManager.sendMessage({
                    action: 'join',
                    name: playerName
                });

                UIManager.showScreen('waiting');
            } else {
                alert('Failed to connect to the game server. Please try again.');
            }
        }, 1000);
    }

    // Start the game (host only)
    function startGame() {
        WebSocketManager.sendMessage({
            action: 'startGame'
        });
    }

    // Submit answer to the server
    function submitAnswer(answer) {
        if (hasAnswered) return;

        hasAnswered = true;

        WebSocketManager.sendMessage({
            action: 'answer',
            name: playerName,
            answer: answer
        });
    }

    // Play again after game over
    function playAgain() {
        // Return to waiting room
        UIManager.showScreen('waiting');

        // Reset game state
        hasAnswered = false;

        // Clear any active timers
        if (timerInterval) {
            clearInterval(timerInterval);
            timerInterval = null;
        }
    }

    // Setup WebSocket message handlers
    function setupMessageHandlers() {
        // Clear any existing handlers
        WebSocketManager.clearHandlers();

        // Host status update
        WebSocketManager.on('hostStatus', (message) => {
            isHost = message.isHost;
            document.getElementById('host-controls').classList.toggle('hidden', !isHost);
        });

        // Player list update
        WebSocketManager.on('playerList', (message) => {
            UIManager.updatePlayerList(message.players);
        });

        // Game started
        WebSocketManager.on('gameStarted', () => {
            UIManager.showScreen('game');
        });

        // New question
        WebSocketManager.on('question', (message) => {
            // Reset for new question
            hasAnswered = false;

            // Clear existing timer
            if (timerInterval) {
                clearInterval(timerInterval);
            }

            // Display question
            UIManager.displayQuestion(message);

            // Start timer
            timerInterval = UIManager.startTimer(message.timeLimit || 30);
        });

        // Answer result
        WebSocketManager.on('answerResult', (message) => {
            UIManager.showAnswerFeedback(message.correct, message.correct ? 'Correct!' : 'Incorrect!');
        });

        // Score update
        WebSocketManager.on('scoreUpdate', (message) => {
            UIManager.updateScores(message.scores);
        });

        // Reveal correct answer
        WebSocketManager.on('revealAnswer', (message) => {
            UIManager.highlightCorrectAnswer(message.correctAnswer);
        });

        // Game over
        WebSocketManager.on('gameOver', (message) => {
            // Clear timer
            if (timerInterval) {
                clearInterval(timerInterval);
                timerInterval = null;
            }

            UIManager.showGameResults(message);
        });

        // Game status
        WebSocketManager.on('gameStatus', (message) => {
            if (message.status === 'inProgress') {
                alert('A game is already in progress. Please wait for it to finish.');
            }
        });

        // Error message
        WebSocketManager.on('error', (message) => {
            alert(`Game error: ${message.message}`);
        });
    }

    // Public API
    return {
        init
    };
})();

// Initialize game when DOM is loaded
document.addEventListener('DOMContentLoaded', GameManager.init);