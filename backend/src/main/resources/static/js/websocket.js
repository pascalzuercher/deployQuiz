// WebSocket Communication Module
const WebSocketManager = (() => {
    // Private variables
    let socket;
    let isConnected = false;
    let reconnectAttempts = 0;
    let maxReconnectAttempts = 5;
    let reconnectDelay = 2000; // Start with 2 seconds
    let messageHandlers = {};
    let autoJoinEnabled = false; // Flag to control auto-joining the default game

    // DOM Elements
    const statusIcon = document.getElementById('status-icon');
    const statusText = document.getElementById('status-text');

    // Update connection status UI
    function updateConnectionStatus(connected) {
        isConnected = connected;

        if (connected) {
            statusIcon.className = 'status-connected';
            statusText.textContent = 'Connected';
        } else {
            statusIcon.className = 'status-disconnected';
            statusText.textContent = 'Disconnected';
        }
    }

    // Connect to WebSocket server
    function connect(enableAutoJoin = false) {
        // Store auto-join preference
        autoJoinEnabled = enableAutoJoin;

        // Use the appropriate WebSocket URL based on your deployment
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const host = window.location.host; // includes hostname and port

        try {
            socket = new WebSocket(`${protocol}//${host}/quiz`);

            socket.onopen = handleOpen;
            socket.onmessage = handleMessage;
            socket.onclose = handleClose;
            socket.onerror = handleError;
        } catch (error) {
            console.error('Error creating WebSocket:', error);
            updateConnectionStatus(false);
        }
    }

    // WebSocket event handlers
    function handleOpen() {
        console.log('WebSocket connection established');
        updateConnectionStatus(true);
        reconnectAttempts = 0;
        reconnectDelay = 2000;

        // Trigger getAvailableGames upon connection
        if (!autoJoinEnabled) {
            setTimeout(() => {
                if (isConnected) {
                    sendMessage({
                        action: 'getAvailableGames'
                    });
                    console.log('Requested available games automatically');
                }
            }, 500);
        }
    }

    function handleMessage(event) {
        try {
            const message = JSON.parse(event.data);
            console.log('Received message:', message);

            // Intercept gameStatus message that contains "waiting" without user
            // having explicitly joined a game - only if auto-join is disabled
            if (!autoJoinEnabled &&
                message.action === 'gameStatus' &&
                message.status === 'waiting' &&
                !message.gameId) {  // Only ignore if no game ID (means it's not a response to join)
                // Don't process this message - let the user select a game first
                console.log('Ignoring auto-join attempt. User must select a game first.');
                return;
            }

            // Call registered handlers for this message type
            if (message.action && messageHandlers[message.action]) {
                messageHandlers[message.action].forEach(handler => handler(message));
            }

            // Call any global message handlers
            if (messageHandlers['*']) {
                messageHandlers['*'].forEach(handler => handler(message));
            }
        } catch (error) {
            console.error('Error parsing message:', error);
        }
    }

    function handleClose(event) {
        console.log('WebSocket connection closed:', event.code, event.reason);
        updateConnectionStatus(false);

        // Attempt to reconnect if not a normal closure
        if (event.code !== 1000) {
            attemptReconnect();
        }
    }

    function handleError(error) {
        console.error('WebSocket error:', error);
        updateConnectionStatus(false);
    }

    // Attempt to reconnect with exponential backoff
    function attemptReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            console.log('Max reconnect attempts reached');
            return;
        }

        reconnectAttempts++;
        const delay = reconnectDelay * Math.pow(1.5, reconnectAttempts - 1);
        console.log(`Attempting to reconnect in ${delay}ms (attempt ${reconnectAttempts}/${maxReconnectAttempts})`);

        setTimeout(() => {
            console.log(`Reconnecting... (attempt ${reconnectAttempts}/${maxReconnectAttempts})`);
            connect(autoJoinEnabled);
        }, delay);
    }

    // Send a message to the server
    function sendMessage(message) {
        if (!socket || socket.readyState !== WebSocket.OPEN) {
            console.error('WebSocket is not connected');
            return false;
        }

        try {
            console.log('Sending message:', message);
            socket.send(JSON.stringify(message));
            return true;
        } catch (error) {
            console.error('Error sending message:', error);
            return false;
        }
    }

    // Register a handler for a specific message type
    function on(messageType, handler) {
        if (!messageHandlers[messageType]) {
            messageHandlers[messageType] = [];
        }

        messageHandlers[messageType].push(handler);
    }

    // Remove a handler for a specific message type
    function off(messageType, handler) {
        if (!messageHandlers[messageType]) return;

        messageHandlers[messageType] = messageHandlers[messageType]
            .filter(h => h !== handler);
    }

    // Clear all handlers
    function clearHandlers() {
        messageHandlers = {};
    }

    // Disconnect from the WebSocket server
    function disconnect() {
        if (socket) {
            socket.close(1000, 'Closing connection normally');
        }
    }

    // Public API
    return {
        connect,
        disconnect,
        sendMessage,
        on,
        off,
        clearHandlers,
        isConnected: () => isConnected
    };
})();

// Automatically export for other modules
window.WebSocketManager = WebSocketManager;