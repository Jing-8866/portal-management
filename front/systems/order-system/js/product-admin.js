let allAdminProducts = [], adminProductPage = 1, adminProductPageSize = 10, editingProductId = null;
let currentImageMode = 'url';

document.addEventListener('DOMContentLoaded', function () {
    if (!initOrderLayout('product-admin.html')) return;
    if (!isAdmin()) {
        showToast('无权访问', 'error');
        setTimeout(function () { window.location.href = 'index.html'; }, 800);
        return;
    }
    loadAdminProducts();
    loadProductCategories('pCategory', false);
});

async function loadAdminProducts() {
    try {
        var keyword = document.getElementById('searchInput').value.trim();
        var status = document.getElementById('statusFilter').value;
        var url = '/products';
        var params = [];
        if (keyword) params.push('keyword=' + encodeURIComponent(keyword));
        if (status) params.push('status=' + status);
        if (params.length) url += '?' + params.join('&');
        allAdminProducts = await request(url) || [];
        adminProductPage = 1;
        renderAdminTable();
    } catch (e) { showToast('加载失败', 'error'); }
}

function renderAdminTable() {
    var start = (adminProductPage - 1) * adminProductPageSize;
    var pageData = allAdminProducts.slice(start, start + adminProductPageSize);
    var tbody = document.getElementById('productTableBody');
    if (!pageData.length) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:40px;color:#999;">暂无数据</td></tr>';
    } else {
        tbody.innerHTML = pageData.map(function (p, i) {
            var statusBadge = p.status === 'on_shelf'
                ? '<span class="status-badge status-completed">上架</span>'
                : '<span class="status-badge status-cancelled">下架</span>';
            var toggleBtn = p.status === 'on_shelf'
                ? '<button class="btn-action btn-danger" onclick="toggleShelf(' + p.id + ',\'off_shelf\')">下架</button>'
                : '<button class="btn-action btn-success" onclick="toggleShelf(' + p.id + ',\'on_shelf\')">上架</button>';
            return '<tr><td>' + (start + i + 1) + '</td><td>' + (p.name || '') + '</td><td>' + (p.category || '-') + '</td>' +
                '<td>' + formatMoney(p.price) + '</td><td>' + (p.stock || 0) + '</td><td>' + statusBadge + '</td>' +
                '<td>' + formatTime(p.createdTime) + '</td><td>' +
                '<button class="btn-action btn-view" onclick="openProductModal(' + p.id + ')">编辑</button> ' + toggleBtn +
                ' <button class="btn-action btn-danger" onclick="deleteProduct(' + p.id + ')">删除</button></td></tr>';
        }).join('');
    }
    renderAdminPagination();
}

function renderAdminPagination() {
    var adminProductTotal = allAdminProducts.length;
    var totalPages = Math.ceil(adminProductTotal / adminProductPageSize) || 1;
    var html = '<div class="pagination-info">共 ' + adminProductTotal + ' 条</div><div class="pagination-btns">';
    html += '<button ' + (adminProductPage <= 1 ? 'disabled' : '') + ' onclick="goAdminPage(' + (adminProductPage - 1) + ')">‹</button>';
    for (var p = 1; p <= totalPages; p++) html += '<button class="' + (p === adminProductPage ? 'active' : '') + '" onclick="goAdminPage(' + p + ')">' + p + '</button>';
    html += '<button ' + (adminProductPage >= totalPages ? 'disabled' : '') + ' onclick="goAdminPage(' + (adminProductPage + 1) + ')">›</button></div>';
    document.getElementById('pagination').innerHTML = html;
}

function goAdminPage(p) { adminProductPage = p; renderAdminTable(); }
function applyAdminFilter() { loadAdminProducts(); }

function resetProductForm() {
    document.getElementById('pName').value = '';
    document.getElementById('pCategory').value = '';
    document.getElementById('pPrice').value = '';
    document.getElementById('pStock').value = 0;
    document.getElementById('pDesc').value = '';
    document.getElementById('pImageUrl').value = '';
    document.getElementById('pImageFile').value = '';
    document.getElementById('pStatus').value = 'on_shelf';
    switchImageMode('url');
    updateProductImagePreview();
}

