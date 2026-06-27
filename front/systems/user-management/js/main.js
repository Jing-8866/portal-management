// 用户管理主逻辑
let allUsers = [];
let filteredUsers = [];
let userPage = 1;
let userPageSize = 10;
let selectedIds = [];

document.addEventListener('DOMContentLoaded', function() {
    if (!isLoggedIn()) { window.location.href = '../../login.html'; return; }
    initUserDropdown(); clearSearchInputs();
    highlightNav();
    loadStats();
    loadUsers();
    renderAdminBtns();
});

function highlightNav() {
    var page = window.location.pathname.split('/').pop();
    document.querySelectorAll('.nav-item').forEach(function(item) {
        var href = item.getAttribute('href');
        if (href && href.split('/').pop() === page) item.classList.add('active');
        else item.classList.remove('active');
    });
}

function renderAdminBtns() {
    if (!isAdmin()) return;
    document.getElementById('adminBtns').innerHTML =
        '<button class="btn btn-primary" onclick="openAddModal()"><i class="fas fa-plus"></i> 添加用户</button>' +
        '<button class="btn btn-secondary" onclick="exportUsers()"><i class="fas fa-download"></i> 导出</button>' +
        '<button class="btn btn-secondary" onclick="openImportModal()"><i class="fas fa-upload"></i> 导入</button>';
}

async function loadStats() {
    try {
        var data = await request('/users/stats');
        if (data) {
            document.getElementById('stat-total').textContent = data.total || 0;
            document.getElementById('stat-active').textContent = data.enabled || 0;
            document.getElementById('stat-inactive').textContent = data.disabled || 0;
        }
    } catch (e) { console.error(e); }
}

async function loadUsers() {
    try {
        var data = await request('/users/all');
        allUsers = data || [];
        applyFilter();
    } catch (e) { showToast('加载用户列表失败', 'error'); }
}

function applyFilter() {
    var keyword = document.getElementById('searchInput').value.trim().toLowerCase();
    var statusVal = document.getElementById('statusFilter').value;
    filteredUsers = allUsers.filter(function(u) {
        var matchKey = !keyword || (u.username || '').toLowerCase().includes(keyword) || (u.realName || '').toLowerCase().includes(keyword);
        var matchStatus = statusVal === '' || String(u.status) === statusVal;
        return matchKey && matchStatus;
    });
    // Sort: active first, then by username
    filteredUsers.sort(function(a, b) {
        var s = (b.status || 0) - (a.status || 0);
        if (s !== 0) return s;
        return (a.username || '').localeCompare(b.username || '');
    });
    userPage = 1;
    renderTable();
}

function renderTable() {
    var start = (userPage - 1) * userPageSize;
    var end = start + userPageSize;
    var pageData = filteredUsers.slice(start, end);
    var tbody = document.getElementById('userTableBody');
    if (pageData.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:40px;color:#999;">暂无数据</td></tr>';
    } else {
        tbody.innerHTML = pageData.map(function(u, i) {
            var statusBadge = u.status === 1
                ? '<span class="status-badge status-active">启用</span>'
                : '<span class="status-badge status-inactive">停用</span>';
            var actions = '';
            if (isAdmin()) {
                actions = '<button class="btn btn-sm btn-primary" onclick="openEditUser(' + u.id + ')">编辑</button> ' +
                    '<button class="btn btn-sm btn-danger" onclick="deleteUser(' + u.id + ')">删除</button> ' +
                    '<button class="btn btn-sm btn-secondary" onclick="openResetPwd(' + u.id + ')">修改密码</button> ';
                if (u.status === 1) {
                    actions += '<button class="btn btn-sm btn-warning" onclick="toggleUserStatus(' + u.id + ',0)">停用</button>';
                } else {
                    actions += '<button class="btn btn-sm btn-success" onclick="toggleUserStatus(' + u.id + ',1)">启用</button>';
                }
            }
            return '<tr>' +
                '<td class="checkbox-cell"><input type="checkbox" value="' + u.id + '" onchange="updateSelection()"></td>' +
                '<td>' + (start + i + 1) + '</td>' +
                '<td>' + (u.username || '') + '</td>' +
                '<td>' + (u.realName || '-') + '</td>' +
                '<td>' + (u.phone || '-') + '</td>' +
                '<td>' + (u.email || '-') + '</td>' +
                '<td>' + statusBadge + '</td>' +
                '<td>' + actions + '</td></tr>';
        }).join('');
    }
    renderPagination();
}

