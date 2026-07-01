let allInstances=[], filteredInstances=[], allDbTypes=[], selectedDbTypes=[], dbTypeFilterInitialized=false;
document.addEventListener('DOMContentLoaded', function(){
    if (!assertSubsystemLogin(SUBSYSTEM_CODE.DB_MGMT)) return;
    initUserDropdown(); clearSearchInputs(); loadInstances();
    document.addEventListener('click', function(e) {
        var wrap = document.getElementById('dbTypeFilterWrap');
        if (wrap && !wrap.contains(e.target)) closeDbTypeFilter();
    });
    if(hasSubsystemAdmin(SUBSYSTEM_CODE.DB_MGMT)){
        document.getElementById('adminBtns').innerHTML=
            '<button class="btn btn-secondary" onclick="refreshAll()"><i class="fas fa-sync-alt"></i> 全部刷新</button>'+
            '<button class="btn btn-primary" onclick="openAddInstance()"><i class="fas fa-plus"></i> 新增实例</button>';
    }
});
async function loadInstances(){
    try{
        allInstances=(await request('/db/instances')||[]).map(function(inst){
            if(inst.status!==1) inst.tableCount=0;
            return inst;
        });
        renderDbTypeFilter();
        filterInstances();
        loadStats();
    }catch(e){showToast('加载实例失败','error');}
}
function getInstanceDbType(inst){
    return (inst.dbType || 'MySQL').trim();
}
function collectDbTypes(){
    var types=[];
    allInstances.forEach(function(inst){
        var t=getInstanceDbType(inst);
        if(types.indexOf(t)<0) types.push(t);
    });
    types.sort();
    return types;
}
function renderDbTypeFilter(){
    var prevTypes=allDbTypes.slice();
    var prevSelected=selectedDbTypes.slice();
    allDbTypes=collectDbTypes();
    if(!allDbTypes.length){
        selectedDbTypes=[];
        updateDbTypeFilterLabel();
        document.getElementById('dbTypeFilterOptions').innerHTML='';
        return;
    }
    if(!dbTypeFilterInitialized){
        selectedDbTypes=allDbTypes.slice();
        dbTypeFilterInitialized=true;
    }else{
        var hadAllSelected=prevTypes.length>0 && prevSelected.length===prevTypes.length;
        selectedDbTypes=allDbTypes.filter(function(t){ return prevSelected.indexOf(t)>=0; });
        if(hadAllSelected) selectedDbTypes=allDbTypes.slice();
    }
    var html=allDbTypes.map(function(t){
        var checked=selectedDbTypes.indexOf(t)>=0?' checked':'';
        return '<label class="multi-filter-item"><input type="checkbox" value="'+escapeAttr(t)+'"'+checked+
            ' onchange="onDbTypeOptionChange()"><span>'+escapeHtml(t)+'</span></label>';
    }).join('');
    document.getElementById('dbTypeFilterOptions').innerHTML=html;
    syncDbTypeSelectAll();
    updateDbTypeFilterLabel();
}
function toggleDbTypeFilter(e){
    e.stopPropagation();
    var panel=document.getElementById('dbTypeFilterPanel');
    var trigger=document.getElementById('dbTypeFilterTrigger');
    var open=panel.style.display!=='none';
    panel.style.display=open?'none':'block';
    trigger.classList.toggle('open', !open);
}
function closeDbTypeFilter(){
    var panel=document.getElementById('dbTypeFilterPanel');
    var trigger=document.getElementById('dbTypeFilterTrigger');
    if(!panel||panel.style.display==='none') return;
    panel.style.display='none';
    if(trigger) trigger.classList.remove('open');
}
function onDbTypeSelectAll(checked){
    selectedDbTypes=checked?allDbTypes.slice():[];
    document.querySelectorAll('#dbTypeFilterOptions input[type=checkbox]').forEach(function(cb){
        cb.checked=checked;
    });
    document.getElementById('dbTypeSelectAll').checked=checked;
    document.getElementById('dbTypeSelectAll').indeterminate=false;
    updateDbTypeFilterLabel();
    filterInstances();
}
function onDbTypeOptionChange(){
    selectedDbTypes=[];
    document.querySelectorAll('#dbTypeFilterOptions input[type=checkbox]').forEach(function(cb){
        if(cb.checked) selectedDbTypes.push(cb.value);
    });
    syncDbTypeSelectAll();
    updateDbTypeFilterLabel();
    filterInstances();
}
function syncDbTypeSelectAll(){
    var allBox=document.getElementById('dbTypeSelectAll');
    if(!allBox||!allDbTypes.length) return;
    var count=selectedDbTypes.length;
    allBox.checked=count===allDbTypes.length;
    allBox.indeterminate=count>0 && count<allDbTypes.length;
}
function updateDbTypeFilterLabel(){
    var label=document.getElementById('dbTypeFilterLabel');
    if(!label) return;
    if(!allDbTypes.length){ label.textContent='无类型'; return; }
    if(selectedDbTypes.length===allDbTypes.length){ label.textContent='全部'; return; }
    if(!selectedDbTypes.length){ label.textContent='未选择'; return; }
    if(selectedDbTypes.length<=2){ label.textContent=selectedDbTypes.join('、'); return; }
    label.textContent='已选 '+selectedDbTypes.length+' 项';
}
function instanceMatchesFilter(inst){
    var keyword=document.getElementById('searchInput').value.trim().toLowerCase();
    var matchKeyword=!keyword||(inst.instanceName||'').toLowerCase().includes(keyword)||(inst.host||'').toLowerCase().includes(keyword);
    var matchType=!allDbTypes.length||selectedDbTypes.indexOf(getInstanceDbType(inst))>=0;
    return matchKeyword && matchType;
}
function escapeHtml(str){
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
function escapeAttr(str){
    return escapeHtml(str).replace(/'/g,'&#39;');
}
async function loadStats(){
    try{
        var s=await request('/db/stats');
        if(s){
            document.getElementById('stat-instances').textContent=s.totalInstances||allInstances.length;
            document.getElementById('stat-tables').textContent=s.totalTables||0;
            document.getElementById('stat-online').textContent=s.onlineCount||0;
        }
    }catch(e){
        document.getElementById('stat-instances').textContent=allInstances.length;
    }
}
function filterInstances(){
    filteredInstances=allInstances.filter(instanceMatchesFilter);
    renderCards();
}
function buildCardHtml(inst){
    var statusClass=inst.status===1?'online':'offline';
    var statusText=inst.status===1?'在线':'离线';
    var tableCount=inst.status===1?(inst.tableCount||0):0;
    var actions = inst.status === 1
        ? '<button class="btn btn-sm btn-primary" onclick="viewTables('+inst.id+')"><i class="fas fa-table"></i> 查看表</button>'
        : '<button class="btn btn-sm btn-primary" disabled title="实例离线，无法查看表"><i class="fas fa-table"></i> 查看表</button>';
    if(hasSubsystemAdmin(SUBSYSTEM_CODE.DB_MGMT)){
        actions+=' <button class="btn-icon" onclick="editInstance('+inst.id+')" title="编辑"><i class="fas fa-edit"></i></button>'+
            ' <button class="btn-icon btn-icon-danger" onclick="deleteInstance('+inst.id+')" title="删除"><i class="fas fa-trash"></i></button>'+
            ' <button class="btn-icon" onclick="refreshInstance('+inst.id+')" title="刷新连接状态"><i class="fas fa-sync-alt"></i></button>';
    }
    var dot = inst.status===1 ? '<span class="status-dot online"></span>' : '<span class="status-dot offline"></span>';
    return '<div class="instance-card" id="instance-card-'+inst.id+'">'+
        '<div class="card-header">'+
            '<div class="card-title-row">'+dot+'<span class="card-title">'+(inst.instanceName||'')+'</span></div>'+
            '<span class="card-status '+statusClass+'">'+statusText+'</span>'+
        '</div>'+
        '<div class="card-info-table">'+
            '<div class="info-row"><span class="info-label">主机:</span><span class="info-value">'+(inst.host||'')+':'+(inst.port||3306)+'</span></div>'+
            '<div class="info-row"><span class="info-label">数据库:</span><span class="info-value">'+(inst.dbName||'')+'</span></div>'+
            '<div class="info-row"><span class="info-label">类型:</span><span class="info-value">'+escapeHtml(getInstanceDbType(inst))+'</span></div>'+
            '<div class="info-row"><span class="info-label">表数量:</span><span class="info-value">'+tableCount+'</span></div>'+
            (inst.description?'<div class="info-row"><span class="info-label">描述:</span><span class="info-value">'+inst.description+'</span></div>':'')+
        '</div>'+
        '<div class="card-actions">'+actions+'</div></div>';
}
function renderCards(){
    var grid=document.getElementById('instancesGrid');
    if(!filteredInstances.length){grid.innerHTML='<p style="color:#999;text-align:center;padding:40px;">暂无实例</p>';return;}
    grid.innerHTML=filteredInstances.map(buildCardHtml).join('');
}
function updateStatsFromLocal(){
    document.getElementById('stat-instances').textContent=allInstances.length;
    var totalTables=allInstances.reduce(function(sum,i){return sum+(i.tableCount||0);},0);
    var onlineCount=allInstances.filter(function(i){return i.status===1;}).length;
    document.getElementById('stat-tables').textContent=totalTables;
    document.getElementById('stat-online').textContent=onlineCount;
}
function applySingleCardUpdate(id){
    var inst=allInstances.find(function(x){return String(x.id)===String(id);});
    if(!inst)return;
    var fIdx=filteredInstances.findIndex(function(x){return String(x.id)===String(id);});
    if(fIdx>=0) filteredInstances[fIdx]=inst;
    var matches=instanceMatchesFilter(inst);
    var cardEl=document.getElementById('instance-card-'+id);
    if(matches){
        if(cardEl) cardEl.outerHTML=buildCardHtml(inst);
        else filterInstances();
    }else if(cardEl){
        cardEl.remove();
        filteredInstances=filteredInstances.filter(function(x){return String(x.id)!==String(id);});
        if(!filteredInstances.length){
            document.getElementById('instancesGrid').innerHTML='<p style="color:#999;text-align:center;padding:40px;">暂无实例</p>';
        }
    }
    updateStatsFromLocal();
}
function mergeInstance(updated, singleCardOnly){
    if(!updated||updated.id==null)return;
    if(updated.status!==1) updated.tableCount=0;
    var idx=allInstances.findIndex(function(x){return String(x.id)===String(updated.id);});
    if(idx>=0) allInstances[idx]=Object.assign({},allInstances[idx],updated);
    else allInstances.push(updated);
    if(singleCardOnly) applySingleCardUpdate(updated.id);
    else{ filterInstances(); loadStats(); }
}

/** 刷新单个实例：重连检测并回写最新状态，仅更新该卡片 */
async function refreshInstanceData(id, silent, singleCardOnly) {
    if(singleCardOnly===undefined) singleCardOnly=true;
    try {
        var updated = await request('/db/instances/' + id + '/refresh', { method: 'POST' });
        if (updated) {
            mergeInstance(updated, singleCardOnly);
            if (!silent) showToast('刷新成功', 'success');
            return updated;
        }
    } catch (e) {
        if (!silent) showToast(e.message || '刷新失败', 'error');
    }
    try {
        var latest = await request('/db/instances/' + id);
        if (latest) mergeInstance(latest, singleCardOnly);
    } catch (e2) { /* ignore */ }
    return null;
}
function viewTables(id){ window.location.href='tables.html?id='+id; }

function openAddInstance(){
    document.getElementById('instanceModalTitle').textContent='新增实例';
    document.getElementById('inst-pass-label').textContent='密码 *';
    document.getElementById('inst-pass').placeholder='请输入密码';
    document.getElementById('inst-id').value='';
    document.getElementById('inst-name').value='';
    document.getElementById('inst-host').value='';
    document.getElementById('inst-port').value='3306';
    document.getElementById('inst-dbname').value='';
    document.getElementById('inst-user').value='';
    document.getElementById('inst-pass').value='';
    document.getElementById('inst-desc').value='';
    document.getElementById('instanceModal').style.display='flex';
}
function editInstance(id){
    var inst=allInstances.find(function(x){return String(x.id)===String(id);});
    if(!inst)return;
    document.getElementById('instanceModalTitle').textContent='编辑实例';
    document.getElementById('inst-pass-label').textContent='密码（留空不修改）';
    document.getElementById('inst-pass').placeholder='留空则保持原密码';
    document.getElementById('inst-id').value=inst.id;
    document.getElementById('inst-name').value=inst.instanceName||'';
    document.getElementById('inst-host').value=inst.host||'';
    document.getElementById('inst-port').value=inst.port||3306;
    document.getElementById('inst-dbname').value=inst.dbName||'';
    document.getElementById('inst-user').value=inst.dbUsername||'';
    document.getElementById('inst-pass').value='';
    document.getElementById('inst-desc').value=inst.description||'';
    document.getElementById('instanceModal').style.display='flex';
}
function closeInstanceModal(){document.getElementById('instanceModal').style.display='none';}

function isDuplicateInstanceName(name, excludeId) {
    var normalized = name.trim().toLowerCase();
    return allInstances.some(function(inst) {
        if (excludeId && String(inst.id) === String(excludeId)) return false;
        return (inst.instanceName || '').trim().toLowerCase() === normalized;
    });
}

async function handleSaveInstance(){
    var id=document.getElementById('inst-id').value;
    var name=document.getElementById('inst-name').value.trim();
    var host=document.getElementById('inst-host').value.trim();
    var port=document.getElementById('inst-port').value;
    var dbName=document.getElementById('inst-dbname').value.trim();
    var dbUser=document.getElementById('inst-user').value.trim();
    var dbPass=document.getElementById('inst-pass').value.trim();
    // 新增时密码必填，编辑时密码可为空（空则不更改，测试连接时用库中密码）
    if(!name||!host||!dbName||!dbUser){showToast('请填写必填项','warning');return;}
    if(!id && !dbPass){showToast('新增实例密码不能为空','warning');return;}
    if(isDuplicateInstanceName(name, id || null)){
        showToast('实例名称已存在，请使用其他名称','warning');
        return;
    }
    var body={instanceName:name,host:host,port:parseInt(port),dbName:dbName,dbType:'MySQL',description:document.getElementById('inst-desc').value.trim()};
    body['db'+'Username']=dbUser;
    if(dbPass){ body['db'+'Pass'+'word']=dbPass; }
    try{
        if(id){
            await request('/db/instances/'+id,{method:'PUT',body:JSON.stringify(body)});
            closeInstanceModal();
            await refreshInstanceData(id, true, true);
            showToast('修改成功','success');
        } else {
            await request('/db/instances',{method:'POST',body:JSON.stringify(body)});
            closeInstanceModal();
            await loadInstances();
            var created=allInstances.find(function(x){
                return (x.instanceName||'').trim().toLowerCase()===name.toLowerCase();
            });
            if(created) await refreshInstanceData(created.id, true, true);
            showToast('新增成功','success');
        }
    }catch(e){showToast(e.message||'保存失败','error');}
}
async function deleteInstance(id){
    if(!confirm('确定删除该实例？'))return;
    try{await request('/db/instances/'+id,{method:'DELETE'});showToast('删除成功','success');loadInstances();}catch(e){showToast(e.message||'删除失败','error');}
}
async function refreshInstance(id){
    await refreshInstanceData(id, false);
}
async function refreshAll(){
    try{await request('/db/instances/refresh',{method:'POST'});showToast('全部刷新成功','success');loadInstances();}catch(e){showToast(e.message||'刷新失败','error');}
}
async function testConnection(){
    var instId=document.getElementById('inst-id').value;
    var host=document.getElementById('inst-host').value.trim();
    var port=document.getElementById('inst-port').value;
    var dbName=document.getElementById('inst-dbname').value.trim();
    var dbUser=document.getElementById('inst-user').value.trim();
    var dbPass=document.getElementById('inst-pass').value.trim();
    if(!host||!dbName||!dbUser){showToast('请填写连接信息','warning');return;}
    if(!instId && !dbPass){showToast('新增实例请填写密码','warning');return;}
    var body={host:host,port:parseInt(port),dbName:dbName};
    body['db'+'Username']=dbUser;
    // 编辑：有输入则用输入框密码，无输入则后端按 id 取库中密码
    if(dbPass){ body['db'+'Pass'+'word']=dbPass; }
    if(instId){ body.id=parseInt(instId); }
    try{
        var r=await request('/db/instances/test-connection',{method:'POST',body:JSON.stringify(body)});
        if(r===true) showToast('连接成功','success');
        else showToast('连接失败','error');
    }catch(e){showToast('连接测试失败: '+e.message,'error');}
}
