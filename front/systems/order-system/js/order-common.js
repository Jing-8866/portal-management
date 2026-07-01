// 订单子系统公共逻辑
var ORDER_STATUS_MAP = {
    pending: '待付款', paid: '待发货', shipped: '待收货', completed: '已完成',
    cancelled: '已取消', refunding: '退款中', refunded: '已退款',
    returning: '退货中', returned: '已退货',
    processing: '处理中'
};
var ORDER_STATUS_CLASS = {
    pending: 'status-pending', paid: 'status-paid', shipped: 'status-shipped',
    completed: 'status-completed', cancelled: 'status-cancelled',
    refunding: 'status-refunding', refunded: 'status-refunded',
    returning: 'status-returning', returned: 'status-returned',
    processing: 'status-processing'
};
var PRODUCT_STATUS_MAP = { on_shelf: '上架', off_shelf: '下架' };

function getOrderSidebarHtml(activePage) {
    var adminNav = isAdmin() ? (
        '<div class="nav-group-title">商品管理</div>' +
        '<a class="nav-item' + (activePage === 'product-admin.html' ? ' active' : '') + '" href="product-admin.html"><i class="fas fa-box"></i> 商品管理</a>' +
        '<a class="nav-item' + (activePage === 'category-admin.html' ? ' active' : '') + '" href="category-admin.html"><i class="fas fa-tags"></i> 分类管理</a>' +
        '<div class="nav-group-title">数据分析</div>' +
        '<a class="nav-item' + (activePage === 'order-stats.html' ? ' active' : '') + '" href="order-stats.html"><i class="fas fa-chart-bar"></i> 订单统计</a>'
    ) : '';
    return '<div class="sidebar-header"><div class="logo"><i class="fas fa-cart-shopping"></i></div><h2>订单商城</h2></div>' +
        '<nav class="sidebar-nav">' +
        '<div class="nav-group-title">商城</div>' +
        '<a class="nav-item' + (activePage === 'index.html' ? ' active' : '') + '" href="index.html"><i class="fas fa-store"></i> 商品列表</a>' +
        '<a class="nav-item' + (activePage === 'cart.html' ? ' active' : '') + '" href="cart.html"><i class="fas fa-shopping-cart"></i> 购物车 <span id="cartBadge" class="nav-badge" style="display:none;"></span></a>' +
        '<a class="nav-item' + (activePage === 'addresses.html' ? ' active' : '') + '" href="addresses.html"><i class="fas fa-map-marker-alt"></i> 收货地址</a>' +
        '<div class="nav-group-title">订单</div>' +
        '<a class="nav-item' + (activePage === 'orders.html' ? ' active' : '') + '" href="orders.html"><i class="fas fa-list-alt"></i> 我的订单</a>' +
        (isAdmin() ? '<a class="nav-item' + (activePage === 'orders-all.html' ? ' active' : '') + '" href="orders-all.html"><i class="fas fa-clipboard-list"></i> 全部订单</a>' : '') +
        adminNav +
        '</nav>' +
        '<div class="sidebar-footer"><a href="../../index.html"><i class="fas fa-arrow-left"></i> 返回门户首页</a></div>';
}

function initOrderLayout(activePage) {
    if (!isLoggedIn()) { window.location.href = '../../login.html'; return false; }
    var sidebar = document.querySelector('.sidebar');
    if (sidebar) sidebar.innerHTML = getOrderSidebarHtml(activePage);
    initUserDropdown();
    clearSearchInputs();
    initSearchClearButtons();
    updateCartBadge();
    return true;
}

async function updateCartBadge() {
    var badge = document.getElementById('cartBadge');
    if (!badge) return;
    try {
        var res = await request('/cart/count');
        var count = (res && res.count) || 0;
        if (count > 0) { badge.textContent = count; badge.style.display = 'inline-flex'; }
        else { badge.style.display = 'none'; }
    } catch (e) { badge.style.display = 'none'; }
}

function orderStatusBadge(status) {
    return '<span class="status-badge ' + (ORDER_STATUS_CLASS[status] || '') + '">' + (ORDER_STATUS_MAP[status] || status) + '</span>';
}

function formatMoney(val) {
    return '¥' + (parseFloat(val) || 0).toFixed(2);
}

