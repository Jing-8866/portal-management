var cartAddresses = [], selectedAddressId = null;

document.addEventListener('DOMContentLoaded', function () {
    if (!initOrderLayout('cart.html')) return;
    loadCart();
});

async function loadCart() {
    try {
        var items = await request('/cart') || [];
        cartAddresses = await request('/addresses') || [];
        renderCart(items);
        renderAddressSelect();
    } catch (e) { showToast('加载购物车失败', 'error'); }
}

function renderCart(items) {
    var container = document.getElementById('cartContainer');
    var checkoutBar = document.getElementById('checkoutBar');
    if (!items.length) {
        container.innerHTML = '<div class="empty-tip"><i class="fas fa-shopping-cart"></i><p>购物车是空的</p><a href="index.html" class="btn btn-primary">去逛逛</a></div>';
        checkoutBar.style.display = 'none';
        return;
    }
    var total = 0;
    var html = '<div class="toolbar" style="margin-bottom:12px;">' +
        '<div class="toolbar-left"><span style="font-size:14px;color:#666;">共 ' + items.length + ' 种商品</span></div>' +
        '<div class="toolbar-right">' +
        '<button class="btn btn-danger btn-sm" onclick="clearCart()"><i class="fas fa-trash-alt"></i> 清空购物车</button>' +
        '</div></div><div class="cart-list">';
    items.forEach(function (item) {
        total += parseFloat(item.subtotal) || 0;
        var disabled = item.status !== 'on_shelf';
        html += '<div class="cart-item' + (disabled ? ' cart-item-disabled' : '') + '">' +
            '<div class="cart-item-info"><h4>' + (item.productName || '') + '</h4>' +
            (disabled ? '<span class="tag-off">已下架</span>' : '') +
            '<p>单价 ' + formatMoney(item.productPrice) + ' · 库存 ' + (item.stock || 0) + '</p></div>' +
            '<div class="cart-item-qty">' +
            '<button onclick="changeQty(' + item.id + ',' + (item.quantity - 1) + ')">-</button>' +
            '<input type="number" value="' + item.quantity + '" min="1" max="' + (item.stock || 1) + '" onchange="changeQty(' + item.id + ',this.value)">' +
            '<button onclick="changeQty(' + item.id + ',' + (item.quantity + 1) + ')">+</button></div>' +
            '<div class="cart-item-subtotal">' + formatMoney(item.subtotal) + '</div>' +
            '<button class="btn-remove" onclick="removeItem(' + item.id + ')"><i class="fas fa-trash"></i></button></div>';
    });
    html += '</div>';
    container.innerHTML = html;
    document.getElementById('cartTotal').textContent = formatMoney(total);
    checkoutBar.style.display = 'flex';

    var user = getCurrentUser() || {};
    document.getElementById('customerName').value = user.realName || user.username || '';
}

function renderAddressSelect() {
    var select = document.getElementById('addressSelect');
    var preview = document.getElementById('addressSelectPreview');
    if (!cartAddresses.length) {
        if (select) select.style.display = 'none';
        if (preview) preview.innerHTML = '<div class="address-empty-tip">暂无收货地址，请先 <a href="addresses.html">添加地址</a></div>';
        selectedAddressId = null;
        return;
    }
    if (select) select.style.display = '';
    if (!selectedAddressId) {
        var def = cartAddresses.find(function (a) { return a.isDefault === 1; });
        selectedAddressId = def ? def.id : cartAddresses[0].id;
    }
    select.innerHTML = cartAddresses.map(function (a) {
        var full = (a.province || '') + (a.city || '') + (a.district || '') + (a.detailAddress || '');
        var labelPrefix = a.label ? '[' + a.label + '] ' : '';
        var text = labelPrefix + (a.receiverName || '') + ' ' + (a.receiverPhone || '') + ' - ' + full;
        return '<option value="' + a.id + '"' + (a.id === selectedAddressId ? ' selected' : '') + '>' + text + '</option>';
    }).join('');
    select.value = String(selectedAddressId);
    renderAddressPreview();
}

function renderAddressPreview() {
    var preview = document.getElementById('addressSelectPreview');
    if (!preview) return;
    var a = cartAddresses.find(function (x) { return x.id === selectedAddressId; });
    if (!a) {
        preview.innerHTML = '';
        return;
    }
    var full = (a.province || '') + (a.city || '') + (a.district || '') + (a.detailAddress || '');
    preview.innerHTML = '<div class="address-select-head">' +
        '<strong>' + (a.receiverName || '') + '</strong> ' + (a.receiverPhone || '') +
        (a.label ? ' <span class="address-label">' + a.label + '</span>' : '') +
        (a.isDefault === 1 ? ' <span class="address-default-tag">默认</span>' : '') +
        '</div><div class="address-select-full">' + full + '</div>';
}

function selectAddress(id) {
    selectedAddressId = parseInt(id, 10);
    renderAddressPreview();
}

async function changeQty(id, qty) {
    qty = parseInt(qty, 10);
    if (isNaN(qty) || qty < 1) return;
    try {
        await request('/cart/' + id, { method: 'PUT', body: JSON.stringify({ quantity: qty }) });
        loadCart();
        updateCartBadge();
    } catch (e) { showToast(e.message || '更新失败', 'error'); loadCart(); }
}

async function removeItem(id) {
    if (!confirm('确定移除该商品？')) return;
    try {
        await request('/cart/' + id, { method: 'DELETE' });
        showToast('已移除', 'success');
        loadCart();
        updateCartBadge();
    } catch (e) { showToast(e.message || '移除失败', 'error'); }
}

async function clearCart() {
    if (!confirm('确定清空购物车中的所有商品？')) return;
    try {
        await request('/cart', { method: 'DELETE' });
        showToast('购物车已清空', 'success');
        loadCart();
        updateCartBadge();
    } catch (e) { showToast(e.message || '清空失败', 'error'); }
}

async function submitCheckout() {
    if (!selectedAddressId) {
        showToast('请选择收货地址', 'error');
        return;
    }
    var data = {
        addressId: selectedAddressId,
        customerName: document.getElementById('customerName').value.trim(),
        remark: document.getElementById('remark').value.trim()
    };
    if (!confirm('确认提交订单？')) return;
    try {
        await request('/orders/checkout', { method: 'POST', body: JSON.stringify(data) });
        showToast('下单成功', 'success');
        updateCartBadge();
        sessionStorage.setItem('orderListNeedRefresh', '1');
        window.location.href = 'orders.html';
    } catch (e) { showToast(e.message || '下单失败', 'error'); }
}
