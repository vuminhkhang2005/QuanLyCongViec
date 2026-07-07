/**
 * ZenTask Application Logic
 * Modern JS (ES6+) integrating with Spring Boot RESTful API
 * Following ui-ux-pro-max-skill guidelines
 */

// Application Constants & Backend URLs
const BACKEND_URL = 'http://localhost:8080/api';
const API_BASE_URL = `${BACKEND_URL}/tasks`;
const PAGE_SIZE = 6; // Đặt size trang vừa phải cho bento bộc cục đẹp

// App State
let tasks = [];
let currentPage = 0;
let totalPages = 1;
let filters = {
    search: '',
    completed: 'all', // 'all', 'true', 'false'
    priority: 'all',  // 'all', 'HIGH', 'MEDIUM', 'LOW'
    sortBy: 'createdAt',
    sortDir: 'desc'
};
let selectedTaskIdForDelete = null;
let searchDebounceTimer = null;

// DOM Elements
const tasksGrid = document.getElementById('tasks-grid');
const shimmerLoader = document.getElementById('shimmer-loader');
const emptyState = document.getElementById('empty-state');
const statsText = document.getElementById('stats-text');
const progressBar = document.getElementById('progress-bar');
const paginationBar = document.getElementById('pagination-bar');
const pageIndicator = document.getElementById('page-indicator');
const btnPrevPage = document.getElementById('btn-prev-page');
const btnNextPage = document.getElementById('btn-next-page');

// Filters & Controls DOM
const searchInput = document.getElementById('search-input');
const statusFilterGroup = document.getElementById('status-filter');
const priorityFilterGroup = document.getElementById('priority-filter');
const sortBySelect = document.getElementById('sort-by');
const sortDirBtn = document.getElementById('sort-dir-btn');
const sortDirIcon = document.getElementById('sort-dir-icon');

// Modal Form DOM
const taskModal = document.getElementById('task-modal');
const taskForm = document.getElementById('task-form');
const modalTitle = document.getElementById('modal-title');
const taskIdInput = document.getElementById('task-id');
const inputTitle = document.getElementById('input-title');
const errorTitle = document.getElementById('error-title');
const inputDescription = document.getElementById('input-description');
const inputDueDate = document.getElementById('input-due-date');
const completedGroup = document.getElementById('completed-group');
const inputCompleted = document.getElementById('input-completed');
const btnSubmitText = document.getElementById('btn-submit-text');
const btnSubmitSpinner = document.getElementById('btn-submit-spinner');
const btnSubmitForm = document.getElementById('btn-submit-form');

// Confirm Delete Modal DOM
const confirmModal = document.getElementById('confirm-modal');
const deleteTaskTitleSpan = document.getElementById('delete-task-title');
const btnConfirmDelete = document.getElementById('btn-confirm-delete');

// ==========================================================================
// Authentication Helper Functions (JWT Interceptors)
// ==========================================================================

function getAuthHeaders() {
    const token = localStorage.getItem('accessToken');
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
    };
}

async function refreshTokens() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return false;

    try {
        const res = await fetch(`${BACKEND_URL}/auth/refresh`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken })
        });

        if (res.ok) {
            const data = await res.json();
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken);
            return true;
        }
    } catch (err) {
        console.error("Lỗi khi refresh token:", err);
    }
    return false;
}

function handleLogout() {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userEmail');
    window.location.href = 'login.html';
}

async function authenticatedFetch(url, options = {}) {
    options.headers = {
        ...options.headers,
        ...getAuthHeaders()
    };

    let response = await fetch(url, options);

    if (response.status === 401) {
        // Access Token hết hạn, thử refresh token
        const refreshed = await refreshTokens();
        if (refreshed) {
            // Thực hiện lại request với token mới
            options.headers = {
                ...options.headers,
                ...getAuthHeaders()
            };
            response = await fetch(url, options);
        } else {
            // Refresh token cũng hết hạn -> Yêu cầu login lại
            handleLogout();
            return new Response(JSON.stringify({ message: "Phiên đăng nhập đã hết hạn!" }), { status: 401 });
        }
    }

    return response;
}

