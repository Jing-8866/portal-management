let allOrders=[], filteredOrders=[], orderPage=1, orderPageSize=10;
document.addEventListener('DOMContentLoaded', function(){
    if(!isLoggedIn()){window.location.href='../../login.html';return;}
    initUserDropdown(); clearSearchInputs();
    refreshOrderListPage();
    sessionStorage.removeItem('orderListNeedRefresh');
});
window.addEventListener('pageshow', function(e){
    if(e.persisted || sessionStorage.getItem('orderListNeedRefresh') === '1'){
        sessionStorage.removeItem('orderListNeedRefresh');
        refreshOrderListPage();
    }
});
function refreshOrderListPage(){
    loadStats();
    loadOrders();
}
async function loadStats(){
    try{
        var s=await request('/orders/stats');
        if(s){
            document.getElementById('stat-total').textContent=s.total||0;
            document.getElementById('stat-pending').textContent=s.pending||0;
            document.getElementById('stat-completed').textContent=s.completed||0;
            document.getElementById('stat-cancelled').textContent=s.cancelled||0;
        }
    }catch(e){console.error(e);}
}
async function loadOrders(){
    try{
        var res=await request('/orders?page=1&size=9999')||{};
        allOrders=res.records||res||[];
        applyFilter();
    }catch(e){showToast('加载订单失败','error');}
}
function applyFilter(){
    var keyword=document.getElementById('searchInput').value.trim().toLowerCase();
    var statusVal=document.getElementById('statusFilter').value;
    filteredOrders=allOrders.filter(function(o){
        var mk=!keyword||(o.orderNo||'').toLowerCase().includes(keyword)||(o.customerName||'').toLowerCase().includes(keyword);
        var ms=!statusVal||o.status===statusVal;
        return mk&&ms;
    });
    orderPage=1; renderTable();
}
function renderTable(){
    var start=(orderPage-1)*orderPageSize, pageData=filteredOrders.slice(start,start+orderPageSize);
    var tbody=document.getElementById('orderTableBody');
    if(!pageData.length){tbody.innerHTML='<tr><td colspan="7" style="text-align:center;padding:40px;color:#999;">暂无数据</td></tr>';}
    else{
        tbody.innerHTML=pageData.map(function(o,i){
            var statusMap={pending:'待处理',processing:'处理中',completed:'已完成',cancelled:'已取消'};
            var classMap={pending:'status-pending',processing:'status-processing',completed:'status-completed',cancelled:'status-cancelled'};
            var badge='<span class="status-badge '+(classMap[o.status]||'')+'">'+(statusMap[o.status]||o.status)+'</span>';
            var actions='';
            actions+='<button class="btn btn-sm" style="background:#3B82F6;color:#fff;padding:4px 10px;border:none;border-radius:4px;cursor:pointer;font-size:12px;margin-right:4px;" onclick="viewOrder('+o.id+')">查看</button>';
            if(isAdmin()&&(o.status==='pending'||o.status==='processing')){
                actions+='<button class="btn btn-sm" style="background:#10B981;color:#fff;padding:4px 10px;border:none;border-radius:4px;cursor:pointer;font-size:12px;margin-right:4px;" onclick="updateOrderStatus('+o.id+',\'completed\')">完成</button>'+
                    '<button class="btn btn-sm" style="background:#EF4444;color:#fff;padding:4px 10px;border:none;border-radius:4px;cursor:pointer;font-size:12px;" onclick="updateOrderStatus('+o.id+',\'cancelled\')">取消</button>';
            }
            return '<tr><td>'+(start+i+1)+'</td><td>'+(o.orderNo||'')+'</td><td>'+(o.customerName||'')+'</td><td>¥'+(o.amount||0).toFixed(2)+'</td><td>'+badge+'</td><td>'+formatTime(o.createdTime)+'</td><td>'+actions+'</td></tr>';
        }).join('');
    }
    renderPagination();
}
function renderPagination(){
    var total=filteredOrders.length, totalPages=Math.ceil(total/orderPageSize)||1;
    var html='<div class="pagination-info">共 '+total+' 条</div><div class="pagination-btns">';
    html+='<button '+(orderPage<=1?'disabled':'')+' onclick="goPage('+(orderPage-1)+')">‹</button>';
    for(var p=1;p<=totalPages;p++) html+='<button class="'+(p===orderPage?'active':'')+'" onclick="goPage('+p+')">'+p+'</button>';
    html+='<button '+(orderPage>=totalPages?'disabled':'')+' onclick="goPage('+(orderPage+1)+')">›</button></div>';
    document.getElementById('pagination').innerHTML=html;
}
function goPage(p){orderPage=p;renderTable();}
function viewOrder(id){ window.location.href='order-detail.html?id='+id; }
function highlightNav(){var page=window.location.pathname.split('/').pop();document.querySelectorAll('.nav-item').forEach(function(item){var href=item.getAttribute('href');if(href&&href.split('/').pop()===page)item.classList.add('active');else item.classList.remove('active');});}
// call highlightNav on load
highlightNav();
async function updateOrderStatus(id,status){
    if(!confirm('确定'+( status==='completed'?'完成':'取消')+'该订单？'))return;
    try{
        await request('/orders/'+id+'/status?status='+status,{method:'PUT'});
        showToast('操作成功','success'); loadOrders(); loadStats();
    }catch(e){showToast(e.message||'操作失败','error');}
}