function switchImageMode(mode) {
    currentImageMode = mode;
    document.querySelectorAll('.image-mode-tab').forEach(function (btn) {
        btn.classList.toggle('active', btn.getAttribute('data-mode') === mode);
    });
    document.getElementById('imageUrlPanel').style.display = mode === 'url' ? 'block' : 'none';
    document.getElementById('imageUploadPanel').style.display = mode === 'upload' ? 'block' : 'none';
}

function updateProductImagePreview() {
    var preview = document.getElementById('pImagePreview');
    var url = document.getElementById('pImageUrl').value.trim();
    var src = resolveImageUrl(url);
    if (!src) {
        preview.className = 'image-preview empty';
        preview.innerHTML = '<i class="fas fa-image"></i><span>图片预览</span>';
        return;
    }
    preview.className = 'image-preview';
    preview.innerHTML = '<img src="' + src + '" alt="预览" onerror="onImagePreviewError()">';
}

function onImagePreviewError() {
    var preview = document.getElementById('pImagePreview');
    preview.className = 'image-preview empty error';
    preview.innerHTML = '<i class="fas fa-exclamation-triangle"></i><span>图片加载失败</span>';
}

async function uploadProductImage(input) {
    var file = input.files && input.files[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
        showToast('请选择图片文件', 'error');
        input.value = '';
        return;
    }
    if (file.size > 5 * 1024 * 1024) {
        showToast('图片不能超过 5MB', 'error');
        input.value = '';
        return;
    }
    var formData = new FormData();
    formData.append('file', file);
    try {
        showToast('上传中...', 'info');
        var res = await request('/products/upload-image', { method: 'POST', body: formData });
        if (res && res.url) {
            document.getElementById('pImageUrl').value = res.url;
            switchImageMode('url');
            updateProductImagePreview();
            showToast('上传成功', 'success');
        }
    } catch (e) {
        showToast(e.message || '上传失败', 'error');
    }
    input.value = '';
}

function openProductModal(id) {
    editingProductId = id || null;
    document.getElementById('productModalTitle').textContent = id ? '编辑商品' : '新增商品';
    if (id) {
        request('/products/' + id).then(function (p) {
            document.getElementById('pName').value = p.name || '';
            document.getElementById('pCategory').value = p.category || '';
            document.getElementById('pPrice').value = p.price || '';
            document.getElementById('pStock').value = p.stock || 0;
            document.getElementById('pDesc').value = p.description || '';
            document.getElementById('pImageUrl').value = p.imageUrl || '';
            document.getElementById('pStatus').value = p.status || 'on_shelf';
            switchImageMode(p.imageUrl && p.imageUrl.indexOf('/uploads/') === 0 ? 'upload' : 'url');
            updateProductImagePreview();
            document.getElementById('productModal').style.display = 'flex';
        });
    } else {
        resetProductForm();
        document.getElementById('productModal').style.display = 'flex';
    }
}

function closeProductModal() {
    document.getElementById('productModal').style.display = 'none';
    editingProductId = null;
}

async function saveProduct() {
    var data = {
        name: document.getElementById('pName').value.trim(),
        category: document.getElementById('pCategory').value.trim(),
        price: parseFloat(document.getElementById('pPrice').value),
        stock: parseInt(document.getElementById('pStock').value, 10) || 0,
        description: document.getElementById('pDesc').value.trim(),
        imageUrl: document.getElementById('pImageUrl').value.trim(),
        status: document.getElementById('pStatus').value
    };
    if (!data.name || isNaN(data.price)) { showToast('请填写名称和价格', 'error'); return; }
    if (!data.category) { showToast('请选择商品分类', 'error'); return; }
    try {
        if (editingProductId) {
            await request('/products/' + editingProductId, { method: 'PUT', body: JSON.stringify(data) });
        } else {
            await request('/products', { method: 'POST', body: JSON.stringify(data) });
        }
        showToast('保存成功', 'success');
        closeProductModal();
        loadAdminProducts();
    } catch (e) { showToast(e.message || '保存失败', 'error'); }
}

async function toggleShelf(id, status) {
    try {
        await request('/products/' + id + '/status?status=' + status, { method: 'PUT' });
        showToast('操作成功', 'success');
        loadAdminProducts();
    } catch (e) { showToast(e.message || '操作失败', 'error'); }
}

async function deleteProduct(id) {
    if (!confirm('确定删除该商品？')) return;
    try {
        await request('/products/' + id, { method: 'DELETE' });
        showToast('已删除', 'success');
        loadAdminProducts();
    } catch (e) { showToast(e.message || '删除失败', 'error'); }
}