/** 解析商品图片地址：支持 http(s) 外链与本地上传路径 */
function resolveImageUrl(url) {
    if (!url) return '';
    if (/^https?:\/\//i.test(url)) return url;
    if (url.startsWith('/')) {
        var base = (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL)
            ? CONFIG.API_BASE_URL.replace(/\/api\/?$/, '') : 'http://localhost:8080';
        return base + url;
    }
    return url;
}

function parseOrderDate(str) {
    if (!str) return null;
    var normalized = String(str).replace(' ', 'T');
    var d = new Date(normalized);
    return isNaN(d.getTime()) ? null : d;
}

function formatPayCountdown(createdTime, timeoutMinutes) {
    var created = parseOrderDate(createdTime);
    if (!created) return '-';
    var deadline = created.getTime() + timeoutMinutes * 60 * 1000;
    var remain = deadline - Date.now();
    if (remain <= 0) return '已超时';
    var totalSec = Math.floor(remain / 1000);
    var m = Math.floor(totalSec / 60);
    var s = totalSec % 60;
    return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
}

/** 从后端加载商品分类并填充下拉框 */
async function loadProductCategories(selectId, includeAllOption) {
    var select = document.getElementById(selectId);
    if (!select) return [];
    try {
        var categories = await request('/products/categories') || [];
        var html = includeAllOption ? '<option value="">全部分类</option>' : '<option value="">请选择分类</option>';
        categories.forEach(function (name) {
            html += '<option value="' + name + '">' + name + '</option>';
        });
        select.innerHTML = html;
        return categories;
    } catch (e) {
        if (includeAllOption) select.innerHTML = '<option value="">全部分类</option>';
        return [];
    }
}

function getStatusFilterOptions() {
    return [
        { value: '', label: '全部状态' },
        { value: 'pending', label: '待付款' },
        { value: 'paid', label: '待发货' },
        { value: 'shipped', label: '待收货' },
        { value: 'completed', label: '已完成' },
        { value: 'cancelled', label: '已取消' },
        { value: 'refunding', label: '退款中' },
        { value: 'refunded', label: '已退款' },
        { value: 'returning', label: '退货中' },
        { value: 'returned', label: '已退货' }
    ];
}

/** 构建物流进度时间线 HTML（优先使用后端返回的 logistics 数据） */
function buildLogisticsTimelineHtml(order, logistics) {
    var nodes = (logistics && logistics.length) ? mapLogisticsFromApi(logistics) : buildLogisticsNodes(order);
    if (!nodes.length) return '';
    var html = '<div class="logistics-section"><h4 class="logistics-title"><i class="fas fa-truck"></i> 物流进度</h4><div class="logistics-timeline">';
    nodes.forEach(function (node, i) {
        var isLast = i === nodes.length - 1;
        html += '<div class="logistics-step ' + node.state + (isLast ? ' last' : '') + '">';
        html += '<div class="logistics-dot"><i class="fas ' + (node.icon || 'fa-circle') + '"></i></div>';
        html += '<div class="logistics-content">';
        html += '<div class="logistics-step-title">' + node.title + '</div>';
        if (node.location) {
            html += '<div class="logistics-step-location"><i class="fas fa-map-marker-alt"></i><span>' + escapeHtml(node.location) + '</span></div>';
        }
        if (node.desc) html += '<div class="logistics-step-desc">' + escapeHtml(node.desc) + '</div>';
        if (node.time) html += '<div class="logistics-step-time">' + formatTime(node.time) + '</div>';
        html += '</div></div>';
    });
    html += '</div></div>';
    return html;
}

function mapLogisticsFromApi(logistics) {
    var iconMap = {
        '提交订单': 'fa-clipboard-check', '待付款': 'fa-hourglass-half', '付款成功': 'fa-credit-card',
        '待发货': 'fa-box-open', '商家发货': 'fa-box', '快件已揽收': 'fa-shipping-fast',
        '运输中': 'fa-truck-moving', '到达派送站': 'fa-warehouse', '派送中': 'fa-hand-holding-box',
        '已签收': 'fa-check-circle', '交易完成': 'fa-check-circle', '确认收货': 'fa-hand-holding-box',
        '订单已取消': 'fa-times-circle', '退款处理中': 'fa-undo', '已退款': 'fa-money-bill-wave',
        '退货处理中': 'fa-undo', '已退货': 'fa-box-open'
    };
    return logistics.map(function (item) {
        return {
            title: item.title || '',
            desc: item.description || '',
            location: item.location || '',
            time: item.eventTime || null,
            state: item.state || 'wait',
            icon: iconMap[item.title] || 'fa-circle'
        };
    });
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function buildLogisticsNodes(order) {
    var s = order.status;
    var dest = order.receiverAddress || '目的地（待填写）';
    var warehouse = '上海市浦东新区华东仓储中心（金科路288号）';
    var nodes = [];
    nodes.push({
        title: '提交订单',
        desc: '订单已提交，等待付款',
        location: warehouse,
        time: order.createdTime,
        state: 'done',
        icon: 'fa-clipboard-check'
    });

    if (s === 'cancelled') {
        if (order.payTime) {
            nodes.push({ title: '付款成功', location: warehouse + ' · 备货区', time: order.payTime, state: 'done', icon: 'fa-credit-card' });
            nodes.push({ title: '订单已取消', desc: '订单已关闭', location: warehouse, time: order.updatedTime, state: 'failed', icon: 'fa-times-circle' });
        } else {
            nodes.push({ title: '订单已取消', desc: '未付款，订单已关闭', location: warehouse, time: order.updatedTime, state: 'failed', icon: 'fa-times-circle' });
        }
        return nodes;
    }

    if (s === 'pending') {
        nodes.push({ title: '待付款', desc: '等待买家完成付款', location: warehouse + ' · 备货区', state: 'active', icon: 'fa-hourglass-half' });
        nodes.push({ title: '商家发货', desc: '等待商家发货', location: warehouse, state: 'wait', icon: 'fa-box' });
        nodes.push({ title: '确认收货', desc: '等待买家确认收货', location: dest, state: 'wait', icon: 'fa-hand-holding-box' });
        return nodes;
    }

    nodes.push({
        title: '付款成功',
        desc: '买家已付款，等待发货',
        location: warehouse + ' · 备货区',
        time: order.payTime,
        state: 'done',
        icon: 'fa-credit-card'
    });

    if (s === 'paid') {
        nodes.push({ title: '待发货', desc: '商家正在备货', location: warehouse + ' · 出库分拣区', state: 'active', icon: 'fa-box-open' });
        nodes.push({ title: '确认收货', desc: '预计送达', location: dest, state: 'wait', icon: 'fa-hand-holding-box' });
    } else if (s === 'refunding') {
        nodes.push({ title: '退款处理中', desc: '等待商家处理退款申请', location: warehouse + ' · 备货区', state: 'active', icon: 'fa-undo' });
    } else if (s === 'refunded') {
        nodes.push({ title: '已退款', desc: '退款已完成', location: warehouse + ' · 备货区', time: order.updatedTime, state: 'failed', icon: 'fa-money-bill-wave' });
    } else if (s === 'shipped') {
        nodes.push({ title: '快件已揽收', desc: '商品已发出，运输中', location: '上海市浦东新区华东揽收站（张江镇）', time: order.shipTime, state: 'done', icon: 'fa-shipping-fast' });
        nodes.push({ title: '派送中', desc: '等待买家确认收货', location: dest, state: 'active', icon: 'fa-hand-holding-box' });
    } else if (s === 'returning') {
        nodes.push({ title: '商家发货', location: '上海市浦东新区华东揽收站', time: order.shipTime, state: 'done', icon: 'fa-shipping-fast' });
        nodes.push({ title: '退货处理中', desc: '等待商家处理退货申请', location: warehouse, state: 'active', icon: 'fa-undo' });
    } else if (s === 'returned') {
        nodes.push({ title: '商家发货', location: '上海市浦东新区华东揽收站', time: order.shipTime, state: 'done', icon: 'fa-shipping-fast' });
        nodes.push({ title: '已退货', desc: '退货已完成', location: warehouse, time: order.completeTime || order.updatedTime, state: 'failed', icon: 'fa-box-open' });
    } else if (s === 'completed') {
        nodes.push({ title: '快件已揽收', desc: '商品已发出', location: '上海市浦东新区华东揽收站（张江镇）', time: order.shipTime, state: 'done', icon: 'fa-shipping-fast' });
        nodes.push({ title: '交易完成', desc: '买家已确认收货', location: dest, time: order.completeTime, state: 'done', icon: 'fa-check-circle' });
    }
    return nodes;
}

async function reorderToCart(orderId) {
    if (!confirm('将该订单的商品重新加入购物车？')) return;
    try {
        var res = await request('/orders/' + orderId + '/reorder-to-cart', { method: 'POST' });
        var msg = '已成功加入 ' + (res.addedCount || 0) + ' 种商品到购物车';
        if (res.skipped && res.skipped.length) {
            msg += '，' + res.skipped.length + ' 种未加入';
            showToast(msg + '：' + res.skipped.join('；'), 'info');
        } else {
            showToast(msg, 'success');
        }
        updateCartBadge();
    } catch (e) {
        showToast(e.message || '加入购物车失败', 'error');
    }
}
