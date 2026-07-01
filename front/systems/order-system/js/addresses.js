let addressList = [], editingAddressId = null;

document.addEventListener('DOMContentLoaded', function () {
    if (!initOrderLayout('addresses.html')) return;
    loadAddresses();
});

async function loadAddresses() {
    try {
        addressList = await request('/addresses') || [];
        renderAddressList();
    } catch (e) {
        showToast('加载地址失败', 'error');
    }
}

function renderAddressList() {
    var container = document.getElementById('addressList');
    if (!addressList.length) {
        container.innerHTML = '<div class="empty-tip"><i class="fas fa-map-marker-alt"></i><p>暂无收货地址</p><button class="btn btn-primary" onclick="openAddressModal()">新增地址</button></div>';
        return;
    }
    container.innerHTML = addressList.map(function (a) {
        var full = formatFullAddress(a);
        return '<div class="address-card' + (a.isDefault === 1 ? ' address-default' : '') + '">' +
            '<div class="address-card-head">' +
            '<span class="address-name">' + (a.receiverName || '') + '</span>' +
            '<span class="address-phone">' + (a.receiverPhone || '') + '</span>' +
            (a.label ? '<span class="address-label">' + a.label + '</span>' : '') +
            (a.isDefault === 1 ? '<span class="address-default-tag">默认</span>' : '') +
            '</div>' +
            '<div class="address-full">' + full + '</div>' +
            '<div class="address-actions">' +
            (a.isDefault !== 1 ? '<button class="btn-link" onclick="setDefault(' + a.id + ')">设为默认</button>' : '') +
            '<button class="btn-link" onclick="openAddressModal(' + a.id + ')">编辑</button>' +
            '<button class="btn-link danger" onclick="deleteAddress(' + a.id + ')">删除</button>' +
            '</div></div>';
    }).join('');
}

function formatFullAddress(a) {
    return (a.province || '') + (a.city || '') + (a.district || '') + (a.detailAddress || '');
}

function openAddressModal(id) {
    editingAddressId = id || null;
    document.getElementById('addressModalTitle').textContent = id ? '编辑地址' : '新增地址';
    if (id) {
        var a = addressList.find(function (x) { return x.id === id; });
        if (!a) return;
        document.getElementById('aLabel').value = a.label || '';
        document.getElementById('aName').value = a.receiverName || '';
        document.getElementById('aPhone').value = a.receiverPhone || '';
        document.getElementById('aProvince').value = a.province || '';
        document.getElementById('aCity').value = a.city || '';
        document.getElementById('aDistrict').value = a.district || '';
        document.getElementById('aDetail').value = a.detailAddress || '';
        document.getElementById('aDefault').checked = a.isDefault === 1;
    } else {
        document.getElementById('aLabel').value = '';
        document.getElementById('aName').value = '';
        document.getElementById('aPhone').value = '';
        document.getElementById('aProvince').value = '';
        document.getElementById('aCity').value = '';
        document.getElementById('aDistrict').value = '';
        document.getElementById('aDetail').value = '';
        document.getElementById('aDefault').checked = addressList.length === 0;
    }
    document.getElementById('addressModal').style.display = 'flex';
}

function closeAddressModal() {
    document.getElementById('addressModal').style.display = 'none';
    editingAddressId = null;
}

async function saveAddress() {
    var data = {
        label: document.getElementById('aLabel').value.trim(),
        receiverName: document.getElementById('aName').value.trim(),
        receiverPhone: document.getElementById('aPhone').value.trim(),
        province: document.getElementById('aProvince').value.trim(),
        city: document.getElementById('aCity').value.trim(),
        district: document.getElementById('aDistrict').value.trim(),
        detailAddress: document.getElementById('aDetail').value.trim(),
        isDefault: document.getElementById('aDefault').checked ? 1 : 0
    };
    if (!data.receiverName || !data.receiverPhone || !data.detailAddress) {
        showToast('请填写收货人、电话和详细地址', 'error');
        return;
    }
    try {
        if (editingAddressId) {
            await request('/addresses/' + editingAddressId, { method: 'PUT', body: JSON.stringify(data) });
        } else {
            await request('/addresses', { method: 'POST', body: JSON.stringify(data) });
        }
        showToast('保存成功', 'success');
        closeAddressModal();
        loadAddresses();
    } catch (e) {
        showToast(e.message || '保存失败', 'error');
    }
}

async function setDefault(id) {
    try {
        await request('/addresses/' + id + '/default', { method: 'PUT' });
        showToast('已设为默认地址', 'success');
        loadAddresses();
    } catch (e) {
        showToast(e.message || '操作失败', 'error');
    }
}

async function deleteAddress(id) {
    if (!confirm('确定删除该地址？')) return;
    try {
        await request('/addresses/' + id, { method: 'DELETE' });
        showToast('已删除', 'success');
        loadAddresses();
    } catch (e) {
        showToast(e.message || '删除失败', 'error');
    }
}
