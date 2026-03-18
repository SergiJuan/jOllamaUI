/**
 * jOllamaUI - Main Application JavaScript
 * Handles all frontend functionality for the Ollama web interface
 */

(function () {
    'use strict';

    // ==========================================================================
    // Configuration & State
    // ==========================================================================

    const API_BASE = '';
    const VIEWS = ['chat', 'models', 'settings'];

    // Application state
    const state = {
        currentView: 'chat',
        chatHistory: [],
        selectedModel: '',
        isConnected: false
    };

    // DOM element cache
    const elements = {};

    // ==========================================================================
    // Utility Functions
    // ==========================================================================

    /**
     * Escapes HTML special characters to prevent XSS
     * @param {string} text - Raw text to escape
     * @returns {string} Escaped HTML string
     */
    function escapeHtml(text) {
        return text
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;');
    }

    /**
     * Formats bytes to human-readable size
     * @param {number} bytes - Size in bytes
     * @returns {string} Formatted size (MB or GB)
     */
    function formatBytes(bytes) {
        if (!bytes) return '—';
        return bytes >= 1e9
            ? (bytes / 1e9).toFixed(1) + ' GB'
            : (bytes / 1e6).toFixed(1) + ' MB';
    }

    /**
     * Cache DOM elements for reuse
     */
    function cacheElements() {
        // Navigation
        elements.nav = {};
        VIEWS.forEach(view => {
            elements.nav[view] = document.getElementById(`nav-${view}`);
        });

        // Views
        elements.views = {};
        VIEWS.forEach(view => {
            elements.views[view] = document.getElementById(`view-${view}`);
        });

        // Connection status
        elements.connDot = document.getElementById('conn-dot');
        elements.connLabel = document.getElementById('conn-label');

        // Chat elements
        elements.chatMessages = document.getElementById('chat-messages');
        elements.chatInput = document.getElementById('chat-input');
        elements.sendButton = document.getElementById('send-button');
        elements.chatStatus = document.getElementById('chat-status');
        elements.modelSelector = document.getElementById('model-selector-chat');
        elements.clearChatBtn = document.getElementById('clear-chat');

        // Models elements
        elements.modelsTable = document.getElementById('models-table-body');
        elements.modelsStatus = document.getElementById('models-status');
        elements.pullInput = document.getElementById('pull-model-input');
        elements.pullBtn = document.getElementById('pull-model-btn');
        elements.deleteBtn = document.getElementById('delete-model-btn');
        elements.refreshModelsBtn = document.getElementById('refresh-models');

        // Settings elements
        elements.settingsHost = document.getElementById('settings-host');
        elements.saveSettingsBtn = document.getElementById('save-settings');
        elements.settingsStatus = document.getElementById('settings-status');
    }

    // ==========================================================================
    // View Management
    // ==========================================================================

    /**
     * Switches between application views
     * @param {string} viewName - Name of the view to show
     */
    function showView(viewName) {
        state.currentView = viewName;

        VIEWS.forEach(view => {
            const isActive = view === viewName;
            elements.views[view].classList.toggle('active', isActive);
            elements.nav[view].classList.toggle('active', isActive);
        });

        // Load models when switching to models view
        if (viewName === 'models') {
            loadModels();
        }
    }

    // ==========================================================================
    // Connection Status
    // ==========================================================================

    /**
     * Checks connection to the Ollama API
     */
    async function checkConnection() {
        try {
            const response = await fetch(`${API_BASE}/api/models`);
            state.isConnected = response.ok;

            elements.connDot.className = 'conn-dot ' + (response.ok ? 'connected' : 'error');
            elements.connLabel.textContent = response.ok ? 'Connected' : 'Error';
        } catch {
            state.isConnected = false;
            elements.connDot.className = 'conn-dot error';
            elements.connLabel.textContent = 'No connection';
        }
    }

    // ==========================================================================
    // Chat Functionality
    // ==========================================================================

    /**
     * Adds a message to the chat display
     * @param {string} role - Message role ('user', 'assistant', or 'error')
     * @param {string} text - Message content
     */
    function addMessage(role, text) {
        const row = document.createElement('div');
        row.className = `message-row ${role}`;

        const avatarMap = { user: 'You', assistant: 'AI', error: '!' };
        const avatar = avatarMap[role] || '?';

        row.innerHTML = `
            <div class="message-avatar">${avatar}</div>
            <div class="message-bubble">${escapeHtml(text)}</div>
        `;

        elements.chatMessages.appendChild(row);
        elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
    }

    /**
     * Shows the typing indicator
     */
    function showTyping() {
        const row = document.createElement('div');
        row.id = 'typing-row';
        row.className = 'message-row assistant';
        row.innerHTML = `
            <div class="message-avatar">AI</div>
            <div class="message-bubble" style="padding:10px 14px; width:fit-content;">
                <div class="typing-indicator"><span></span><span></span><span></span></div>
            </div>
        `;
        elements.chatMessages.appendChild(row);
        elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
    }

    /**
     * Removes the typing indicator
     */
    function removeTyping() {
        document.getElementById('typing-row')?.remove();
    }

    /**
     * Sends a chat message and handles the streaming response
     */
    async function sendMessage() {
        const text = elements.chatInput.value.trim();
        const model = elements.modelSelector.value;

        if (!text || !model) return;

        // Add user message to UI and history
        addMessage('user', text);
        state.chatHistory.push({ role: 'user', content: text });

        // Clear input
        elements.chatInput.value = '';
        elements.chatInput.style.height = 'auto';
        elements.sendButton.disabled = true;
        elements.chatStatus.innerHTML = '<span class="spinner"></span>Waiting for response…';

        showTyping();

        try {
            const response = await fetch(`${API_BASE}/api/chat`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ model, history: state.chatHistory })
            });

            if (!response.ok) {
                removeTyping();
                const error = await response.json().catch(() => ({}));
                addMessage('error', error.error || response.statusText);
                return;
            }

            removeTyping();

            // Create assistant message row with thinking block
            const row = document.createElement('div');
            row.className = 'message-row assistant';
            row.innerHTML = `
                <div class="message-avatar">AI</div>
                <div style="display:flex;flex-direction:column;gap:4px;max-width:min(520px,70vw)">
                    <div class="thinking-block" style="display:none">
                        <div class="thinking-header">
                            <span class="arrow">▶</span>
                            <span>Thinking</span>
                        </div>
                        <div class="thinking-body"></div>
                    </div>
                    <div class="message-bubble"></div>
                </div>
            `;
            elements.chatMessages.appendChild(row);

            const thinkBlock = row.querySelector('.thinking-block');
            const thinkBody = row.querySelector('.thinking-body');
            const thinkHeader = row.querySelector('.thinking-header');
            const bubble = row.querySelector('.message-bubble');

            // Toggle thinking visibility on click
            thinkHeader.addEventListener('click', () => {
                thinkHeader.classList.toggle('open');
                thinkBody.classList.toggle('open');
            });

            // Process the SSE stream
            let fullReply = '';
            let fullThink = '';
            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop();

                for (const line of lines) {
                    if (!line.startsWith('data: ')) continue;

                    const raw = line.slice(6);
                    if (raw === '[DONE]') break;

                    if (raw.startsWith('[THINK]')) {
                        // Thinking token
                        fullThink += raw.slice(7).replace(/\\n/g, '\n');
                        thinkBlock.style.display = 'block';
                        thinkBody.textContent = fullThink;
                    } else {
                        // Response token
                        fullReply += raw.replace(/\\n/g, '\n');
                        bubble.textContent = fullReply;
                    }
                    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
                }
            }

            // Add assistant response to history
            state.chatHistory.push({ role: 'assistant', content: fullReply });

        } catch (err) {
            removeTyping();
            addMessage('error', 'Connection error: ' + err.message);
        } finally {
            elements.sendButton.disabled = false;
            elements.chatStatus.textContent = '';
        }
    }

    /**
     * Clears the chat history
     */
    function clearChat() {
        elements.chatMessages.innerHTML = '';
        state.chatHistory = [];
        addMessage('assistant', 'History cleared. Start a new conversation.');
    }

    // ==========================================================================
    // Models Management
    // ==========================================================================

    /**
     * Loads the list of installed models from the API
     */
    async function loadModels() {
        elements.modelsStatus.textContent = 'Loading…';
        elements.modelsTable.innerHTML = '';

        try {
            const response = await fetch(`${API_BASE}/api/models`);
            const models = await response.json();

            if (!Array.isArray(models) || models.length === 0) {
                elements.modelsStatus.textContent = 'No models installed.';
                elements.modelSelector.innerHTML = '<option value="">No models</option>';
                return;
            }

            // Populate table
            models.forEach(model => {
                const row = document.createElement('tr');
                const modified = model.modifiedAt ? model.modifiedAt.slice(0, 10) : '—';
                const size = model.formattedSize || formatBytes(model.size);

                row.innerHTML = `
                    <td>${escapeHtml(model.name)}</td>
                    <td class="muted">${size}</td>
                    <td class="muted">${modified}</td>
                    <td><input type="radio" name="selModel" value="${escapeHtml(model.name)}"></td>
                `;
                elements.modelsTable.appendChild(row);
            });

            elements.modelsStatus.textContent = `${models.length} model(s) available`;

            // Update model selector in chat
            elements.modelSelector.innerHTML = models.map(m =>
                `<option value="${escapeHtml(m.name)}">${escapeHtml(m.name)}</option>`
            ).join('');

            // Enable delete button on selection
            document.querySelectorAll('input[name="selModel"]').forEach(radio =>
                radio.addEventListener('change', () => {
                    elements.deleteBtn.disabled = false;
                })
            );

        } catch (e) {
            elements.modelsStatus.textContent = 'Error: ' + e.message;
        }
    }

    /**
     * Downloads a new model from the Ollama registry
     */
    async function pullModel() {
        const name = elements.pullInput.value.trim();
        if (!name) {
            elements.modelsStatus.textContent = 'Enter a model name.';
            return;
        }

        elements.pullBtn.disabled = true;
        elements.modelsStatus.innerHTML = `<span class="spinner"></span>Downloading ${escapeHtml(name)}… (this may take a while)`;
        elements.pullInput.value = '';

        try {
            const response = await fetch(`${API_BASE}/api/pull`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ model: name })
            });

            const data = await response.json();
            elements.modelsStatus.textContent = (response.ok && data.status === 'ok')
                ? `✓ "${name}" downloaded successfully.`
                : '✗ ' + (data.error || 'Unknown error');

            if (response.ok && data.status === 'ok') {
                await loadModels();
            }
        } catch {
            elements.modelsStatus.textContent = '✗ Network error.';
        } finally {
            elements.pullBtn.disabled = false;
        }
    }

    /**
     * Deletes the selected model
     */
    async function deleteModel() {
        const selected = document.querySelector('input[name="selModel"]:checked');
        if (!selected) return;

        const name = selected.value;
        if (!confirm(`Delete "${name}"? This action cannot be undone.`)) return;

        elements.modelsStatus.innerHTML = `<span class="spinner"></span>Deleting ${name}…`;
        elements.deleteBtn.disabled = true;

        try {
            const response = await fetch(`${API_BASE}/api/models/${encodeURIComponent(name)}`, {
                method: 'DELETE'
            });

            const data = await response.json().catch(() => ({}));
            elements.modelsStatus.textContent = response.ok
                ? `✓ "${name}" deleted.`
                : '✗ ' + (data.error || response.statusText);

            if (response.ok) {
                await loadModels();
            }
        } catch {
            elements.modelsStatus.textContent = '✗ Network error.';
        }
    }

    // ==========================================================================
    // Settings
    // ==========================================================================

    /**
     * Saves the Ollama host settings
     */
    async function saveSettings() {
        const host = elements.settingsHost.value.trim();

        if (!host) {
            elements.settingsStatus.textContent = 'Host cannot be empty.';
            return;
        }

        try {
            const response = await fetch(`${API_BASE}/api/settings`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ host })
            });

            elements.settingsStatus.textContent = response.ok ? '✓ Saved.' : '✗ Error saving.';

            if (response.ok) {
                // Re-check connection with new host
                await checkConnection();
            }
        } catch {
            elements.settingsStatus.textContent = '✗ No connection.';
        }

        setTimeout(() => {
            elements.settingsStatus.textContent = '';
        }, 4000);
    }

    // ==========================================================================
    // Event Listeners
    // ==========================================================================

    function setupEventListeners() {
        // Navigation
        VIEWS.forEach(view => {
            elements.nav[view].addEventListener('click', () => showView(view));
        });

        // Chat
        elements.sendButton.addEventListener('click', sendMessage);
        elements.chatInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
            }
        });
        elements.chatInput.addEventListener('input', function () {
            this.style.height = 'auto';
            this.style.height = Math.min(this.scrollHeight, 160) + 'px';
        });
        elements.clearChatBtn.addEventListener('click', clearChat);

        // Models
        elements.refreshModelsBtn.addEventListener('click', loadModels);
        elements.pullBtn.addEventListener('click', pullModel);
        elements.deleteBtn.addEventListener('click', deleteModel);

        // Settings
        elements.saveSettingsBtn.addEventListener('click', saveSettings);
    }

    // ==========================================================================
    // Initialization
    // ==========================================================================

    function init() {
        cacheElements();
        setupEventListeners();

        // Initial data load
        checkConnection();
        loadModels();
    }

    // Start the application when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
