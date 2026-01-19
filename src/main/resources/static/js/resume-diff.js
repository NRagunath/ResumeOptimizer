// Resume Diff Change Management JavaScript

let resumeId = null;
let changes = [];
let originalText = '';
let optimizedText = '';

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', function() {
    // Get resume ID from data attribute or URL
    const resumeIdElement = document.getElementById('resumeId');
    if (resumeIdElement) {
        resumeId = resumeIdElement.textContent || resumeIdElement.getAttribute('data-resume-id');
    }
    
    // Get original and optimized text
    const origElement = document.getElementById('origText');
    const optElement = document.getElementById('optText');
    if (origElement) originalText = origElement.textContent || '';
    if (optElement) optimizedText = optElement.textContent || '';
    
    if (resumeId) {
        loadChanges();
        setupPreviewRefresh();
    } else {
        // Fallback to simple diff if no resume ID
        renderSimpleDiff();
    }
});

// Load changes from backend
async function loadChanges() {
    try {
        console.log('Loading changes for resume:', resumeId);
        const response = await fetch(`/resume/changes/${resumeId}`);
        if (!response.ok) {
            throw new Error('Failed to load changes: ' + response.status);
        }
        changes = await response.json();
        console.log('Loaded', changes.length, 'changes');
        
        if (changes.length === 0) {
            console.warn('No changes found');
            // Show message that no changes were detected
            const changesList = document.getElementById('changesList');
            if (changesList) {
                changesList.innerHTML = '<div class="text-center text-muted py-4"><i class="fas fa-info-circle"></i> No changes detected. The optimized resume is identical to the original.</div>';
            }
            renderSimpleDiff();
        } else {
            renderChanges();
            updatePreview();
        }
    } catch (error) {
        console.error('Error loading changes:', error);
        const changesList = document.getElementById('changesList');
        if (changesList) {
            changesList.innerHTML = '<div class="text-center text-danger py-4"><i class="fas fa-exclamation-triangle"></i> Error loading changes: ' + error.message + '</div>';
        }
        renderSimpleDiff();
    }
}