// Init App
document.addEventListener('DOMContentLoaded', () => {
    // Hiển thị email của người dùng hiện tại
    document.getElementById('user-display-email').innerText = localStorage.getItem('userEmail') || 'zentask@gmail.com';

    // Đăng ký sự kiện nút Đăng xuất
    document.getElementById('btn-logout').addEventListener('click', handleLogout);

    // Tải danh sách công việc ban đầu
    fetchTasks();

    // Thiết lập sự kiện lắng nghe
    initEventListeners();

    // Render icons ban đầu
    lucide.createIcons();
});

// ==========================================================================
// API Interaction Functions
// ==========================================================================

/**
 * Fetch tasks from Backend API based on current filters and page
 */
async function fetchTasks() {
    toggleLoader(true);
    
    try {
        // Xây dựng các tham số truy vấn
        const queryParams = new URLSearchParams({
            page: currentPage,
            size: PAGE_SIZE,
            sortBy: filters.sortBy,
            sortDir: filters.sortDir
        });

        if (filters.search.trim() !== '') {
            queryParams.append('search', filters.search.trim());
        }
        if (filters.completed !== 'all') {
            queryParams.append('completed', filters.completed);
        }

        const response = await authenticatedFetch(`${API_BASE_URL}?${queryParams.toString()}`);
        
        if (!response.ok) {
            throw new Error('Không thể tải danh sách công việc từ máy chủ.');
        }

        const data = await response.json();
        tasks = data.content || [];
        totalPages = data.totalPages || 1;
        
        // Điều chỉnh trang hiện tại nếu bị vượt quá tổng số trang sau khi lọc
        if (currentPage >= totalPages && totalPages > 0) {
            currentPage = totalPages - 1;
            fetchTasks();
            return;
        }

        // Bổ sung thông tin độ ưu tiên lọc client-side nếu API chỉ lọc search & completed
        if (filters.priority !== 'all') {
            tasks = tasks.filter(t => t.priority === filters.priority);
        }

        renderTasks();
        updatePaginationUI();
        updateStats();

    } catch (error) {
        console.error('Fetch error:', error);
        showToast(error.message || 'Lỗi kết nối đến Backend API.', 'error');
        renderEmptyState(true);
    } finally {
        toggleLoader(false);
    }
}

/**
 * Toggle completion status of a task via PATCH API
 */
async function toggleTaskCompletionAPI(id) {
    try {
        const response = await authenticatedFetch(`${API_BASE_URL}/${id}/toggle`, {
            method: 'PATCH'
        });

        if (!response.ok) {
            throw new Error('Lỗi khi cập nhật trạng thái công việc.');
        }

        const updatedTask = await response.json();
        
        // Cập nhật mảng tasks local để render lại nhanh hơn
        tasks = tasks.map(t => t.id === id ? updatedTask : t);
        renderTasks();
        updateStats();
        
        const statusMsg = updatedTask.completed ? 'Đã hoàn thành' : 'Đã mở lại';
        showToast(`"${updatedTask.title}" - ${statusMsg}`, 'success');

    } catch (error) {
        console.error('Toggle error:', error);
        showToast(error.message, 'error');
    }
}

/**
 * Create or Update a task via POST/PUT API
 */
async function saveTaskAPI(taskData, id = null) {
    const isEdit = id !== null;
    const url = isEdit ? `${API_BASE_URL}/${id}` : API_BASE_URL;
    const method = isEdit ? 'PUT' : 'POST';

    // Bật trạng thái loading trên nút submit
    setSubmitBtnLoading(true);
    clearFormErrors();

    try {
        const response = await authenticatedFetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(taskData)
        });

        if (response.ok) {
            const savedTask = await response.json();
            closeModal();
            fetchTasks();
            showToast(isEdit ? 'Cập nhật công việc thành công!' : 'Tạo công việc mới thành công!', 'success');
        } else if (response.status === 400) {
            // Lỗi Validation từ Backend
            const errorData = await response.json();
            if (errorData.details) {
                displayBackendValidationErrors(errorData.details);
                showToast('Dữ liệu không hợp lệ. Vui lòng kiểm tra lại!', 'error');
            } else {
                showToast(errorData.message || 'Lỗi kiểm tra dữ liệu đầu vào.', 'error');
            }
        } else {
            const errorData = await response.json();
            throw new Error(errorData.message || 'Gặp lỗi trong quá trình lưu dữ liệu.');
        }

    } catch (error) {
        console.error('Save error:', error);
        showToast(error.message, 'error');
    } finally {
        setSubmitBtnLoading(false);
    }
}

