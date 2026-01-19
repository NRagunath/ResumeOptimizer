/**
 * Scraper Status Widget
 * Handles real-time updates of scraper status and manual refresh
 */

const ScraperStatus = {
    init: function() {
        this.statusContainer = document.getElementById('scraperStatusWidget');
        if (!this.statusContainer) return;

        this.lastUpdatedEl = document.getElementById('lastUpdatedTime');
        this.refreshBtn = document.getElementById('manualRefreshBtn');
        this.statusIndicator = document.getElementById('globalStatusIndicator');
        
        this.bindEvents();
        this.fetchStatus();
        
        // Poll for status every 60 seconds
        setInterval(() => this.fetchStatus(), 60000);
    },

    bindEvents: function() {
        if (this.refreshBtn) {
            this.refreshBtn.addEventListener('click', (e) => {
                e.preventDefault();
                this.triggerRefresh();
            });
        }
    },

    fetchStatus: function() {
        fetch('/api/scraper/health')
            .then(response => response.json())
            .then(data => {
                this.updateUI(data);
            })
            .catch(error => console.error('Error fetching scraper status:', error));
    },

    updateUI: function(data) {
        // Calculate overall health and last update time
        let overallStatus = 'HEALTHY';
        let lastSuccessTime = 0;
        let totalJobs = 0;

        Object.values(data).forEach(portal => {
            if (portal.status === 'ERROR') overallStatus = 'ERROR';
            else if (portal.status === 'WARNING' && overallStatus !== 'ERROR') overallStatus = 'WARNING';
            
            if (portal.lastSuccess) {
                const time = new Date(portal.lastSuccess).getTime();
                if (time > lastSuccessTime) lastSuccessTime = time;
            }
            
            totalJobs += (portal.lastJobCount || 0);
        });

        // Update status indicator
        if (this.statusIndicator) {
            this.statusIndicator.className = `status-dot ${this.getStatusClass(overallStatus)}`;
            this.statusIndicator.title = `System Status: ${overallStatus}`;
        }

        // Update last updated text
        if (this.lastUpdatedEl && lastSuccessTime > 0) {
            const timeAgo = this.timeSince(lastSuccessTime);
            this.lastUpdatedEl.textContent = `Updated ${timeAgo} ago`;
        }

        // Update portal specific icons if they exist
        Object.keys(data).forEach(portalName => {
            const iconEl = document.getElementById(`status-icon-${portalName}`);
            if (iconEl) {
                iconEl.className = `fas fa-circle ${this.getStatusTextClass(data[portalName].status)}`;
                iconEl.title = `${portalName}: ${data[portalName].status}`;
            }
        });
    },

    triggerRefresh: function() {
        const btn = this.refreshBtn;
        const originalContent = btn.innerHTML;
        
        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i> Refreshing...';

        fetch('/jobs/api/refresh', { method: 'POST' })
            .then(response => {
                if (response.ok) {
                    this.showToast('Scraping started. Jobs will appear shortly.', 'success');
                    // Poll more frequently for a short while
                    let checkCount = 0;
                    const interval = setInterval(() => {
                        this.fetchStatus();
                        checkCount++;
                        if (checkCount > 10) clearInterval(interval);
                    }, 5000);
                } else {
                    this.showToast('Failed to start scraping.', 'error');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                this.showToast('Error triggering refresh.', 'error');
            })
            .finally(() => {
                setTimeout(() => {
                    btn.disabled = false;
                    btn.innerHTML = originalContent;
                }, 2000);
            });
    },

    getStatusClass: function(status) {
        if (status === 'HEALTHY') return 'bg-success';
        if (status === 'WARNING') return 'bg-warning';
        return 'bg-danger';
    },

    getStatusTextClass: function(status) {
        if (status === 'HEALTHY') return 'text-success';
        if (status === 'WARNING') return 'text-warning';
        return 'text-danger';
    },

    timeSince: function(date) {
        const seconds = Math.floor((new Date() - date) / 1000);
        let interval = seconds / 31536000;
        if (interval > 1) return Math.floor(interval) + " years";
        interval = seconds / 2592000;
        if (interval > 1) return Math.floor(interval) + " months";
        interval = seconds / 86400;
        if (interval > 1) return Math.floor(interval) + " days";
        interval = seconds / 3600;
        if (interval > 1) return Math.floor(interval) + " hours";
        interval = seconds / 60;
        if (interval > 1) return Math.floor(interval) + " minutes";
        return Math.floor(seconds) + " seconds";
    },

    showToast: function(message, type = 'info') {
        // Simple toast implementation or use existing library
        // For now, just alert
        // alert(message); 
        // Better: create a temporary element
        const toast = document.createElement('div');
        toast.className = `toast-notification ${type}`;
        toast.textContent = message;
        toast.style.position = 'fixed';
        toast.style.bottom = '20px';
        toast.style.right = '20px';
        toast.style.padding = '10px 20px';
        toast.style.borderRadius = '4px';
        toast.style.color = '#fff';
        toast.style.backgroundColor = type === 'success' ? '#28a745' : '#dc3545';
        toast.style.zIndex = '9999';
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 3000);
    }
};

document.addEventListener('DOMContentLoaded', () => {
    ScraperStatus.init();
});