// Render changes with highlighting
function renderChanges() {
    const leftPane = document.getElementById('leftPane');
    const rightPane = document.getElementById('rightPane');
    const changesList = document.getElementById('changesList');
    
    if (!leftPane || !rightPane) return;
    
    // Start with escaped text
    let leftHtml = escapeHtml(originalText);
    let rightHtml = escapeHtml(optimizedText);
    
    // Filter out changes that don't have valid positions or text
    const validChanges = changes.filter(c => {
        if (c.changeType === 'DELETE' || c.changeType === 'MODIFY') {
            return c.startPosition != null && c.endPosition != null && c.originalText;
        } else if (c.changeType === 'INSERT') {
            return c.newStartPosition != null && c.newEndPosition != null && c.newText;
        }
        return false;
    });
    
    if (validChanges.length === 0) {
        // No valid changes, just show the text
        leftPane.innerHTML = `<pre style="white-space: pre-wrap; margin: 0;">${leftHtml}</pre>`;
        rightPane.innerHTML = `<pre style="white-space: pre-wrap; margin: 0;">${rightHtml}</pre>`;
        if (changesList) {
            changesList.innerHTML = '<div class="text-center text-muted py-4">No changes detected</div>';
        }
        return;
    }
    
    // Sort changes by position (reverse for deletions to preserve positions)
    const deleteModifyChanges = validChanges
        .filter(c => c.changeType === 'DELETE' || c.changeType === 'MODIFY')
        .sort((a, b) => (b.startPosition || 0) - (a.startPosition || 0));
    
    // Apply deletions/modifications to left pane (from end to start)
    deleteModifyChanges.forEach((change) => {
        const start = change.startPosition || 0;
        const end = change.endPosition || start;
        const originalText = change.originalText || '';
        const changeId = change.id;
        const status = change.status || 'PENDING';
        
        // Find the text in the escaped HTML
        const escapedOriginal = escapeHtml(originalText);
        const startIdx = leftHtml.indexOf(escapedOriginal, start);
        
        if (startIdx !== -1 && startIdx >= 0 && startIdx < leftHtml.length) {
            const endIdx = startIdx + escapedOriginal.length;
            const before = leftHtml.substring(0, startIdx);
            const deleted = leftHtml.substring(startIdx, endIdx);
            const after = leftHtml.substring(endIdx);
            
            const statusClass = status === 'ACCEPTED' ? 'accepted' : status === 'DECLINED' ? 'declined' : 'pending';
            leftHtml = before + 
                `<span class="change-delete ${statusClass}" data-change-id="${changeId}" title="${escapeHtml(change.description || '')}">${deleted}</span>` + 
                after;
        } else {
            // Fallback: try to find by position
            if (start < leftHtml.length && end <= leftHtml.length) {
                const before = leftHtml.substring(0, start);
                const deleted = leftHtml.substring(start, end);
                const after = leftHtml.substring(end);
                
                const statusClass = status === 'ACCEPTED' ? 'accepted' : status === 'DECLINED' ? 'declined' : 'pending';
                leftHtml = before + 
                    `<span class="change-delete ${statusClass}" data-change-id="${changeId}" title="${escapeHtml(change.description || '')}">${deleted}</span>` + 
                    after;
            }
        }
    });
    
    // Sort insertions by new position (reverse order)
    const insertModifyChanges = validChanges
        .filter(c => c.changeType === 'INSERT' || c.changeType === 'MODIFY')
        .sort((a, b) => (b.newStartPosition || 0) - (a.newStartPosition || 0));
    
    // Apply insertions/modifications to right pane (from end to start)
    insertModifyChanges.forEach((change) => {
        const start = change.newStartPosition || 0;
        const end = change.newEndPosition || start + (change.newText || '').length;
        const newText = change.newText || '';
        const changeId = change.id;
        const status = change.status || 'PENDING';
        
        // Find the text in the escaped HTML
        const escapedNew = escapeHtml(newText);
        const startIdx = rightHtml.indexOf(escapedNew, start);
        
        if (startIdx !== -1 && startIdx >= 0 && startIdx < rightHtml.length) {
            const endIdx = startIdx + escapedNew.length;
            const before = rightHtml.substring(0, startIdx);
            const inserted = rightHtml.substring(startIdx, endIdx);
            const after = rightHtml.substring(endIdx);
            
            const statusClass = status === 'ACCEPTED' ? 'accepted' : status === 'DECLINED' ? 'declined' : 'pending';
            rightHtml = before + 
                `<span class="change-insert ${statusClass}" data-change-id="${changeId}" title="${escapeHtml(change.description || '')}">${inserted}</span>` + 
                after;
        } else {
            // Fallback: try to find by position
            if (start < rightHtml.length && end <= rightHtml.length) {
                const before = rightHtml.substring(0, start);
                const inserted = rightHtml.substring(start, end);
                const after = rightHtml.substring(end);
                
                const statusClass = status === 'ACCEPTED' ? 'accepted' : status === 'DECLINED' ? 'declined' : 'pending';
                rightHtml = before + 
                    `<span class="change-insert ${statusClass}" data-change-id="${changeId}" title="${escapeHtml(change.description || '')}">${inserted}</span>` + 
                    after;
            }
        }
    });
    
    leftPane.innerHTML = `<pre style="white-space: pre-wrap; margin: 0;">${leftHtml}</pre>`;
    rightPane.innerHTML = `<pre style="white-space: pre-wrap; margin: 0;">${rightHtml}</pre>`;
    
    // Render changes list
    if (changesList) {
        renderChangesList(changesList);
        // Update change count
        const changeCountElement = document.getElementById('changeCount');
        if (changeCountElement) {
            changeCountElement.textContent = changes.length;
        }
    }
    
    // Attach event listeners to change elements
    attachChangeListeners();
}

