var allOrders = [], filteredOrders = [], orderPage = 1, orderPageSize = 10;
var orderListConfig = { scope: 'mine', activePage: 'orders.html', showCountdown: false, showCustomerCol: false, adminOnly: false };
var paymentTimeoutMinutes = 30;
var countdownTimer = null;
var countdownRefreshScheduled = false;

function initOrderListPage(config) {
    orderListConfig = Object.assign({
        scope: 'mine',
        activePage: 'orders.html',
        showCountdown: false,
        showCustomerCol: false,
        adminOnly: false
    }, config);

    function boot() {
        if (!initOrderLayout(orderListConfig.activePage)) return;
        if (orderListConfig.adminOnly && !hasSubsystemAdmin(SUBSYSTEM_CODE.ORDER_MGMT)) {
            showToast('无权访问', 'error');
            setTimeout(function () { window.location.href = 'orders.html'; }, 800);
            return;
        }
        setupOrderTableHeader();
        loadOrderSettings().then(function () {
            refreshOrderListPage();
        });
        sessionStorage.removeItem('orderListNeedRefresh');
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }

    window.addEventListener('pageshow', function (e) {
        if (e.persisted || sessionStorage.getItem('orderListNeedRefresh') === '1') {
            sessionStorage.removeItem('orderListNeedRefresh');
            refreshOrderListPage();
        }
    });

    window.addEventListener('beforeunload', stopPayCountdownTimer);
}

function setupOrderTableHeader() {
    var head = document.getElementById('orderTableHead');
    if (!head) return;
    var html = '<th>序号</th><th>订单号</th>';
    if (orderListConfig.showCustomerCol) html += '<th>客户</th>';
    html += '<th>金额</th><th>状态</th>';
    if (orderListConfig.showCountdown) html += '<th>支付倒计时</th>';
    html += '<th>创建时间</th><th>操作</th>';
    head.innerHTML = html;
}

async function loadOrderSettings() {
    if (!orderListConfig.showCountdown) return;
    try {
        var settings = await request('/orders/settings');
        if (settings && settings.paymentTimeoutMinutes) {
            paymentTimeoutMinutes = settings.paymentTimeoutMinutes;
        }
    } catch (e) { /* 使用默认 30 分钟 */ }
}

function refreshOrderListPage() {
    loadStats();
    loadOrders();
}

async function loadStats() {
    try {
        var s = await request('/orders/stats?scope=' + orderListConfig.scope);
        if (s) {
            document.getElementById('stat-total').textContent = s.total || 0;
            document.getElementById('stat-pending').textContent = s.pending || 0;
            document.getElementById('stat-paid').textContent = s.paid || 0;
            document.getElementById('stat-completed').textContent = s.completed || 0;
        }
    } catch (e) { console.error(e); }
}

async function loadOrders() {
    try {
        var statusVal = document.getElementById('statusFilter').value;
        var startDate = document.getElementById('startDate').value;
        var endDate = document.getElementById('endDate').value;
        var keyword = document.getElementById('searchInput').value.trim();
        var url = '/orders?scope=' + orderListConfig.scope;
        if (statusVal) url += '&status=' + statusVal;
        if (startDate) url += '&startDate=' + startDate;
        if (endDate) url += '&endDate=' + endDate;
        if (keyword) url += '&keyword=' + encodeURIComponent(keyword);
        allOrders = await request(url) || [];
        applyFilter();
    } catch (e) { showToast('加载订单失败', 'error'); }
}

function applyFilter() {
    var keyword = document.getElementById('searchInput').value.trim().toLowerCase();
    filteredOrders = allOrders.filter(function (o) {
        if (!keyword) return true;
        return (o.orderNo || '').toLowerCase().includes(keyword) ||
            (o.customerName || '').toLowerCase().includes(keyword) ||
            (o.receiverName || '').toLowerCase().includes(keyword);
    });
    orderPage = 1;
    renderTable();
}

function getTableColSpan() {
    var cols = 6;
    if (orderListConfig.showCustomerCol) cols++;
    if (orderListConfig.showCountdown) cols++;
    return cols;
}

function renderTable() {
    var start = (orderPage - 1) * orderPageSize;
    var pageData = filteredOrders.slice(start, start + orderPageSize);
    var tbody = document.getElementById('orderTableBody');
    var colSpan = getTableColSpan();
    stopPayCountdownTimer();

    if (!pageData.length) {
        tbody.innerHTML = '<tr><td colspan="' + colSpan + '" style="text-align:center;padding:40px;color:#999;">暂无数据</td></tr>';
    } else {
        tbody.innerHTML = pageData.map(function (o, i) {
            var actions = '<button class="btn-action btn-view" onclick="viewOrder(' + o.id + ')">查看</button>';
            actions += buildQuickActions(o);
            var row = '<tr data-order-id="' + o.id + '"><td>' + (start + i + 1) + '</td><td>' + (o.orderNo || '') + '</td>';
            if (orderListConfig.showCustomerCol) {
                row += '<td>' + (o.customerName || o.receiverName || '-') + '</td>';
            }
            row += '<td>' + formatMoney(o.amount) + '</td><td>' + orderStatusBadge(o.status) + '</td>';
            if (orderListConfig.showCountdown) {
                if (o.status === 'pending') {
                    row += '<td><span class="pay-countdown" data-created="' + (o.createdTime || '') + '">' +
                        formatPayCountdown(o.createdTime, paymentTimeoutMinutes) + '</span></td>';
                } else {
                    row += '<td>-</td>';
                }
            }
            row += '<td>' + formatTime(o.createdTime) + '</td><td>' + actions + '</td></tr>';
            return row;
        }).join('');
        if (orderListConfig.showCountdown) {
            startPayCountdownTimer();
        }
    }
    renderPagination();
}

