// Modern UI JavaScript Framework

class ModernUI {
  constructor() {
    this.init();
  }

  init() {
    this.setupAnimations();
    this.setupTooltips();
    this.setupSmoothScroll();
    this.setupFormValidation();
    this.setupLazyLoading();
  }

  // Setup scroll animations
  setupAnimations() {
    const observerOptions = {
      threshold: 0.1,
      rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('animate-fade-in');
          observer.unobserve(entry.target);
        }
      });
    }, observerOptions);

    document.querySelectorAll('.card-modern, .job-card-modern, .stats-card').forEach(el => {
      observer.observe(el);
    });
  }

  // Setup tooltips
  setupTooltips() {
    document.querySelectorAll('[data-tooltip]').forEach(el => {
      el.addEventListener('mouseenter', (e) => {
        const tooltip = document.createElement('div');
        tooltip.className = 'tooltip-modern';
        tooltip.textContent = e.target.dataset.tooltip;
        document.body.appendChild(tooltip);
        
        const rect = e.target.getBoundingClientRect();
        tooltip.style.left = rect.left + (rect.width / 2) - (tooltip.offsetWidth / 2) + 'px';
        tooltip.style.top = rect.top - tooltip.offsetHeight - 10 + 'px';
        
        setTimeout(() => tooltip.classList.add('show'), 10);
      });
      
      el.addEventListener('mouseleave', () => {
        document.querySelectorAll('.tooltip-modern').forEach(t => t.remove());
      });
    });
  }

  // Smooth scroll
  setupSmoothScroll() {
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
      anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
          target.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
          });
        }
      });
    });
  }

  // Form validation
  setupFormValidation() {
    const forms = document.querySelectorAll('form[data-validate]');
    forms.forEach(form => {
      form.addEventListener('submit', (e) => {
        if (!this.validateForm(form)) {
          e.preventDefault();
          this.showToast('Please fill in all required fields', 'error');
        }
      });
    });
  }

  validateForm(form) {
    let isValid = true;
    const requiredFields = form.querySelectorAll('[required]');
    
    requiredFields.forEach(field => {
      if (!field.value.trim()) {
        field.classList.add('is-invalid');
        isValid = false;
      } else {
        field.classList.remove('is-invalid');
      }
    });
    
    return isValid;
  }

  // Lazy loading for images
  setupLazyLoading() {
    if ('IntersectionObserver' in window) {
      const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            const img = entry.target;
            img.src = img.dataset.src;
            img.classList.remove('lazy');
            imageObserver.unobserve(img);
          }
        });
      });

      document.querySelectorAll('img[data-src]').forEach(img => {
        imageObserver.observe(img);
      });
    }
  }

  // Show toast notification
  showToast(message, type = 'info', duration = 3000) {
    const container = document.querySelector('.toast-container') || this.createToastContainer();
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
      <div class="d-flex align-items-center">
        <i class="fas fa-${this.getToastIcon(type)} me-2"></i>
        <span>${message}</span>
        <button class="btn-close ms-auto" onclick="this.parentElement.parentElement.remove()"></button>
      </div>
    `;
    
    container.appendChild(toast);
    
    setTimeout(() => {
      toast.style.animation = 'slideOut 0.3s ease-out';
      setTimeout(() => toast.remove(), 300);
    }, duration);
  }

  createToastContainer() {
    const container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
    return container;
  }

  getToastIcon(type) {
    const icons = {
      success: 'check-circle',
      error: 'exclamation-circle',
      warning: 'exclamation-triangle',
      info: 'info-circle'
    };
    return icons[type] || 'info-circle';
  }

  // Show loading state
  showLoading(element) {
    if (typeof element === 'string') {
      element = document.querySelector(element);
    }
    if (element) {
      element.innerHTML = '<div class="text-center py-5"><div class="spinner-modern mx-auto"></div><p class="mt-3 text-muted">Loading...</p></div>';
    }
  }

  // Hide loading state
  hideLoading(element, content) {
    if (typeof element === 'string') {
      element = document.querySelector(element);
    }
    if (element && content) {
      element.innerHTML = content;
    }
  }

  // Debounce function
  debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
      const later = () => {
        clearTimeout(timeout);
        func(...args);
      };
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
    };
  }

  // Throttle function
  throttle(func, limit) {
    let inThrottle;
    return function(...args) {
      if (!inThrottle) {
        func.apply(this, args);
        inThrottle = true;
        setTimeout(() => inThrottle = false, limit);
      }
    };
  }
}

// Search functionality
class ModernSearch {
  constructor(container, options = {}) {
    this.container = typeof container === 'string' ? document.querySelector(container) : container;
    this.options = {
      placeholder: 'Search...',
      debounce: 300,
      onSearch: null,
      ...options
    };
    this.init();
  }

  init() {
    this.createSearchBar();
    this.setupEventListeners();
  }

  createSearchBar() {
    this.container.innerHTML = `
      <div class="search-modern">
        <i class="fas fa-search search-icon"></i>
        <input type="text" class="form-control form-control-modern" 
               placeholder="${this.options.placeholder}" 
               id="modern-search-input">
      </div>
    `;
    this.input = this.container.querySelector('#modern-search-input');
  }

  setupEventListeners() {
    const debouncedSearch = new ModernUI().debounce((value) => {
      if (this.options.onSearch) {
        this.options.onSearch(value);
      }
    }, this.options.debounce);

    this.input.addEventListener('input', (e) => {
      debouncedSearch(e.target.value);
    });
  }

  getValue() {
    return this.input.value;
  }

  setValue(value) {
    this.input.value = value;
  }

  clear() {
    this.input.value = '';
  }
}

// Filter system
class ModernFilter {
  constructor(container, options = {}) {
    this.container = typeof container === 'string' ? document.querySelector(container) : container;
    this.options = {
      filters: [],
      onFilter: null,
      ...options
    };
    this.activeFilters = new Set();
    this.init();
  }

  init() {
    this.createFilterChips();
    this.setupEventListeners();
  }

  createFilterChips() {
    const chipsContainer = document.createElement('div');
    chipsContainer.className = 'filter-chips';
    
    this.options.filters.forEach(filter => {
      const chip = document.createElement('button');
      chip.className = 'filter-chip';
      chip.textContent = filter.label;
      chip.dataset.filter = filter.value;
      chipsContainer.appendChild(chip);
    });
    
    this.container.appendChild(chipsContainer);
  }

  setupEventListeners() {
    this.container.querySelectorAll('.filter-chip').forEach(chip => {
      chip.addEventListener('click', () => {
        const filterValue = chip.dataset.filter;
        
        if (this.activeFilters.has(filterValue)) {
          this.activeFilters.delete(filterValue);
          chip.classList.remove('active');
        } else {
          this.activeFilters.add(filterValue);
          chip.classList.add('active');
        }
        
        if (this.options.onFilter) {
          this.options.onFilter(Array.from(this.activeFilters));
        }
      });
    });
  }

  getActiveFilters() {
    return Array.from(this.activeFilters);
  }

  clearFilters() {
    this.activeFilters.clear();
    this.container.querySelectorAll('.filter-chip').forEach(chip => {
      chip.classList.remove('active');
    });
    if (this.options.onFilter) {
      this.options.onFilter([]);
    }
  }
}

// Pagination
class ModernPagination {
  constructor(container, options = {}) {
    this.container = typeof container === 'string' ? document.querySelector(container) : container;
    this.options = {
      currentPage: 1,
      totalPages: 1,
      itemsPerPage: 10,
      onPageChange: null,
      ...options
    };
    this.init();
  }

  init() {
    this.render();
  }

  render() {
    const { currentPage, totalPages } = this.options;
    let html = '<nav><ul class="pagination justify-content-center">';
    
    // Previous button
    html += `<li class="page-item ${currentPage === 1 ? 'disabled' : ''}">
      <a class="page-link" href="#" data-page="${currentPage - 1}">Previous</a>
    </li>`;
    
    // Page numbers
    for (let i = 1; i <= totalPages; i++) {
      if (i === 1 || i === totalPages || (i >= currentPage - 2 && i <= currentPage + 2)) {
        html += `<li class="page-item ${i === currentPage ? 'active' : ''}">
          <a class="page-link" href="#" data-page="${i}">${i}</a>
        </li>`;
      } else if (i === currentPage - 3 || i === currentPage + 3) {
        html += '<li class="page-item disabled"><span class="page-link">...</span></li>';
      }
    }
    
    // Next button
    html += `<li class="page-item ${currentPage === totalPages ? 'disabled' : ''}">
      <a class="page-link" href="#" data-page="${currentPage + 1}">Next</a>
    </li>`;
    
    html += '</ul></nav>';
    this.container.innerHTML = html;
    this.setupEventListeners();
  }

  setupEventListeners() {
    this.container.querySelectorAll('.page-link').forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const page = parseInt(link.dataset.page);
        if (page && page !== this.options.currentPage && page >= 1 && page <= this.options.totalPages) {
          this.setPage(page);
        }
      });
    });
  }

  setPage(page) {
    this.options.currentPage = page;
    this.render();
    if (this.options.onPageChange) {
      this.options.onPageChange(page);
    }
  }

  update(totalPages) {
    this.options.totalPages = totalPages;
    if (this.options.currentPage > totalPages) {
      this.options.currentPage = totalPages;
    }
    this.render();
  }
}

// Initialize on DOM ready
document.addEventListener('DOMContentLoaded', () => {
  window.modernUI = new ModernUI();
  window.ModernSearch = ModernSearch;
  window.ModernFilter = ModernFilter;
  window.ModernPagination = ModernPagination;
});

