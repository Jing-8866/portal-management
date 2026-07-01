let allProducts = [], productPage = 1, productPageSize = 12;

document.addEventListener('DOMContentLoaded', function () {
    if (!initOrderLayout('index.html')) return;
    loadProductCategories('categoryFilter', true).then(function () {
        loadProducts();
    });
});

async function loadProducts() {
    try {
        var keyword = document.getElementById('searchInput').value.trim();
        var category = document.getElementById('categoryFilter').value;
        var url = '/products';
        var params = [];
        if (keyword) params.push('keyword=' + encodeURIComponent(keyword));
        if (category) params.push('category=' + encodeURIComponent(category));
        if (params.length) url += '?' + params.join('&');
        allProducts = await request(url) || [];
        productPage = 1;
        renderProductPage();
    } catch (e) {
        showToast('加载商品失败', 'error');
    }
}

function renderProductPage() {
    var start = (productPage - 1) * productPageSize;
    var pageData = allProducts.slice(start, start + productPageSize);
    renderProductGrid(pageData);
    renderProductPagination();
}

function renderProductGrid(products) {
    var grid = document.getElementById('productGrid');
    if (!products.length) {
        grid.innerHTML = '<div class="empty-tip">暂无商品</div>';
        return;
    }
    grid.innerHTML = products.map(function (p) {
        var imgSrc = resolveImageUrl(p.imageUrl);
        var img = imgSrc
            ? '<img src="' + imgSrc.replace(/"/g, '&quot;') + '" alt="">'
            : '<div class="product-placeholder"><i class="fas fa-image"></i></div>';
        return '<div class="product-card">' +
            '<div class="product-image">' + img + '</div>' +
            '<div class="product-body">' +
            '<div class="product-category">' + (p.category || '未分类') + '</div>' +
            '<h4 class="product-name" title="' + (p.name || '') + '">' + (p.name || '') + '</h4>' +
            '<p class="product-desc">' + (p.description || '') + '</p>' +
            '<div class="product-footer">' +
            '<span class="product-price">' + formatMoney(p.price) + '</span>' +
            '<span class="product-stock">库存 ' + (p.stock || 0) + '</span>' +
            '</div>' +
            '<button class="btn btn-primary btn-block" onclick="addToCart(' + p.id + ')"><i class="fas fa-cart-plus"></i> 加入购物车</button>' +
            '</div></div>';
    }).join('');
}

function renderProductPagination() {
    var productTotal = allProducts.length;
    var totalPages = Math.ceil(productTotal / productPageSize) || 1;
    var html = '<div class="pagination-info">共 ' + productTotal + ' 件商品</div><div class="pagination-btns">';
    html += '<button ' + (productPage <= 1 ? 'disabled' : '') + ' onclick="goProductPage(' + (productPage - 1) + ')">‹</button>';
    for (var p = 1; p <= totalPages && p <= 10; p++) {
        html += '<button class="' + (p === productPage ? 'active' : '') + '" onclick="goProductPage(' + p + ')">' + p + '</button>';
    }
    html += '<button ' + (productPage >= totalPages ? 'disabled' : '') + ' onclick="goProductPage(' + (productPage + 1) + ')">›</button></div>';
    document.getElementById('pagination').innerHTML = html;
}

function goProductPage(p) { productPage = p; renderProductPage(); }

function applyProductFilter() { loadProducts(); }

async function addToCart(productId) {
    try {
        await request('/cart', { method: 'POST', body: JSON.stringify({ productId: productId, quantity: 1 }) });
        showToast('已加入购物车', 'success');
        updateCartBadge();
    } catch (e) {
        showToast(e.message || '加入失败', 'error');
    }
}