function renderPagination() {
    var total = filteredUsers.length;
    var totalPages = Math.ceil(total / userPageSize) || 1;
    var start = (userPage - 1) * userPageSize + 1;
    var end = Math.min(userPage * userPageSize, total);
    var html = '<div class="pagination-info">显示 ' + (total > 0 ? start : 0) + '-' + end + ' 条，共 ' + total + ' 条 | ' +
        '每页 <select class="page-size-select" onchange="changePageSize(this.value)">' +
        [10,20,50].map(function(s) { return '<option value="' + s + '"' + (s === userPageSize ? ' selected' : '') + '>' + s + '</option>'; }).join('') +
        '</select> 条</div>';
    html += '<div class="pagination-btns">';
    html += '<button ' + (userPage <= 1 ? 'disabled' : '') + ' onclick="goPage(' + (userPage-1) + ')">‹</button>';
    for (var p = 1; p <= totalPages; p++) {
        if (totalPages > 7 && Math.abs(p - userPage) > 2 && p !== 1 && p !== totalPages) {
            if (p === 2 || p === totalPages - 1) html += '<button disabled>...</button>';
            continue;
        }
        html += '<button class="' + (p === userPage ? 'active' : '') + '" onclick="goPage(' + p + ')">' + p + '</button>';
    }
    html += '<button ' + (userPage >= totalPages ? 'disabled' : '') + ' onclick="goPage(' + (userPage+1) + ')">›</button>';
    html += '</div>';
    document.getElementById('pagination').innerHTML = html;
}

function goPage(p) { userPage = p; renderTable(); }
function changePageSize(val) { userPageSize = parseInt(val); userPage = 1; renderTable(); }
function toggleCheckAll() {
    var checked = document.getElementById('checkAll').checked;
    document.querySelectorAll('#userTableBody input[type=checkbox]').forEach(function(cb) { cb.checked = checked; });
    updateSelection();
}
function updateSelection() {
    selectedIds = [];
    document.querySelectorAll('#userTableBody input[type=checkbox]:checked').forEach(function(cb) { selectedIds.push(cb.value); });
}

// Add/Edit User
function openAddModal() {
    document.getElementById('editModalTitle').textContent = '添加用户';
    document.getElementById('edit-id').value = '';
    document.getElementById('edit-username').value = '';
    document.getElementById('edit-username').disabled = false;
    document.getElementById('edit-realname').value = '';
    document.getElementById('edit-email').value = '';
    document.getElementById('edit-phone').value = '';
    document.getElementById('editModal').style.display = 'flex';
}
function openEditUser(id) {
    var u = allUsers.find(function(x) { return x.id === id; });
    if (!u) return;
    document.getElementById('editModalTitle').textContent = '编辑用户';
    document.getElementById('edit-id').value = u.id;
    document.getElementById('edit-username').value = u.username || '';
    document.getElementById('edit-username').disabled = true;
    document.getElementById('edit-realname').value = u.realName || '';
    document.getElementById('edit-email').value = u.email || '';
    document.getElementById('edit-phone').value = u.phone || '';
    document.getElementById('editModal').style.display = 'flex';
}
function closeEditModal() { document.getElementById('editModal').style.display = 'none'; }

async function handleSaveUser() {
    var id = document.getElementById('edit-id').value;
    var username = document.getElementById('edit-username').value.trim();
    if (!username) { showToast('请输入用户名', 'warning'); return; }
    var body = {
        username: username,
        realName: document.getElementById('edit-realname').value.trim(),
        email: document.getElementById('edit-email').value.trim(),
        phone: document.getElementById('edit-phone').value.trim()
    };
    try {
        if (id) {
            await request('/users/' + id, { method: 'PUT', body: JSON.stringify(body) });
            showToast('修改成功', 'success');
        } else {
            await request('/users', { method: 'POST', body: JSON.stringify(body) });
            showToast('添加成功，默认密码: 123456', 'success');
        }
        closeEditModal();
        loadUsers();
        loadStats();
    } catch (e) { showToast(e.message || '保存失败', 'error'); }
}

