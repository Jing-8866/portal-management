var allCategories = [], filteredCategories = [], categoryPage = 1, categoryPageSize = 10;
var editingCategoryId = null;
var categoryProductCounts = {};

document.addEventListener('DOMContentLoaded', function () {
    if (!initOrderLayout('category-admin.html')) return;
    if (!isAdmin()) {
        showToast('无权访问', 'error');
        setTimeout(function () { window.location.href = 'index.html'; }, 800);
        return;
    }
    loadCategories();
});

async function loadCategories() {
    try {
        var keyword = document.getElementById('searchInput').value.trim();
        var url = '/product-categories';
        if (keyword) url += '?keyword=' + encodeURIComponent(keyword);
        allCategories = await request(url) || [];
        await loadCategoryProductCounts();
        applyCategoryFilter();
    } catch (e) {
        showToast('加载分类失败', 'error');
    }
}

async function loadCategoryProductCounts() {
    categoryProductCounts = {};
    try {
        var products = await request('/products') || [];
        products.forEach(function (p) {
            var cat = p.category || '';
            if (cat) categoryProductCounts[cat] = (categoryProductCounts[cat] || 0) + 1;
        });
    } catch (e) { /* 统计失败不影响列表 */ }
}

function applyCategoryFilter() {
    var keyword = document.getElementById('searchInput').value.trim().toLowerCase();
    filteredCategories = allCategories.filter(function (c) {
        if (!keyword) return true;
        return (c.name || '').toLowerCase().includes(keyword);
    });
    categoryPage = 1;
    renderCategoryTable();
}

function renderCategoryTable() {
    var start = (categoryPage - 1) * categoryPageSize;
    var pageData = filteredCategories.slice(start, start + categoryPageSize);
    var tbody = document.getElementById('categoryTableBody');
    if (!pageData.length) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;color:#999;">暂无数据</td></tr>';
    } else {
        tbody.innerHTML = pageData.map(function (c, i) {
            var count = categoryProductCounts[c.name] || 0;
            return '<tr><td>' + (start + i + 1) + '</td><td>' + (c.name || '') + '</td>' +
                '<td>' + (c.sortOrder != null ? c.sortOrder : 0) + '</td>' +
                '<td>' + count + '</td>' +
                '<td>' + formatTime(c.createdTime) + '</td>' +
                '<td><button class="btn-action btn-view" onclick="openCategoryModal(' + c.id + ')">编辑</button> ' +
                '<button class="btn-action btn-danger" onclick="deleteCategory(' + c.id + ')">删除</button></td></tr>';
        }).join('');
    }
    renderCategoryPagination();
}

function renderCategoryPagination() {
    var total = filteredCategories.length;
    var totalPages = Math.ceil(total / categoryPageSize) || 1;
    var html = '<div class="pagination-info">共 ' + total + ' 条</div><div class="pagination-btns">';
    html += '<button ' + (categoryPage <= 1 ? 'disabled' : '') + ' onclick="goCategoryPage(' + (categoryPage - 1) + ')">‹</button>';
    for (var p = 1; p <= totalPages; p++) {
        html += '<button class="' + (p === categoryPage ? 'active' : '') + '" onclick="goCategoryPage(' + p + ')">' + p + '</button>';
    }
    html += '<button ' + (categoryPage >= totalPages ? 'disabled' : '') + ' onclick="goCategoryPage(' + (categoryPage + 1) + ')">›</button></div>';
    document.getElementById('pagination').innerHTML = html;
}

function goCategoryPage(p) { categoryPage = p; renderCategoryTable(); }

function openCategoryModal(id) {
    editingCategoryId = id || null;
    document.getElementById('categoryModalTitle').textContent = id ? '编辑分类' : '新增分类';
    if (id) {
        var cat = allCategories.find(function (c) { return c.id === id; });
        if (!cat) return;
        document.getElementById('cName').value = cat.name || '';
        document.getElementById('cSortOrder').value = cat.sortOrder != null ? cat.sortOrder : 0;
    } else {
        document.getElementById('cName').value = '';
        document.getElementById('cSortOrder').value = 0;
    }
    document.getElementById('categoryModal').style.display = 'flex';
}

function closeCategoryModal() {
    document.getElementById('categoryModal').style.display = 'none';
    editingCategoryId = null;
}

async function saveCategory() {
    var data = {
        name: document.getElementById('cName').value.trim(),
        sortOrder: parseInt(document.getElementById('cSortOrder').value, 10) || 0
    };
    if (!data.name) {
        showToast('请填写分类名称', 'error');
        return;
    }
    try {
        if (editingCategoryId) {
            await request('/product-categories/' + editingCategoryId, { method: 'PUT', body: JSON.stringify(data) });
        } else {
            await request('/product-categories', { method: 'POST', body: JSON.stringify(data) });
        }
        showToast('保存成功', 'success');
        closeCategoryModal();
        loadCategories();
    } catch (e) {
        showToast(e.message || '保存失败', 'error');
    }
}

async function deleteCategory(id) {
    var cat = allCategories.find(function (c) { return c.id === id; });
    if (!cat) return;
    var count = categoryProductCounts[cat.name] || 0;
    if (count > 0) {
        showToast('该分类下仍有 ' + count + ' 个商品，无法删除', 'error');
        return;
    }
    if (!confirm('确定删除分类「' + cat.name + '」？')) return;
    try {
        await request('/product-categories/' + id, { method: 'DELETE' });
        showToast('已删除', 'success');
        loadCategories();
    } catch (e) {
        showToast(e.message || '删除失败', 'error');
    }
}
