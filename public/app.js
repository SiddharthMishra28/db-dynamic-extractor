document.addEventListener('DOMContentLoaded', () => {
    // --- GitLab API Configuration ---
    // These placeholders will be replaced by GitLab CI/CD
    const PROJECT_ID = "__PROJECT_ID__";
    const BRANCH_NAME = "__BRANCH_NAME__";
    const TRIGGER_TOKEN = "__TRIGGER_TOKEN__";

    const API_URL = `https://gitlab.com/api/v4/projects/${PROJECT_ID}/trigger/pipeline`;

    // --- DOM Element Selection ---
    const form = document.getElementById('pipeline-form');
    const queriesContainer = document.getElementById('queries-container');
    const addQueryBtn = document.getElementById('add-query-btn');
    const runJobBtn = document.getElementById('run-job-btn');
    const messageContainer = document.getElementById('message-container');
    const queryCardTemplate = document.getElementById('query-card-template');

    // --- Functions ---

    /**
     * Adds a new query card to the form.
     */
    const addQueryCard = () => {
        const cardClone = queryCardTemplate.content.cloneNode(true);
        const cardElement = cardClone.querySelector('.query-card');

        const removeBtn = cardElement.querySelector('.remove-btn');
        removeBtn.addEventListener('click', () => {
            // Prevent removing the last card
            if (queriesContainer.children.length > 1) {
                cardElement.remove();
            } else {
                showMessage('You must have at least one query.', 'error');
            }
        });

        queriesContainer.appendChild(cardElement);
    };

    /**
     * Displays a success or error message to the user.
     * @param {string} message - The message to display.
     * @param {string} type - 'success' or 'error'.
     */
    const showMessage = (message, type = 'success') => {
        messageContainer.innerHTML = message;
        messageContainer.className = 'message'; // Reset classes
        messageContainer.classList.add(type, 'show');

        // Hide the message after 5 seconds
        setTimeout(() => {
            messageContainer.classList.remove('show');
        }, 5000);
    };

    /**
     * Handles the form submission to trigger the GitLab pipeline.
     * @param {Event} event - The form submission event.
     */
    const handleFormSubmit = async (event) => {
        event.preventDefault();
        runJobBtn.disabled = true;
        runJobBtn.textContent = 'Running...';

        const queryCards = queriesContainer.querySelectorAll('.query-card');
        const queries = [];
        let hasError = false;

        queryCards.forEach(card => {
            const artifactName = card.querySelector('.artifact-name').value.trim();
            const queryText = card.querySelector('.query-text').value.trim();

            if (!artifactName || !queryText) {
                hasError = true;
                return;
            }
            queries.push(`${artifactName}=${queryText}`);
        });

        if (hasError) {
            showMessage('Please fill out all fields in all query cards.', 'error');
            runJobBtn.disabled = false;
            runJobBtn.textContent = 'Run Job';
            return;
        }

        const queriesString = queries.join(',');

        try {
            const response = await fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    token: TRIGGER_TOKEN,
                    ref: BRANCH_NAME,
                    'variables[QUERIES]': queriesString,
                }),
            });

            const data = await response.json();

            if (!response.ok) {
                const errorMessage = data.message || 'An unknown error occurred.';
                throw new Error(`API Error: ${errorMessage}`);
            }

            const pipelineLink = `<a href="${data.web_url}" target="_blank" rel="noopener noreferrer">View Pipeline</a>`;
            showMessage(`Pipeline triggered successfully! ${pipelineLink}`, 'success');

        } catch (error) {
            console.error('Pipeline trigger failed:', error);
            showMessage(`Error: ${error.message}`, 'error');
        } finally {
            runJobBtn.disabled = false;
            runJobBtn.textContent = 'Run Job';
        }
    };

    // --- Event Listeners ---
    addQueryBtn.addEventListener('click', addQueryCard);
    form.addEventListener('submit', handleFormSubmit);

    // --- Initial State ---
    // Add the first query card when the page loads
    addQueryCard();
});