// Render changes list with accept/decline buttons
function renderChangesList(container) {
    container.innerHTML = '';
    
    changes.forEach((change, index) => {
        const changeDiv = document.createElement('div');
        changeDiv.className = `change-item change-${change.changeType.toLowerCase()} status-${(change.status || 'PENDING').toLowerCase()}`;
        changeDiv.setAttribute('data-change-id', change.id);
        
        const status = change.status || 'PENDING';
        const statusIcon = status === 'ACCEPTED' ? '✓' : status === 'DECLINED' ? '✗' : '○';
        const statusText = status === 'ACCEPTED' ? 'Accepted' : status === 'DECLINED' ? 'Declined' : 'Pending';
        
        let changeDescription = '';
        if (change.changeType === 'INSERT') {
            changeDescription = `Added: "${truncate(change.newText, 50)}"`;
        } else if (change.changeType === 'DELETE') {
            changeDescription = `Removed: "${truncate(change.originalText, 50)}"`;
        } else if (change.changeType === 'MODIFY') {
            changeDescription = `Changed: "${truncate(change.originalText, 30)}" → "${truncate(change.newText, 30)}"`;
        }
        
        changeDiv.innerHTML = `
            <div class="change-header">
                <span class="change-number">#${index + 1}</span>
                <span class="change-type">${change.changeType}</span>
                <span class="change-section">${change.section || 'Unknown'}</span>
                <span class="change-status status-${status.toLowerCase()}">${statusIcon} ${statusText}</span>
            </div>
            <div class="change-description">${escapeHtml(changeDescription)}</div>
            <div class="change-actions">
                <button class="btn btn-sm btn-success accept-btn" ${status === 'ACCEPTED' ? 'disabled' : ''} 
                    onclick="acceptChange(${change.id})">
                    <i class="fas fa-check"></i> Accept
                </button>
                <button class="btn btn-sm btn-danger decline-btn" ${status === 'DECLINED' ? 'disabled' : ''} 
                    onclick="declineChange(${change.id})">
                    <i class="fas fa-times"></i> Decline
                </button>
            </div>
        `;
        
        container.appendChild(changeDiv);
    });
}

// Attach event listeners to highlighted changes
function attachChangeListeners() {
    document.querySelectorAll('.change-delete, .change-insert').forEach(element => {
        element.addEventListener('click', function() {
            const changeId = this.getAttribute('data-change-id');
            const change = changes.find(c => c.id == changeId);
            if (change) {
                // Scroll to change in list
                const changeItem = document.querySelector(`.change-item[data-change-id="${changeId}"]`);
                if (changeItem) {
                    changeItem.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    changeItem.classList.add('highlight');
                    setTimeout(() => changeItem.classList.remove('highlight'), 2000);
                }
            }
        });
    });
}

// Accept a change
async function acceptChange(changeId) {
    try {
        const response = await fetch(`/resume/changes/${changeId}/accept`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to accept change');
        }
        
        const updatedChange = await response.json();
        // Update local changes array
        const index = changes.findIndex(c => c.id === changeId);
        if (index !== -1) {
            changes[index] = updatedChange;
        }
        
        renderChanges();
        updatePreview();
        showNotification('Change accepted', 'success');
    } catch (error) {
        console.error('Error accepting change:', error);
        showNotification('Failed to accept change', 'error');
    }
}

// Decline a change
async function declineChange(changeId) {
    try {
        const response = await fetch(`/resume/changes/${changeId}/decline`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to decline change');
        }
        
        const updatedChange = await response.json();
        // Update local changes array
        const index = changes.findIndex(c => c.id === changeId);
        if (index !== -1) {
            changes[index] = updatedChange;
        }
        
        renderChanges();
        updatePreview();
        showNotification('Change declined', 'success');
    } catch (error) {
        console.error('Error declining change:', error);
        showNotification('Failed to decline change', 'error');
    }
}