function buildQuickActions(o) {
    var html = '';
    if (orderListConfig.scope === 'mine') {
        if (o.status === 'pending') {
            html += '<button class="btn-action btn-success" onclick="updateOrderStatus(' + o.id + ',\'paid\')">付款</button>';
            html += '<button class="btn-action btn-danger" onclick="updateOrderStatus(' + o.id + ',\'cancelled\')">取消</button>';
        }
        if (o.status === 'shipped') {
            html += '<button class="btn-action btn-success" onclick="updateOrderStatus(' + o.id + ',\'completed\')">确认收货</button>';
        }
        if (o.status === 'cancelled') {
            html += '<button class="btn-action btn-primary" onclick="reorderToCart(' + o.id + ')">加入购物车</button>';
        }
        if (canDeleteOrder(o)) {
            html += '<button class="btn-action btn-danger" onclick="deleteOrder(' + o.id + ')">删除</button>';
        }
    }
    if (orderListConfig.scope === 'all') {
        if (o.status === 'paid') html += '<button class="btn-action btn-primary" onclick="updateOrderStatus(' + o.id + ',\'shipped\')">发货</button>';
        if (o.status === 'refunding') html += '<button class="btn-action btn-success" onclick="updateOrderStatus(' + o.id + ',\'refunded\')">同意退款</button>';
        if (o.status === 'returning') html += '<button class="btn-action btn-success" onclick="updateOrderStatus(' + o.id + ',\'returned\')">同意退货</button>';
    }
    return html;
}

function startPayCountdownTimer() {
    stopPayCountdownTimer();
    var hasPending = filteredOrders.some(function (o) { return o.status === 'pending'; });
    if (!hasPending) return;
    countdownTimer = setInterval(function () {
        var expired = false;
        document.querySelectorAll('.pay-countdown').forEach(function (el) {
            var text = formatPayCountdown(el.getAttribute('data-created'), paymentTimeoutMinutes);
            el.textContent = text;
            if (text === '已超时') {
                el.classList.add('pay-countdown-expired');
                expired = true;
            }
        });
        if (expired && !countdownRefreshScheduled) {
            countdownRefreshScheduled = true;
            setTimeout(function () {
                countdownRefreshScheduled = false;
                refreshOrderListPage();
            }, 1500);
        }
    }, 1000);
}

function stopPayCountdownTimer() {
    if (countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
    }
}

function renderPagination() {
    var total = filteredOrders.length, totalPages = Math.ceil(total / orderPageSize) || 1;
    var html = '<div class="pagination-info">共 ' + total + ' 条</div><div class="pagination-btns">';
    html += '<button ' + (orderPage <= 1 ? 'disabled' : '') + ' onclick="goPage(' + (orderPage - 1) + ')">‹</button>';
    for (var p = 1; p <= totalPages; p++) html += '<button class="' + (p === orderPage ? 'active' : '') + '" onclick="goPage(' + p + ')">' + p + '</button>';
    html += '<button ' + (orderPage >= totalPages ? 'disabled' : '') + ' onclick="goPage(' + (orderPage + 1) + ')">›</button></div>';
    document.getElementById('pagination').innerHTML = html;
}

function goPage(p) { orderPage = p; renderTable(); }
function viewOrder(id) { window.location.href = 'order-detail.html?id=' + id; }

function canDeleteOrder(o) {
    return ['cancelled', 'completed', 'refunded', 'returned'].indexOf(o.status) >= 0;
}

async function deleteOrder(id) {
    if (!confirm('确定删除该订单？删除后不可恢复。')) return;
    try {
        await request('/orders/' + id, { method: 'DELETE' });
        showToast('删除成功', 'success');
        refreshOrderListPage();
    } catch (e) { showToast(e.message || '删除失败', 'error'); }
}

async function updateOrderStatus(id, status) {
    var label = ORDER_STATUS_MAP[status] || status;
    if (!confirm('确定执行「' + label + '」操作？')) return;
    try {
        await request('/orders/' + id + '/status?status=' + status, { method: 'PUT' });
        showToast('操作成功', 'success');
        refreshOrderListPage();
    } catch (e) { showToast(e.message || '操作失败', 'error'); }
}

function onFilterChange() { loadOrders(); }
