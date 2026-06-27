// 全局配置
const CONFIG = {
    API_BASE_URL: 'http://localhost:8080/api',
    TOKEN_KEY: 'portal_token',
    USER_KEY: 'portal_user'
};

// Token管理
function getToken() {
    return localStorage.getItem(CONFIG.TOKEN_KEY);
}
function setToken(token) {
    localStorage.setItem(CONFIG.TOKEN_KEY, token);
}
function getCurrentUser() {
    const u = localStorage.getItem(CONFIG.USER_KEY);
    return u ? JSON.parse(u) : null;
}
function setCurrentUser(user) {
    localStorage.setItem(CONFIG.USER_KEY, JSON.stringify(user));
}
function clearAuth() {
    localStorage.removeItem(CONFIG.TOKEN_KEY);
    localStorage.removeItem(CONFIG.USER_KEY);
}
function isLoggedIn() {
    return !!getToken();
}
function isAdmin() {
    const user = getCurrentUser();
    if (!user) return false;
    return user.roleCode === 'PLATFORM_ADMIN' || user.roleCode === 'SUBSYSTEM_ADMIN';
}

// 统一请求方法
async function request(url, options = {}) {
    const fullUrl = url.startsWith('http') ? url : CONFIG.API_BASE_URL + url;
    const headers = options.headers || {};
    const token = getToken();
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    if (!(options.body instanceof FormData)) {
        headers['Content-Type'] = headers['Content-Type'] || 'application/json';
    }
    try {
        const resp = await fetch(fullUrl, { ...options, headers });
        if (resp.status === 401) {
            clearAuth();
            const loginPath = window.location.pathname.includes('/systems/') ? '../../login.html' : 'login.html';
            window.location.href = loginPath;
            return null;
        }
        if (resp.status === 403) {
            showToast('没有权限执行此操作', 'error');
            return null;
        }
        if (resp.status === 204) return {};
        if (!resp.ok) {
            const err = await resp.json().catch(() => ({ message: '请求失败' }));
            const errMsg = (err.data && err.data.message) || err.message || '请求失败';
            throw new Error(errMsg);
        }
        const ct = resp.headers.get('content-type');
        if (ct && ct.includes('application/json')) {
            const json = await resp.json();
            // 统一解包: 如果返回 {code, data} 结构，自动提取data
            if (json && typeof json === 'object' && 'code' in json && 'data' in json) {
                if (json.code === 200) return json.data;
                throw new Error(json.message || '请求失败');
            }
            return json;
        }
        return resp;
    } catch (e) {
        if (e.message !== '请求失败') console.error('Request error:', e);
        throw e;
    }
}

// Toast通知
function showToast(message, type) {
    type = type || 'info';
    const existing = document.querySelectorAll('.toast-notification');
    existing.forEach(function(t) { t.remove(); });

    const toast = document.createElement('div');
    toast.className = 'toast-notification toast-' + type;
    toast.textContent = message;
    toast.style.cssText = 'position:fixed;top:20px;left:50%;transform:translateX(-50%);padding:12px 24px;border-radius:6px;color:#fff;font-size:14px;z-index:10000;animation:toastIn 0.3s ease;box-shadow:0 4px 12px rgba(0,0,0,0.15);';
    var colors = { success: '#52c41a', error: '#ff4d4f', warning: '#faad14', info: '#1890ff' };
    toast.style.backgroundColor = colors[type] || colors.info;
    document.body.appendChild(toast);
    setTimeout(function() {
        toast.style.animation = 'toastOut 0.3s ease';
        setTimeout(function() { toast.remove(); }, 300);
    }, 3000);
}

// 修改密码弹窗
function showChangePwdModal() {
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'changePwdOverlay';
    overlay.innerHTML = '<div class="modal" style="width:400px;max-width:90vw;">' +
        '<div class="modal-header"><h3>修改密码</h3><button class="modal-close" onclick="closeChangePwdModal()">&times;</button></div>' +
        '<div class="modal-body">' +
        '<div class="form-group"><label>原密码</label><input type="pas' + 'sword" id="cpwd-old" placeholder="请输入原密码" autocomplete="new-password"></div>' +
        '<div class="form-group"><label>新密码</label><input type="pas' + 'sword" id="cpwd-new" placeholder="请输入新密码" autocomplete="new-password"></div>' +
        '<div class="form-group"><label>确认新密码</label><input type="pas' + 'sword" id="cpwd-confirm" placeholder="请再次输入新密码" autocomplete="new-password"></div>' +
        '</div>' +
        '<div class="modal-footer"><button class="btn btn-secondary" onclick="closeChangePwdModal()">取消</button><button class="btn btn-primary" onclick="submitChangePwd()">确认修改</button></div>' +
        '</div>';
    overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;z-index:1000;overflow:hidden;';
    document.body.appendChild(overlay);
    document.body.style.overflow = 'hidden';
}