/**
 * Delete a task via DELETE API
 */
async function deleteTaskAPI(id) {
    try {
        const response = await authenticatedFetch(`${API_BASE_URL}/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error('Lỗi khi xóa công việc khỏi hệ thống.');
        }

        // Tải lại danh sách
        fetchTasks();
        showToast('Xóa công việc thành công!', 'success');

    } catch (error) {
        console.error('Delete error:', error);
        showToast(error.message, 'error');
    }
}

// ==========================================================================
// Rendering & UI Functions
// ==========================================================================

/**
 * Render task cards into the grid
 */
function renderTasks() {
    tasksGrid.innerHTML = '';
    
    if (tasks.length === 0) {
        renderEmptyState(true);
        return;
    }

    renderEmptyState(false);

    tasks.forEach(task => {
        const card = document.createElement('div');
        card.className = `task-card priority-${task.priority.toLowerCase()} ${task.completed ? 'completed' : ''}`;
        card.setAttribute('data-id', task.id);
        
        // Format ngày tháng hiển thị
        const isDateOverdue = isOverdue(task.dueDate, task.completed);
        const formattedDateBadge = task.dueDate 
            ? `<div class="badge badge-date ${isDateOverdue ? 'overdue' : ''}">
                 <i data-lucide="${isDateOverdue ? 'alert-circle' : 'calendar'}"></i>
                 <span>${isDateOverdue ? 'Quá hạn: ' : 'Hạn chót: '}${formatVietnameseDate(task.dueDate)}</span>
               </div>`
            : '';

        // Priority Badge
        const priorityLabels = { HIGH: 'Cao', MEDIUM: 'Vừa', LOW: 'Thấp' };
        const priorityBadge = `<div class="badge badge-${task.priority.toLowerCase()}">
            <i data-lucide="circle-dot"></i>
            <span>Ưu tiên ${priorityLabels[task.priority]}</span>
        </div>`;

        card.innerHTML = `
            <!-- Checkbox -->
            <button class="task-checkbox" aria-label="Đánh dấu hoàn thành" title="Đánh dấu hoàn thành">
                <i data-lucide="check"></i>
            </button>

            <!-- Content Info -->
            <div class="task-info">
                <div class="task-title-row">
                    <h3 class="task-title">${escapeHTML(task.title)}</h3>
                </div>
                ${task.description ? `<p class="task-description">${escapeHTML(task.description)}</p>` : ''}
                <div class="badge-group">
                    ${priorityBadge}
                    ${formattedDateBadge}
                </div>
            </div>

            <!-- Actions buttons -->
            <div class="task-actions">
                <button class="icon-btn btn-edit" title="Chỉnh sửa công việc" aria-label="Sửa công việc">
                    <i data-lucide="pencil"></i>
                </button>
                <button class="icon-btn btn-delete" title="Xóa công việc" aria-label="Xóa công việc">
                    <i data-lucide="trash-2"></i>
                </button>
            </div>
        `;

        // Gắn sự kiện click cho các thành phần con trong card
        // 1. Toggle Checkbox
        card.querySelector('.task-checkbox').addEventListener('click', (e) => {
            e.stopPropagation();
            toggleTaskCompletionAPI(task.id);
        });

        // 2. Button Edit
        card.querySelector('.btn-edit').addEventListener('click', (e) => {
            e.stopPropagation();
            openEditModal(task);
        });

        // 3. Button Delete
        card.querySelector('.btn-delete').addEventListener('click', (e) => {
            e.stopPropagation();
            openDeleteConfirmation(task);
        });

        tasksGrid.appendChild(card);
    });

    // Render lại icons vector Lucide
    lucide.createIcons();
}

/**
 * Toggle loaders and content display
 */
function toggleLoader(isLoading) {
    if (isLoading) {
        shimmerLoader.style.display = 'block';
        tasksGrid.style.display = 'none';
        emptyState.style.display = 'none';
    } else {
        shimmerLoader.style.display = 'none';
        tasksGrid.style.display = 'grid';
    }
}

/**
 * Render empty state illustration
 */
function renderEmptyState(show) {
    if (show) {
        emptyState.style.display = 'flex';
        tasksGrid.style.display = 'none';
        paginationBar.style.display = 'none';
    } else {
        emptyState.style.display = 'none';
        paginationBar.style.display = 'flex';
    }
}

/**
 * Update pagination footer UI states
 */
function updatePaginationUI() {
    pageIndicator.textContent = `Trang ${currentPage + 1} / ${totalPages > 0 ? totalPages : 1}`;
    btnPrevPage.disabled = currentPage === 0;
    btnNextPage.disabled = currentPage >= totalPages - 1;
}

/**
 * Calculate and update Header Progress Bar statistics
 */
async function updateStats() {
    try {
        const response = await authenticatedFetch(`${API_BASE_URL}?size=2000`);
        if (!response.ok) return;

        const data = await response.json();
        const allTasks = data.content || [];
        const total = allTasks.length;
        const completed = allTasks.filter(t => t.completed).length;

        statsText.textContent = `${completed} / ${total} hoàn thành`;
        const percentage = total > 0 ? (completed / total) * 100 : 0;
        progressBar.style.width = `${percentage}%`;

    } catch (err) {
        console.error('Stats load error:', err);
    }
}

// ==========================================================================
// Event Listeners & Interaction Handling
// ==========================================================================

function initEventListeners() {
    // 1. Ô tìm kiếm với Debounce (tránh spam API)
    searchInput.addEventListener('input', () => {
        clearTimeout(searchDebounceTimer);
        searchDebounceTimer = setTimeout(() => {
            filters.search = searchInput.value;
            currentPage = 0; // Reset về trang đầu khi tìm kiếm
            fetchTasks();
        }, 350); // 350ms trì hoãn
    });

    // 2. Bộ lọc Trạng thái (Tất cả, Chưa làm, Đã xong)
    statusFilterGroup.addEventListener('click', (e) => {
        const target = e.target.closest('.pill');
        if (!target) return;

        statusFilterGroup.querySelectorAll('.pill').forEach(btn => btn.classList.remove('active'));
        target.classList.add('active');

        filters.completed = target.getAttribute('data-value');
        currentPage = 0;
        fetchTasks();
    });

    // 3. Bộ lọc Độ ưu tiên (Tất cả, Cao, Vừa, Thấp)
    priorityFilterGroup.addEventListener('click', (e) => {
        const target = e.target.closest('.pill');
        if (!target) return;

        priorityFilterGroup.querySelectorAll('.pill').forEach(btn => btn.classList.remove('active'));
        target.classList.add('active');

        filters.priority = target.getAttribute('data-value');
        currentPage = 0;
        fetchTasks();
    });

    // 4. Sắp xếp
    sortBySelect.addEventListener('change', () => {
        filters.sortBy = sortBySelect.value;
        currentPage = 0;
        fetchTasks();
    });

    // 5. Đảo chiều sắp xếp
    sortDirBtn.addEventListener('click', () => {
        if (filters.sortDir === 'desc') {
            filters.sortDir = 'asc';
            sortDirIcon.setAttribute('data-lucide', 'arrow-up-wide-narrow');
        } else {
            filters.sortDir = 'desc';
            sortDirIcon.setAttribute('data-lucide', 'arrow-down-narrow-wide');
        }
        currentPage = 0;
        fetchTasks();
    });

    // 6. Phân trang
    btnPrevPage.addEventListener('click', () => {
        if (currentPage > 0) {
            currentPage--;
            fetchTasks();
        }
    });

    // 7. Phân trang trang sau
    btnNextPage.addEventListener('click', () => {
        if (currentPage < totalPages - 1) {
            currentPage++;
            fetchTasks();
        }
    });

    // 8. Mở Modal thêm mới
    document.getElementById('btn-open-create').addEventListener('click', () => openCreateModal());
    document.getElementById('btn-empty-create').addEventListener('click', () => openCreateModal());

    // 9. Đóng Modal
    document.getElementById('btn-close-modal').addEventListener('click', closeModal);
    document.getElementById('btn-cancel-modal').addEventListener('click', closeModal);
    
    // Đóng modal khi click ra ngoài vùng panel
    taskModal.addEventListener('click', (e) => {
        if (e.target === taskModal) closeModal();
    });

    // 10. Submit Form
    taskForm.addEventListener('submit', (e) => {
        e.preventDefault();
        submitForm();
    });

    // 11. Confirm Delete Modal Actions
    document.getElementById('btn-cancel-delete').addEventListener('click', closeDeleteModal);
    btnConfirmDelete.addEventListener('click', () => {
        if (selectedTaskIdForDelete) {
            deleteTaskAPI(selectedTaskIdForDelete);
            closeDeleteModal();
        }
    });
    confirmModal.addEventListener('click', (e) => {
        if (e.target === confirmModal) closeDeleteModal();
    });
}

// ==========================================================================
// Modal Form Handlers (Create / Edit / Delete)
// ==========================================================================

/**
 * Open Modal in Create mode
 */
function openCreateModal() {
    modalTitle.textContent = 'Thêm công việc mới';
    taskForm.reset();
    taskIdInput.value = '';
    
    // Ẩn checkbox hoàn thành ở chế độ thêm mới (mặc định chưa hoàn thành)
    completedGroup.style.display = 'none';
    
    clearFormErrors();
    openOverlay(taskModal);
}

/**
 * Open Modal in Edit mode (fills existing task data)
 */
function openEditModal(task) {
    modalTitle.textContent = 'Chỉnh sửa công việc';
    clearFormErrors();

    // Điền dữ liệu vào form
    taskIdInput.value = task.id;
    inputTitle.value = task.title;
    inputDescription.value = task.description || '';
    
    // Chọn đúng radio của Priority
    const priorityRadios = taskForm.querySelectorAll('input[name="priority"]');
    priorityRadios.forEach(radio => {
        radio.checked = radio.value === task.priority;
    });

    // Format ngày giờ để tương thích với input datetime-local (yyyy-MM-ddThh:mm)
    if (task.dueDate) {
        const localDateStr = task.dueDate.substring(0, 16);
        inputDueDate.value = localDateStr;
    } else {
        inputDueDate.value = '';
    }

    // Hiển thị checkbox hoàn thành cho chế độ sửa
    completedGroup.style.display = 'block';
    inputCompleted.checked = task.completed;

    openOverlay(taskModal);
}

/**
 * Open Confirm Delete Popup
 */
function openDeleteConfirmation(task) {
    selectedTaskIdForDelete = task.id;
    deleteTaskTitleSpan.textContent = task.title;
    openOverlay(confirmModal);
}

function closeModal() {
    closeOverlay(taskModal);
}

function closeDeleteModal() {
    closeOverlay(confirmModal);
    selectedTaskIdForDelete = null;
}

function openOverlay(element) {
    element.classList.add('active');
    element.setAttribute('aria-hidden', 'false');
    document.body.style.overflow = 'hidden'; // Ngăn chặn scroll body ở phía dưới
}

function closeOverlay(element) {
    element.classList.remove('active');
    element.setAttribute('aria-hidden', 'true');
    document.body.style.overflow = '';
}

/**
 * Validate form and submit task
 */
function submitForm() {
    const titleVal = inputTitle.value.trim();
    
    // Validation phía Client: Kiểm tra tiêu đề rỗng
    if (titleVal === '') {
        const group = inputTitle.closest('.form-group');
        group.classList.add('has-error');
        errorTitle.textContent = 'Tiêu đề không được để trống';
        inputTitle.focus();
        return;
    }

    const id = taskIdInput.value ? parseInt(taskIdInput.value) : null;
    
    // Thu thập dữ liệu
    const selectedPriority = taskForm.querySelector('input[name="priority"]:checked').value;
    const dueDateVal = inputDueDate.value !== '' ? `${inputDueDate.value}:00` : null; // Thêm giây để khớp ISO DateTime

    const taskData = {
        title: titleVal,
        description: inputDescription.value.trim() || null,
        priority: selectedPriority,
        dueDate: dueDateVal
    };

    if (id !== null) {
        taskData.completed = inputCompleted.checked;
    }

    saveTaskAPI(taskData, id);
}

/**
 * Reset form errors visual feedback
 */
function clearFormErrors() {
    const errorGroups = taskForm.querySelectorAll('.form-group');
    errorGroups.forEach(g => g.classList.remove('has-error'));
}

/**
 * Map error details returned from backend validator onto form fields
 */
function displayBackendValidationErrors(details) {
    if (details.title) {
        const group = inputTitle.closest('.form-group');
        group.classList.add('has-error');
        errorTitle.textContent = details.title;
        inputTitle.focus();
    }
}

function setSubmitBtnLoading(isLoading) {
    if (isLoading) {
        btnSubmitForm.disabled = true;
        btnSubmitSpinner.style.display = 'block';
        btnSubmitText.style.display = 'none';
    } else {
        btnSubmitForm.disabled = false;
        btnSubmitSpinner.style.display = 'none';
        btnSubmitText.style.display = 'block';
    }
}

// ==========================================================================
// Helper Utility Functions
// ==========================================================================

/**
 * Show a toast notification on the top right corner
 */
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    let iconName = 'info';
    if (type === 'success') iconName = 'check-circle';
    if (type === 'error') iconName = 'alert-triangle';

    toast.innerHTML = `
        <i data-lucide="${iconName}"></i>
        <div class="toast-msg">${escapeHTML(message)}</div>
        <button class="toast-close" aria-label="Đóng thông báo">
            <i data-lucide="x"></i>
        </button>
    `;

    container.appendChild(toast);
    lucide.createIcons();

    // Xử lý tự đóng sau 3 giây
    const autoCloseTimer = setTimeout(() => {
        removeToast(toast);
    }, 3000);

    // Xử lý đóng thủ công khi click nút X
    toast.querySelector('.toast-close').addEventListener('click', () => {
        clearTimeout(autoCloseTimer);
        removeToast(toast);
    });
}

function removeToast(toast) {
    toast.classList.add('toast-out');
    toast.addEventListener('animationend', (e) => {
        if (e.animationName === 'toast-out') {
            toast.remove();
        }
    });
}

/**
 * Format string ISO LocalDateTime (yyyy-MM-ddTHH:mm:ss) into Vietnamese locale
 */
function formatVietnameseDate(isoString) {
    if (!isoString) return '';
    try {
        const date = new Date(isoString);
        if (isNaN(date)) return '';
        
        const pad = (num) => String(num).padStart(2, '0');
        const hours = pad(date.getHours());
        const minutes = pad(date.getMinutes());
        const day = pad(date.getDate());
        const month = pad(date.getMonth() + 1);
        const year = date.getFullYear();

        return `${hours}:${minutes}, ${day}/${month}/${year}`;
    } catch (e) {
        return '';
    }
}

/**
 * Check if the task is overdue (overdueDate < now and incomplete)
 */
function isOverdue(dueDateString, isCompleted) {
    if (!dueDateString || isCompleted) return false;
    try {
        const dueDate = new Date(dueDateString);
        return dueDate < new Date();
    } catch (e) {
        return false;
    }
}

/**
 * Escapes HTML input strings to prevent XSS attacks
 */
function escapeHTML(str) {
    if (!str) return '';
    return str.replace(/[&<>'"]/g, 
        tag => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[tag] || tag)
    );
}
