// 门户首页逻辑
let allSystems = [];

document.addEventListener('DOMContentLoaded', function() {
    if (!isLoggedIn()) { window.location.replace('login.html'); return; }
    document.documentElement.classList.add('portal-ready');
    initUserDropdown(); clearSearchInputs();
    loadSubsystems();
});

async function loadSubsystems() {
    try {
        const data = await request('/subsystems/my');
        allSystems = data || [];
        renderCards(allSystems);
    } catch (e) {
        showToast('加载子系统失败', 'error');
    }
}

function renderCards(systems) {
    const grid = document.getElementById('systemsGrid');
    let html = '';
    systems.forEach(function(sys) {
        const disabled = sys.status === 0 ? 'disabled' : '';
        const actionsHtml = isPlatformAdmin() ? '<div class="card-actions">' +
            '<button class="btn-edit" title="编辑" onclick="event.stopPropagation();editSubsystem('+sys.id+')"><i class="fas fa-pen"></i></button>' +
            '<button class="btn-toggle" title="'+(sys.status===1?'停用':'启用')+'" onclick="event.stopPropagation();toggleSubsystemStatus('+sys.id+','+sys.status+')"><i class="fas fa-'+(sys.status===1?'ban':'check')+'"></i></button>' +
            '<button class="btn-del" title="删除" onclick="event.stopPropagation();deleteSubsystem('+sys.id+')"><i class="fas fa-trash"></i></button>' +
            '</div>' : '';
        html += '<div class="system-card ' + disabled + '" onclick="enterSystem('+sys.id+',\''+(sys.url||'')+'\','+sys.status+')">' +
            actionsHtml +
            '<div class="card-icon" style="background:'+(sys.color||'#4A90D9')+'"><i class="'+(sys.icon||'fas fa-cube')+'"></i></div>' +
            '<h3>' + (sys.systemName || sys.name || '') + '</h3>' +
            '<p>' + (sys.description || '暂无描述') + '</p></div>';
    });
    if (isPlatformAdmin()) {
        html += '<div class="system-card add-card" onclick="openAddModal()"><i class="fas fa-plus"></i><span>新增子系统</span></div>';
    }
    grid.innerHTML = html;
}

function enterSystem(id, url, status) {
    if (status === 0) { showToast('该系统已停用', 'warning'); return; }
    if (url) window.location.href = url; else showToast('未配置访问地址', 'warning');
}

function filterSystems() {
    const keyword = document.getElementById('searchInput').value.trim().toLowerCase();
    const filtered = allSystems.filter(function(s) {
        return (s.systemName||s.name||'').toLowerCase().includes(keyword) || (s.description || '').toLowerCase().includes(keyword);
    });
    renderCards(filtered);
}

function openAddModal() {
    document.getElementById('modalTitle').textContent = '新增子系统';
    document.getElementById('sys-id').value = '';
    document.getElementById('sys-name').value = '';
    document.getElementById('sys-code').value = '';
    document.getElementById('sys-desc').value = '';
    document.getElementById('sys-url').value = '';
    document.getElementById('sys-icon').value = '';
    document.getElementById('sys-color').value = '#4A90D9';
    document.getElementById('sys-sort').value = '0';
    document.getElementById('systemModal').style.display = 'flex';
}

function editSubsystem(id) {
    const sys = allSystems.find(function(s) { return s.id === id; });
    if (!sys) return;
    document.getElementById('modalTitle').textContent = '编辑子系统';
    document.getElementById('sys-id').value = sys.id;
    document.getElementById('sys-name').value = sys.systemName || '';
    document.getElementById('sys-code').value = sys.systemCode || '';
    document.getElementById('sys-desc').value = sys.description || '';
    document.getElementById('sys-url').value = sys.url || '';
    document.getElementById('sys-icon').value = sys.icon || '';
    document.getElementById('sys-color').value = sys.color || '#4A90D9';
    document.getElementById('sys-sort').value = sys.sortOrder || 0;
    document.getElementById('systemModal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('systemModal').style.display = 'none';
}

async function handleSaveSubsystem() {
    const id = document.getElementById('sys-id').value;
    const sysName = document.getElementById('sys-name').value.trim();
    const sysCode = document.getElementById('sys-code').value.trim();
    const url = document.getElementById('sys-url').value.trim();
    if (!sysName || !sysCode || !url) { showToast('请填写必填项', 'warning'); return; }
    const body = {
        systemName: sysName,
        systemCode: sysCode,
        description: document.getElementById('sys-desc').value.trim(),
        url: url,
        icon: document.getElementById('sys-icon').value.trim(),
        color: document.getElementById('sys-color').value,
        sortOrder: parseInt(document.getElementById('sys-sort').value) || 0
    };
    try {
        if (id) {
            await request('/subsystems/' + id, { method: 'PUT', body: JSON.stringify(body) });
            showToast('修改成功', 'success');
        } else {
            await request('/subsystems', { method: 'POST', body: JSON.stringify(body) });
            showToast('新增成功', 'success');
        }
        closeModal();
        loadSubsystems();
    } catch (e) {
        showToast(e.message || '保存失败', 'error');
    }
}

async function deleteSubsystem(id) {
    if (!confirm('确定删除该子系统？')) return;
    try {
        await request('/subsystems/' + id, { method: 'DELETE' });
        showToast('删除成功', 'success');
        loadSubsystems();
    } catch (e) {
        showToast(e.message || '删除失败', 'error');
    }
}

async function toggleSubsystemStatus(id, currentStatus) {
    const newStatus = currentStatus === 1 ? 0 : 1;
    const msg = newStatus === 0 ? '确定停用该子系统？' : '确定启用该子系统？';
    if (!confirm(msg)) return;
    try {
        await request('/subsystems/' + id + '/status?status=' + newStatus, { method: 'PUT' });
        showToast(newStatus === 1 ? '已启用' : '已停用', 'success');
        loadSubsystems();
    } catch (e) {
        showToast(e.message || '操作失败', 'error');
    }
}