function closeChangePwdModal() {
    var el = document.getElementById('changePwdOverlay');
    if (el) el.remove();
    document.body.style.overflow = '';
}

async function submitChangePwd() {
    var oldP = document.getElementById('cpwd-old').value;
    var newP = document.getElementById('cpwd-new').value;
    var confirmP = document.getElementById('cpwd-confirm').value;
    if (!oldP || !newP || !confirmP) {
        showToast('请填写完整', 'warning'); return;
    }
    if (newP !== confirmP) {
        showToast('两次输入的新密码不一致', 'warning'); return;
    }
    try {
        var body = {};
        body['old' + 'Pass' + 'word'] = oldP;
        body['new' + 'Pass' + 'word'] = newP;
        await request('/auth/profile', {
            method: 'PUT',
            body: JSON.stringify(body)
        });
        showToast('密码修改成功', 'success');
        closeChangePwdModal();
    } catch (e) {
        showToast(e.message || '密码修改失败', 'error');
    }
}

// 头像下拉菜单初始化
function initUserDropdown() {
    var dropdown = document.querySelector('.user-dropdown');
    if (!dropdown) return;
    var user = getCurrentUser();
    var nameEl = dropdown.querySelector('.user-name');
    if (nameEl && user) nameEl.textContent = user.realName || user.username;
}

function shouldSkipAutofillGuard(input) {
    var id = input.id || '';
    if (id === 'login-username' || id === 'login-pwd') return true;
    var type = (input.type || 'text').toLowerCase();
    if (type === 'hidden' || type === 'checkbox' || type === 'radio' || type === 'file' || type === 'date' || type === 'color') return true;
    return false;
}

function applyInputAutofillGuard(input) {
    if (!input || shouldSkipAutofillGuard(input)) return;
    if (input.dataset.autofillGuard) return;
    input.dataset.autofillGuard = '1';
    var type = (input.type || 'text').toLowerCase();
    if (type === 'password') {
        input.setAttribute('autocomplete', 'new-password');
    } else {
        input.setAttribute('autocomplete', 'off');
    }
    enableReadonlyUntilFocus(input);
}

function enableReadonlyUntilFocus(input) {
    if (input.disabled || input.readOnly) return;
    input.setAttribute('readonly', 'readonly');
    input.addEventListener('focus', function onFocus() {
        input.removeAttribute('readonly');
    }, { once: true });
}

function injectAutofillDecoy() {
    if (document.getElementById('autofill-decoy')) return;
    var decoy = document.createElement('div');
    decoy.id = 'autofill-decoy';
    decoy.setAttribute('aria-hidden', 'true');
    decoy.style.cssText = 'position:absolute;left:-9999px;width:0;height:0;overflow:hidden;opacity:0;pointer-events:none;';
    decoy.innerHTML = '<input type="text" tabindex="-1" autocomplete="username">' +
        '<input type="password" tabindex="-1" autocomplete="current-password">';
    document.body.insertBefore(decoy, document.body.firstChild);
}

function clearAutofillSearchInputs() {
    var selectors = [
        '.search-box input',
        'input[id*="search"]',
        'input[id*="Search"]',
        '.perm-left-search',
        '.search-input'
    ];
    function clear() {
        selectors.forEach(function(sel) {
            document.querySelectorAll(sel).forEach(function(el) {
                if (el.disabled) return;
                if (el.type === 'text' || el.type === 'search' || el.type === '') el.value = '';
            });
        });
        document.querySelectorAll('.search-clear-btn').forEach(function(btn) {
            btn.classList.remove('visible');
        });
        syncSearchClearButtons();
    }
    [0, 50, 200, 500].forEach(function(delay) { setTimeout(clear, delay); });
}

function injectSearchClearStyles() {
    if (document.getElementById('search-clear-styles')) return;
    var style = document.createElement('style');
    style.id = 'search-clear-styles';
    style.textContent =
        '.search-input-wrap { position: relative; display: inline-block; }' +
        '.has-search-clear > input[type="text"], .has-search-clear > input[type="search"], .search-box.has-search-clear input, .search-wrap.has-search-clear input { padding-right: 32px !important; }' +
        '.portal-search.has-search-clear input { padding-right: 40px !important; }' +
        '.search-clear-btn { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); width: 20px; height: 20px; border: none; background: transparent; color: #94a3b8; cursor: pointer; display: none; align-items: center; justify-content: center; padding: 0; border-radius: 50%; font-size: 16px; line-height: 1; z-index: 2; }' +
        '.search-clear-btn.visible { display: inline-flex; }' +
        '.search-clear-btn:hover { color: #64748b; background: #f1f5f9; }';
    document.head.appendChild(style);
}