// Update preview
async function updatePreview() {
    if (!resumeId) return;
    
    try {
        const response = await fetch(`/resume/preview/${resumeId}`);
        if (!response.ok) {
            throw new Error('Failed to load preview');
        }
        
        const data = await response.json();
        const previewPane = document.getElementById('previewPane');
        const statsPane = document.getElementById('statsPane');
        
        if (previewPane) {
            previewPane.innerHTML = `<pre style="white-space: pre-wrap; font-size: 0.85em; margin: 0;">${escapeHtml(data.preview || '')}</pre>`;
        }
        
        if (statsPane && data.statistics) {
            const stats = data.statistics;
            statsPane.innerHTML = `
                <div class="stats-grid">
                    <div class="stat-item">
                        <div class="stat-value">${stats.total || 0}</div>
                        <div class="stat-label">Total Changes</div>
                    </div>
                    <div class="stat-item accepted">
                        <div class="stat-value">${stats.accepted || 0}</div>
                        <div class="stat-label">Accepted</div>
                    </div>
                    <div class="stat-item declined">
                        <div class="stat-value">${stats.declined || 0}</div>
                        <div class="stat-label">Declined</div>
                    </div>
                    <div class="stat-item pending">
                        <div class="stat-value">${stats.pending || 0}</div>
                        <div class="stat-label">Pending</div>
                    </div>
                </div>
            `;
        }
    } catch (error) {
        console.error('Error updating preview:', error);
    }
}

// Setup automatic preview refresh
function setupPreviewRefresh() {
    // Refresh preview every 2 seconds if there are pending changes
    setInterval(() => {
        const hasPending = changes.some(c => (c.status || 'PENDING') === 'PENDING');
        if (hasPending) {
            updatePreview();
        }
    }, 2000);
}

// Finalize resume
async function finalizeResume() {
    if (!resumeId) {
        showNotification('Resume ID not found', 'error');
        return;
    }
    
    if (!confirm('Generate final resume with all accepted changes? This will create the final version.')) {
        return;
    }
    
    try {
        const response = await fetch(`/resume/finalize/${resumeId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to finalize resume');
        }
        
        const data = await response.json();
        showNotification('Resume finalized successfully!', 'success');
        
        // Reload page after a delay
        setTimeout(() => {
            window.location.reload();
        }, 1500);
    } catch (error) {
        console.error('Error finalizing resume:', error);
        showNotification('Failed to finalize resume', 'error');
    }
}

// Fallback simple diff rendering
function renderSimpleDiff() {
    const leftPane = document.getElementById('leftPane');
    const rightPane = document.getElementById('rightPane');
    
    if (!leftPane || !rightPane) return;
    
    const orig = originalText || '';
    const opt = optimizedText || '';
    
    // Simple line-by-line diff
    const origLines = orig.split('\n');
    const optLines = opt.split('\n');
    const maxLines = Math.max(origLines.length, optLines.length);
    
    let leftHtml = '';
    let rightHtml = '';
    
    for (let i = 0; i < maxLines; i++) {
        const origLine = origLines[i] || '';
        const optLine = optLines[i] || '';
        
        if (origLine === optLine) {
            leftHtml += escapeHtml(origLine) + '\n';
            rightHtml += escapeHtml(optLine) + '\n';
        } else {
            if (origLine) {
                leftHtml += `<span class="removed">${escapeHtml(origLine)}</span>\n`;
            }
            if (optLine) {
                rightHtml += `<span class="added">${escapeHtml(optLine)}</span>\n`;
            }
        }
    }
    
    leftPane.innerHTML = leftHtml;
    rightPane.innerHTML = rightHtml;
}

// Utility functions
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function truncate(str, maxLen) {
    if (!str) return '';
    if (str.length <= maxLen) return str;
    return str.substring(0, maxLen - 3) + '...';
}

function showNotification(message, type) {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        background: ${type === 'success' ? '#28a745' : '#dc3545'};
        color: white;
        border-radius: 5px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.2);
        z-index: 10000;
        animation: slideIn 0.3s ease-out;
    `;
    
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease-out';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Make functions available globally
window.acceptChange = acceptChange;
window.declineChange = declineChange;
window.finalizeResume = finalizeResume;