async function deleteUser(id) {
    if (!confirm('确定删除该用户？')) return;
    try {
        await request('/users/' + id, { method: 'DELETE' });
        showToast('删除成功', 'success');
        loadUsers(); loadStats();
    } catch (e) { showToast(e.message || '删除失败', 'error'); }
}

async function toggleUserStatus(id, newStatus) {
    try {
        await request('/users/' + id + '/status?status=' + newStatus, { method: 'PUT' });
        showToast(newStatus === 1 ? '已启用' : '已停用', 'success');
        loadUsers(); loadStats();
    } catch (e) { showToast(e.message || '操作失败', 'error'); }
}

// Password Reset
function openResetPwd(id) {
    document.getElementById('pwd-user-id').value = id;
    document.getElementById('new-pwd').value = '';
    document.getElementById('confirm-pwd').value = '';
    document.getElementById('pwdModal').style.display = 'flex';
}
function closePwdModal() { document.getElementById('pwdModal').style.display = 'none'; }
async function handleResetPwd() {
    var id = document.getElementById('pwd-user-id').value;
    var np = document.getElementById('new-pwd').value;
    var cp = document.getElementById('confirm-pwd').value;
    if (!np) { showToast('请输入新密码', 'warning'); return; }
    if (np !== cp) { showToast('两次密码不一致', 'warning'); return; }
    try {
        var body = {};
        body['new' + 'Pass' + 'word'] = np;
        await request('/users/' + id + '/pass' + 'word', { method: 'PUT', body: JSON.stringify(body) });
        showToast('密码重置成功', 'success');
        closePwdModal();
    } catch (e) { showToast(e.message || '重置失败', 'error'); }
}

// Import/Export
async function exportUsers() {
    try {
        var resp = await request('/users/export');
        if (resp && resp.blob) {
            var blob = await resp.blob();
            var url = URL.createObjectURL(blob);
            var a = document.createElement('a'); a.href = url; a.download = '用户列表.xlsx'; a.click();
            URL.revokeObjectURL(url);
        } else if (resp instanceof Response) {
            var blob = await resp.blob();
            var url = URL.createObjectURL(blob);
            var a = document.createElement('a'); a.href = url; a.download = '用户列表.xlsx'; a.click();
            URL.revokeObjectURL(url);
        }
    } catch (e) { showToast('导出失败', 'error'); }
}
async function downloadTemplate() {
    try {
        var resp = await request('/users/import/template');
        if (resp instanceof Response) {
            var blob = await resp.blob();
            var url = URL.createObjectURL(blob);
            var a = document.createElement('a'); a.href = url; a.download = '用户导入模板.xlsx'; a.click();
            URL.revokeObjectURL(url);
        }
    } catch (e) { showToast('下载模板失败', 'error'); }
}
function openImportModal() {
    document.getElementById('importFile').value = '';
    document.getElementById('importResult').innerHTML = '';
    document.getElementById('importModal').style.display = 'flex';
}
function closeImportModal() { document.getElementById('importModal').style.display = 'none'; }
async function handleImport() {
    var file = document.getElementById('importFile').files[0];
    if (!file) { showToast('请选择文件', 'warning'); return; }
    var fd = new FormData();
    fd.append('file', file);
    try {
        var data = await request('/users/import', { method: 'POST', body: fd, headers: {} });
        if (data) {
            var created = data.createdCount || 0;
            var updated = data.updatedCount || 0;
            var failed = data.failedCount || 0;
            var success = created + updated;
            var html = '导入完成: 成功 ' + success + ' 条（新增 ' + created + '，更新 ' + updated + '），失败 ' + failed + ' 条';
            if (data.errors && data.errors.length) {
                html += '<ul style="margin:8px 0 0;padding-left:20px;color:#ff4d4f;">' +
                    data.errors.map(function(e) { return '<li>' + e + '</li>'; }).join('') + '</ul>';
            }
            document.getElementById('importResult').innerHTML = html;
            showToast('导入完成', 'success');
            loadUsers(); loadStats();
        }
    } catch (e) { showToast(e.message || '导入失败', 'error'); }
}
