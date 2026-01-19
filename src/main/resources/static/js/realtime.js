// Connects to STOMP over WebSocket, subscribes to topics, updates UI, handles errors and reconnection
(function(){
  const sock = new SockJS('/ws');
  const client = Stomp.over(sock);
  client.debug = function(){}; // silence

  let connected = false;
  let retries = 0;

  function connect(){
    client.connect({}, function(){
      connected = true; retries = 0;
      window.dispatchEvent(new CustomEvent('rt:connected'));

      client.subscribe('/topic/resume/optimized', function(msg){
        const data = JSON.parse(msg.body);
        const atsOrigEl = document.querySelector('[data-rt="atsOriginal"]');
        const atsOptEl = document.querySelector('[data-rt="atsOptimized"]');
        const optEl = document.querySelector('[data-rt="optimizedText"]');
        const kwEl = document.querySelector('[data-rt="keywords"]');
        if (atsOrigEl && typeof data.atsOriginal !== 'undefined') atsOrigEl.textContent = data.atsOriginal + '%';
        if (atsOptEl && typeof data.atsOptimized !== 'undefined') atsOptEl.textContent = data.atsOptimized + '%';
        if (optEl && typeof data.optimizedText === 'string') optEl.textContent = data.optimizedText;
        if (kwEl && Array.isArray(data.injectedKeywords)) {
          kwEl.innerHTML = '';
          data.injectedKeywords.slice(0,50).forEach(k=>{
            const span=document.createElement('span');
            span.className='badge bg-warning text-dark';
            span.style.margin='2px';
            span.textContent=k;
            kwEl.appendChild(span);
          });
        }
        if (Array.isArray(data.insights)) {
          const list = document.getElementById('insights');
          if (list) {
            list.innerHTML = '';
            data.insights.forEach(i=>{
              const li = document.createElement('li');
              li.textContent = i;
              list.appendChild(li);
            });
          }
        }
        if (typeof data.pdfUrl === 'string') {
          const btn = document.querySelector('a.btn-success[href*="/resume/pdf/"]');
          if (btn) btn.setAttribute('href', data.pdfUrl);
        }
        showToast('Resume updated in real-time');
      });

      // Subscribe to new job notifications
      client.subscribe('/topic/jobs/new', function(msg){
        const data = JSON.parse(msg.body);
        showToast('New job available: ' + data.job.title);
        // Optionally add the new job to the UI
        if (typeof addNewJobToUI === 'function') {
          addNewJobToUI(data.job);
        }
      });

      client.subscribe('/topic/tracking/applications', function(msg){
        const app = JSON.parse(msg.body);
        const tbody = document.querySelector('[data-rt="appsTable"]');
        if (tbody) {
          const tr = document.createElement('tr');
          tr.innerHTML = `<td>${escapeHtml(app.jobTitle||'')}</td>
                          <td>${escapeHtml(app.companyName||'')}</td>
          <td>${escapeHtml(app.applicationDate||'')}</td>
          <td>${escapeHtml(app.status||'')}</td>
          <td>${app.interviewLikelihood? Math.round(app.interviewLikelihood*100)+'%':''}</td>
          <td>${escapeHtml(app.notes||'')}</td>
          <td><a target="_blank" href="${escapeAttr(app.applyUrl||'#')}">Open</a></td>`;
          tbody.prepend(tr);
        }
        showToast('Application added (synced)');
      });
    }, function(){
      connected = false;
      const delay = Math.min(1000 * Math.pow(2,retries++), 15000);
      setTimeout(connect, delay);
    });
  }

  function showToast(text){
    const div = document.createElement('div');
    div.className = 'alert alert-info position-fixed top-0 end-0 m-3';
    div.textContent = text;
    document.body.appendChild(div);
    setTimeout(()=>div.remove(), 2000);
  }

  function escapeHtml(s){return s.replace(/[&<>"]+/g, c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;'}[c]));}
  function escapeAttr(s){return s.replace(/["']+/g, '');}

  // Function to refresh job listings from API
  function refreshJobs() {
    // This would fetch updated job listings from the API
    // For now, we'll just show a toast to indicate refresh
    showToast('Refreshing job listings...');
    
    // Example of how to fetch updated jobs from API
    // fetch('/api/jobs')
    //   .then(response => response.json())
    //   .then(data => {
    //     if (data.success && data.jobs) {
    //       updateJobListings(data.jobs);
    //     }
    //   })
    //   .catch(error => console.error('Error refreshing jobs:', error));
  }
  
  // Function to add a new job to the UI
  function addNewJobToUI(job) {
    // Find the jobs container
    const jobsContainer = document.querySelector('#jobsList') || document.querySelector('.list-group');
    
    if (!jobsContainer) {
      console.log('Jobs container not found');
      return;
    }
    
    // Create job card element
    const jobCard = createJobCardElement(job);
    
    // Add to the top of the list
    jobsContainer.insertBefore(jobCard, jobsContainer.firstChild);
    
    // Update the job count in the stats
    updateJobCount();
    
    showToast('New job added: ' + job.title);
  }
  
  // Function to update job count display
  function updateJobCount() {
    const jobCards = document.querySelectorAll('.job-card');
    const jobCountElement = document.querySelector('[data-rt="jobCount"]') || document.querySelector('.text-muted');
    if (jobCountElement) {
      // Update text that contains 'jobs' if possible
      const text = jobCountElement.textContent;
      if (text.includes('jobs')) {
        jobCountElement.textContent = text.replace(/\d+ jobs/, jobCards.length + ' jobs');
      }
    }
  }
  
  // Function to get new jobs since last visit
  function checkForNewJobs() {
    // Get the last visit timestamp from localStorage
    const lastVisit = localStorage.getItem('lastJobVisit');
    const now = Date.now();
    
    if (lastVisit) {
      // Fetch new jobs since last visit
      fetch(`/api/new-since?timestamp=${lastVisit}`)
        .then(response => response.json())
        .then(data => {
          if (data.success && data.jobs && data.jobs.length > 0) {
            // Add new jobs to the UI
            data.jobs.forEach(job => {
              addNewJobToUI(job);
            });
            
            showToast(`Found ${data.jobs.length} new jobs since your last visit!`);
          }
        })
        .catch(error => console.error('Error fetching new jobs:', error));
    }
    
    // Update the last visit timestamp
    localStorage.setItem('lastJobVisit', now);
  }
  
  // Initialize job tracking when page loads
  document.addEventListener('DOMContentLoaded', function() {
    // Record the visit time
    localStorage.setItem('lastJobVisit', Date.now());
    
    // Optionally check for new jobs after a delay
    setTimeout(checkForNewJobs, 2000); // Check for new jobs after 2 seconds
  });
  
  // Function to create a job card element
  function createJobCardElement(job) {
    const div = document.createElement('div');
    div.className = 'list-group-item p-4 job-card';
    
    const postedDate = job.postedDate ? new Date(job.postedDate).toLocaleDateString() : 'N/A';
    
    div.innerHTML = `      <div class="d-flex justify-content-between align-items-start">
        <div>
          <h5 class="mb-1">
            <a href="${job.applyUrl || '#'}" target="_blank" class="text-decoration-none text-dark">
              ${escapeHtml(job.title || 'No Title')}
            </a>
          </h5>
          <small class="text-muted">
            ${escapeHtml(job.company || 'No Company')} • 
            <span class="source-badge source-portal">
              <i class="fas fa-globe me-1"></i>Portal
            </span>
            <span class="badge bg-light text-dark border ms-1">${postedDate}</span>
          </small>
        </div>
        <div>
          <a href="${job.applyUrl || '#'}" target="_blank" class="btn btn-primary-modern btn-sm">
            Apply Now <i class="fas fa-external-link-alt ms-1"></i>
          </a>
        </div>
      </div>
      <p class="mb-0 mt-2 text-muted small">
        ${escapeHtml(job.description || 'No description available').substring(0, 200)}...
      </p>`;
    
    return div;
  }
  
  // Offline queue for tracking adds
  const queueKey = 'rt:trackingQueue';
  function enqueueTracking(formData){
    const q = JSON.parse(localStorage.getItem(queueKey)||'[]');
    q.push(formData);
    localStorage.setItem(queueKey, JSON.stringify(q));
    showToast('Saved offline; will sync on reconnection');
  }

  async function flushQueue(){
    const q = JSON.parse(localStorage.getItem(queueKey)||'[]');
    if (!q.length) return;
    for (const item of q){
      try {
        await fetch('/tracking/add', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: new URLSearchParams(item)});
      } catch(e){ /* keep item if fails */ }
    }
    localStorage.removeItem(queueKey);
  }

  window.addEventListener('online', flushQueue);
  window.addEventListener('rt:connected', flushQueue);

  // Hook tracking form to offline queue
  document.addEventListener('submit', function(e){
    const f = e.target;
    if (f && f.getAttribute('data-rt') === 'trackingForm'){
      if (!navigator.onLine){
        e.preventDefault();
        const data = Object.fromEntries(new FormData(f).entries());
        enqueueTracking(data);
      }
    }
  });

  connect();
  // Track Apply link clicks: store as application instantly
  document.addEventListener('click', function(e){
    const a = e.target.closest('a[data-rt="applyLink"]');
    if (!a) return;
    const item = a.closest('.list-group-item');
    const title = item ? item.querySelector('h5')?.textContent || '' : '';
    const company = item ? item.querySelector('small')?.textContent || '' : '';
    const applyUrl = a.getAttribute('href') || '';
    // Fire-and-forget POST to track
    fetch('/jobs/track', {
      method: 'POST', headers: {'Content-Type':'application/json'},
      body: JSON.stringify({jobTitle:title, companyName:company, applyUrl:applyUrl})
    }).catch(()=>{});
  });
})();