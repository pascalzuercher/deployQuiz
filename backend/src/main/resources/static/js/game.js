// Main Game Logic Module
const GameManager = (() => {
    // DOM Elements
    const playerNameInput = document.getElementById('player-name');
    const joinSelectedGameBtn = document.getElementById('join-selected-game-btn');
    const createGameBtn = document.getElementById('create-game-btn');
    const newGameNameInput = document.getElementById('new-game-name');
    const refreshGamesBtn = document.getElementById('refresh-games-btn');
    const startGameBtn = document.getElementById('start-game-btn');
    const playAgainBtn = document.getElementById('play-again-btn');
    const leaveGameBtn = document.getElementById('leave-game-btn');

    // Game state
    let playerName = '';
    let currentGameId = '';
    let currentGameName = '';
    let isHost = false;
    let hasAnswered = false;
    let timerInterval = null;

    // Initialize the game
    function init() {
        // Set up event listeners
        joinSelectedGameBtn.addEventListener('click', joinSelectedGame);
        createGameBtn.addEventListener('click', createNewGame);
        refreshGamesBtn.addEventListener('click', refreshGamesList);
        startGameBtn.addEventListener('click', startGame);
        playAgainBtn.addEventListener('click', returnToLobby);
        leaveGameBtn.addEventListener('click', leaveGame);

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

        // Connect to WebSocket immediately, but disable auto-join
        WebSocketManager.connect(false);

        // Request available games when loaded
        setTimeout(refreshGamesList, 1000);
    }

    // Refresh the list of available games
    function refreshGamesList() {
        if (WebSocketManager.isConnected()) {
            console.log('Requesting available games list');
            WebSocketManager.sendMessage({
                action: 'getAvailableGames'
            });
        } else {
            console.log('WebSocket not connected, attempting to connect...');
            WebSocketManager.connect(false);
            setTimeout(refreshGamesList, 1000);
        }
    }

    // Join the selected game
    function joinSelectedGame() {
        playerName = playerNameInput.value.trim();
        const selectedGameId = UIManager.getSelectedGameId();

        if (!playerName) {
            alert('Please enter your name');
            return;
        }

        if (!selectedGameId) {
            alert('Please select a game to join');
            return;
        }

        if (WebSocketManager.isConnected()) {
            console.log('Joining game:', selectedGameId, 'as', playerName);

            // Set currentGameId so we can track which game we're joining
            currentGameId = selectedGameId;

            // Send join specific game message
            WebSocketManager.sendMessage({
                action: 'joinSpecificGame',
                gameId: selectedGameId,
                name: playerName
            });
        } else {
            alert('Not connected to the game server. Please try again.');
            WebSocketManager.connect(false);
        }
    }

    // Create a new game
    function createNewGame() {
        playerName = playerNameInput.value.trim();
        const gameName = newGameNameInput.value.trim() || `${playerName}'s Game`;

        if (!playerName) {
            alert('Please enter your name');
            return;
        }

        if (WebSocketManager.isConnected()) {
            console.log('Creating new game named:', gameName);
            // Send create game message
            WebSocketManager.sendMessage({
                action: 'createNewGame',
                name: playerName,
                gameName: gameName
            });
        } else {
            alert('Not connected to the game server. Please try again.');
            WebSocketManager.connect(false);
        }
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
            answer: answer
        });
    }

    // Return to lobby after game over
    function returnToLobby() {
        refreshGamesList();
        UIManager.showScreen('login');

        // Reset game state
        hasAnswered = false;
        isHost = false;

        // Clear any active timers
        if (timerInterval) {
            clearInterval(timerInterval);
            timerInterval = null;
        }
    }

    // Leave the current game
    function leaveGame() {
        // Just disconnect and reconnect to leave the game
        WebSocketManager.disconnect();
        setTimeout(() => {
            WebSocketManager.connect(false);
            refreshGamesList();
            UIManager.showScreen('login');
        }, 500);
    }

    // Setup WebSocket message handlers
    function setupMessageHandlers() {
        // Clear any existing handlers
        WebSocketManager.clearHandlers();

        // Available games list
        WebSocketManager.on('availableGames', (message) => {
            console.log('Received available games:', message.games);
            UIManager.updateAvailableGames(message.games);
        });

        // Game created confirmation
        WebSocketManager.on('gameCreated', (message) => {
            console.log('Game created successfully:', message);
            currentGameId = message.gameId;
            currentGameName = message.gameName;
            UIManager.setWaitingRoomGameInfo(currentGameId, currentGameName);
            UIManager.showScreen('waiting');
        });

        // Host status update
        WebSocketManager.on('hostStatus', (message) => {
            console.log('Received host status:', message);
            isHost = message.isHost;
            document.getElementById('host-controls').classList.toggle('hidden', !isHost);
            document.querySelector('.waiting-message').classList.toggle('hidden', isHost);
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
            UIManager.showAnswerFeedback(
                message.correct,
                message.correct ? 'Correct!' : 'Incorrect!',
                message.fastest
            );
        });

        // Score update
        WebSocketManager.on('scoreUpdate', (message) => {
            UIManager.updateScores(message.scores);
        });

        // Reveal correct answer
        WebSocketManager.on('revealAnswer', (message) => {
            UIManager.highlightCorrectAnswer(message.correctAnswer);

            // Show fastest player if available
            if (message.fastestPlayer) {
                UIManager.showFastestPlayer(message.fastestPlayer);
            }
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
            console.log('Received game status:', message);

            if (message.status === 'inProgress') {
                alert('A game is already in progress. Please wait for it to finish.');
                UIManager.showScreen('login');
                refreshGamesList();
            } else if (message.status === 'waiting') {
                // Only transition to waiting room if we have a valid gameId and name
                if (message.gameId && message.gameName) {
                    currentGameId = message.gameId;
                    currentGameName = message.gameName;
                    UIManager.setWaitingRoomGameInfo(currentGameId, currentGameName);
                    UIManager.showScreen('waiting');
                }
            }
        });

        // Error message
        WebSocketManager.on('error', (message) => {
            alert(`Game error: ${message.message}`);
            UIManager.showScreen('login');
        });
    }

    // Public API
    return {
        init
    };
})();

// Initialize game when DOM is loaded
document.addEventListener('DOMContentLoaded', GameManager.init);