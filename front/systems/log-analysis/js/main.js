let allLogs=[], filteredLogs=[], logPage=1, logPageSize=10, subsystemMap={};
document.addEventListener('DOMContentLoaded', function(){
    if (!assertSubsystemLogin(SUBSYSTEM_CODE.LOG_ANALYSIS)) return;
    initUserDropdown(); clearSearchInputs(); initDefaultDateRange(); initPage();
});
async function initPage(){ await loadSources(); loadLogs(); }

function formatDateInput(d) {
    var y = d.getFullYear();
    var m = String(d.getMonth() + 1).padStart(2, '0');
    var day = String(d.getDate()).padStart(2, '0');
    return y + '-' + m + '-' + day;
}

function initDefaultDateRange() {
    var today = new Date();
    var from = new Date(today);
    from.setDate(today.getDate() - 6);
    document.getElementById('dateFrom').value = formatDateInput(from);
    document.getElementById('dateTo').value = formatDateInput(today);
}

function onDateRangeChange() {
    var dateFrom = document.getElementById('dateFrom').value;
    var dateTo = document.getElementById('dateTo').value;
    if (dateFrom && dateTo && dateFrom > dateTo) {
        showToast('开始日期不能晚于结束日期', 'warning');
        return;
    }
    loadLogs();
}

async function loadSources(){
    try{
        var codes = await request('/logs/subsystem-codes') || [];
        var subsystems = [];
        try { subsystems = await request('/subsystems') || []; } catch(e){}
        subsystems.forEach(function(s){ if(s.systemCode) subsystemMap[s.systemCode]=s.systemName; });
        var sel = document.getElementById('sourceFilter');
        // Merge codes and subsystem names
        var sourceSet = new Set();
        codes.forEach(function(c){ sourceSet.add(c); });
        subsystems.forEach(function(s){ if(s.systemCode) sourceSet.add(s.systemCode); });
        sourceSet.forEach(function(src){
            var name = src;
            var sys = subsystems.find(function(s){return s.systemCode===src;});
            if(sys) name = sys.systemName + ' ('+src+')';
            sel.innerHTML += '<option value="'+src+'">'+name+'</option>';
        });
    }catch(e){console.error('Load sources failed:', e);}
}

async function loadLogs(){
    try{
        var dateFrom = document.getElementById('dateFrom').value;
        var dateTo = document.getElementById('dateTo').value;
        var params = [];
        if (dateFrom) params.push('startDate=' + encodeURIComponent(dateFrom));
        if (dateTo) params.push('endDate=' + encodeURIComponent(dateTo));
        var url = '/logs/all' + (params.length ? '?' + params.join('&') : '');
        allLogs = await request(url) || [];
        applyFilter();
    } catch(e){
        console.error('加载日志失败:', e);
        showToast('加载日志失败: '+(e.message||'请检查后端日志服务'),'error');
        allLogs=[]; applyFilter();
    }
}

function applyFilter(){
    var keyword = document.getElementById('searchInput').value.trim().toLowerCase();
    var level = document.getElementById('levelFilter').value;
    var source = document.getElementById('sourceFilter').value;

    filteredLogs = allLogs.filter(function(log){
        var mk = !keyword || (log.operation||'').toLowerCase().includes(keyword) || (log.username||'').toLowerCase().includes(keyword);
        var ml = !level || log.level===level;
        var ms = !source || log.subsystemCode===source;
        return mk && ml && ms;
    });

    // Update stats based on filtered data
    updateStats();
    logPage = 1;
    renderTable();
}

function updateStats(){
    var total = filteredLogs.length;
    var errors = filteredLogs.filter(function(l){return l.level==='ERROR';}).length;
    var warns = filteredLogs.filter(function(l){return l.level==='WARN';}).length;
    var fails = filteredLogs.filter(function(l){return l.status===0;}).length;
    document.getElementById('stat-total').textContent = total;
    document.getElementById('stat-error').textContent = errors;
    document.getElementById('stat-warn').textContent = warns;
    document.getElementById('stat-fail').textContent = fails;
}

function renderTable(){
    var start=(logPage-1)*logPageSize, pageData=filteredLogs.slice(start,start+logPageSize);
    var tbody=document.getElementById('logTableBody');
    if(!pageData.length){
        tbody.innerHTML='<tr><td colspan="9" style="text-align:center;padding:40px;color:#999;">暂无日志数据</td></tr>';
    } else {
        tbody.innerHTML=pageData.map(function(log){
            var levelBadge='<span class="level-badge level-'+(log.level||'INFO')+'">'+(log.level||'INFO')+'</span>';
            var statusHtml = '';
            if(log.status===1){
                statusHtml='<span class="status-success">成功</span>';
            } else {
                statusHtml='<span class="status-fail">失败</span>';
            }
            var deleteBtn='';
            if(hasSubsystemAdmin(SUBSYSTEM_CODE.LOG_ANALYSIS)){
                deleteBtn='<button class="btn btn-sm btn-danger" onclick="deleteLog('+log.id+')"><i class="fas fa-trash"></i></button>';
            }
            return '<tr><td>'+formatTime(log.createdTime)+'</td><td>'+levelBadge+'</td><td>'+(subsystemMap[log.subsystemCode]||log.subsystemCode||'-')+'</td><td>'+(log.username||'-')+'</td><td>'+(log.operation||'-')+'</td><td>'+(log.ip||'-')+'</td><td>'+statusHtml+'</td><td>'+(log.duration||'-')+'</td><td>'+deleteBtn+'</td></tr>';
        }).join('');
    }
    renderPagination();
}

function renderPagination(){
    var total=filteredLogs.length, totalPages=Math.ceil(total/logPageSize)||1;
    var start=(logPage-1)*logPageSize+1, end=Math.min(logPage*logPageSize,total);
    var html='<div class="pagination-info">显示 '+(total>0?start:0)+'-'+end+' 条，共 '+total+' 条 | 每页 '+
        '<select class="page-size-select" onchange="changePageSize(this.value)">'+
        [5,10,20,50,100].map(function(s){return '<option value="'+s+'"'+(s===logPageSize?' selected':'')+'>'+s+'</option>';}).join('')+
        '</select> 条</div>';
    html+='<div class="pagination-btns"><button '+(logPage<=1?'disabled':'')+' onclick="goPage('+(logPage-1)+')">‹</button>';
    for(var p=1;p<=totalPages;p++){
        if(totalPages>7&&Math.abs(p-logPage)>2&&p!==1&&p!==totalPages){if(p===2||p===totalPages-1)html+='<button disabled>...</button>';continue;}
        html+='<button class="'+(p===logPage?'active':'')+'" onclick="goPage('+p+')">'+p+'</button>';
    }
    html+='<button '+(logPage>=totalPages?'disabled':'')+' onclick="goPage('+(logPage+1)+')">›</button></div>';
    document.getElementById('pagination').innerHTML=html;
}
function goPage(p){logPage=p;renderTable();}
function changePageSize(v){logPageSize=parseInt(v);logPage=1;renderTable();}

async function deleteLog(id){
    if(!confirm('确定删除该日志？'))return;
    try{
        await request('/logs/'+id,{method:'DELETE'});
        showToast('删除成功','success');
        loadLogs();
    }catch(e){showToast(e.message||'删除失败','error');}
}