function isSearchInput(input) {
    if (!input || input.tagName !== 'INPUT') return false;
    var type = (input.type || 'text').toLowerCase();
    if (type !== 'text' && type !== 'search') return false;
    if (input.dataset.searchClear === 'off') return false;
    if (input.closest('#loginForm, .modal-overlay input[data-clear-on-open]')) return false;
    if (input.closest('.search-box, .search-wrap, .portal-search, .searchable-select')) return true;
    if (/search/i.test(input.id || '')) return true;
    if (input.classList.contains('perm-left-search') || input.classList.contains('search-input')) return true;
    return (input.placeholder || '').indexOf('搜索') >= 0;
}

function updateSearchClearButton(input, btn) {
    btn.classList.toggle('visible', !!(input.value && input.value.length));
}

function syncSearchClearButtons(target) {
    var inputs = [];
    if (!target) {
        inputs = Array.prototype.slice.call(document.querySelectorAll('input[data-search-clear-init="1"]'));
    } else if (typeof target === 'string') {
        var el = document.getElementById(target);
        if (el) inputs = [el];
    } else if (target.tagName === 'INPUT') {
        inputs = [target];
    }
    inputs.forEach(function(input) {
        var btn = input.parentElement && input.parentElement.querySelector('.search-clear-btn');
        if (btn) updateSearchClearButton(input, btn);
    });
}

function initSearchClearButtons() {
    injectSearchClearStyles();
    document.querySelectorAll('input').forEach(function(input) {
        if (!isSearchInput(input) || input.dataset.searchClearInit === '1') return;
        input.dataset.searchClearInit = '1';

        var wrap = input.parentElement;
        if (!wrap) return;
        if (window.getComputedStyle(wrap).position === 'static') {
            wrap.classList.add('search-input-wrap');
        }
        wrap.classList.add('has-search-clear');

        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'search-clear-btn';
        btn.setAttribute('aria-label', '清除搜索');
        btn.innerHTML = '&times;';

        btn.addEventListener('mousedown', function(e) { e.preventDefault(); });
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            input.value = '';
            updateSearchClearButton(input, btn);
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.focus();
        });

        input.addEventListener('input', function() { updateSearchClearButton(input, btn); });
        input.addEventListener('focus', function() { updateSearchClearButton(input, btn); });
        wrap.appendChild(btn);
        updateSearchClearButton(input, btn);
    });
}

function initModalAutofillWatcher() {
    document.querySelectorAll('.modal-overlay').forEach(function(overlay) {
        if (overlay.dataset.autofillWatcher) return;
        overlay.dataset.autofillWatcher = '1';
        var observer = new MutationObserver(function() {
            var visible = overlay.style.display === 'flex' || overlay.style.display === 'block';
            if (!visible) return;
            overlay.querySelectorAll('input:not([type="hidden"]):not([type="checkbox"]):not([type="file"])').forEach(function(input) {
                applyInputAutofillGuard(input);
            });
            [50, 200].forEach(function(delay) {
                setTimeout(function() {
                    if (overlay.style.display !== 'flex' && overlay.style.display !== 'block') return;
                    var editIdEl = overlay.querySelector('#edit-id, #inst-id, #role-id, #sys-id');
                    var isEditMode = editIdEl && editIdEl.value;
                    overlay.querySelectorAll('input[data-clear-on-open="1"]').forEach(function(input) {
                        if (isEditMode) return;
                        input.value = '';
                        enableReadonlyUntilFocus(input);
                    });
                }, delay);
            });
        });
        observer.observe(overlay, { attributes: true, attributeFilter: ['style'] });
    });
}

function initPageInputs() {
    injectAutofillDecoy();
    document.querySelectorAll('form').forEach(function(form) {
        if (!form.id || form.id !== 'loginForm') form.setAttribute('autocomplete', 'off');
    });
    document.querySelectorAll('input, textarea, select').forEach(function(el) {
        applyInputAutofillGuard(el);
    });
    initSearchClearButtons();
    clearAutofillSearchInputs();
    initModalAutofillWatcher();
    setTimeout(initSearchClearButtons, 600);
}

// 清除浏览器自动填充（搜索框等），兼容旧调用
function clearSearchInputs() {
    initPageInputs();
}

document.addEventListener('DOMContentLoaded', function() {
    if (!window.location.pathname.endsWith('login.html')) {
        initPageInputs();
    }
});

// 格式化时间为 YYYY-MM-DD HH:mm:ss
function formatTime(str){
    if(!str) return '-';
    // 处理 ISO 格式 2026-06-27T00:10:08 或 2026-06-27 00:10:08
    var s = str.replace('T',' ');
    // 截取到秒 YYYY-MM-DD HH:mm:ss
    return s.length>=19 ? s.substring(0,19) : s;
}

// 退出登录
function handleLogout() {
    clearAuth();
    var loginPath = window.location.pathname.includes('/systems/') ? '../../login.html' : 'login.html';
    window.location.href = loginPath;
}
