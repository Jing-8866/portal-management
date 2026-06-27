let allOnlineInstances = [];
let sqlEditor = null;
let resultTabs = [];
let activeResultTab = 0;

var SQL_EDITOR_DEFAULT = '';
var RESULT_PAGE_SIZE = 50;
var SQL_EDITOR_HEIGHT_KEY = 'portal_sql_editor_height';
var MIN_EDITOR_HEIGHT = 160;
var MIN_RESULT_HEIGHT = 160;
var SQL_RESIZE_HANDLE_HEIGHT = 10;

document.addEventListener('DOMContentLoaded', function() {
    if (!isLoggedIn()) { window.location.href = '../../login.html'; return; }
    initUserDropdown();
    highlightNav();
    initSqlResize();
    initEditor();
    loadOnlineInstances();
});

function highlightNav() {
    var page = window.location.pathname.split('/').pop();
    document.querySelectorAll('.nav-item').forEach(function(item) {
        var href = item.getAttribute('href');
        if (href && href.split('/').pop() === page) item.classList.add('active');
        else item.classList.remove('active');
    });
}

async function loadOnlineInstances() {
    try {
        var all = await request('/db/instances') || [];
        allOnlineInstances = all.filter(function(i) { return i.status === 1; });
        renderDbTypeOptions();
        onDbTypeChange();
        if (!allOnlineInstances.length) {
            showToast('暂无在线实例，请先在实例管理中配置并刷新', 'warning');
        }
    } catch (e) {
        showToast('加载实例失败', 'error');
    }
}

function renderDbTypeOptions() {
    var types = [];
    allOnlineInstances.forEach(function(inst) {
        var t = (inst.dbType || 'MySQL').trim();
        if (types.indexOf(t) < 0) types.push(t);
    });
    types.sort();
    var sel = document.getElementById('dbTypeFilter');
    sel.innerHTML = '<option value="">请选择</option>' +
        types.map(function(t) { return '<option value="' + t + '">' + t + '</option>'; }).join('');
    if (types.length === 1) sel.value = types[0];
}

function onDbTypeChange() {
    var dbType = document.getElementById('dbTypeFilter').value;
    var list = allOnlineInstances;
    if (dbType) {
        list = allOnlineInstances.filter(function(i) {
            return (i.dbType || 'MySQL') === dbType;
        });
    }
    var sel = document.getElementById('instanceFilter');
    if (!list.length) {
        sel.innerHTML = '<option value="">无在线实例</option>';
        return;
    }
    sel.innerHTML = '<option value="">请选择在线实例</option>' +
        list.map(function(inst) {
            return '<option value="' + inst.id + '">' + (inst.instanceName || inst.dbName) + '</option>';
        }).join('');
    if (list.length === 1) sel.value = String(list[0].id);
}

function initEditor() {
    if (typeof CodeMirror === 'undefined') {
        showToast('SQL 编辑器加载失败，请检查网络', 'error');
        return;
    }
    var wrap = document.getElementById('sqlEditorWrap');
    sqlEditor = CodeMirror(wrap, {
        value: SQL_EDITOR_DEFAULT,
        mode: 'text/x-mysql',
        theme: 'portal-sql',
        lineNumbers: true,
        indentWithTabs: false,
        indentUnit: 4,
        tabSize: 4,
        lineWrapping: true,
        autofocus: true,
        viewportMargin: Infinity,
        extraKeys: {
            'Ctrl-Enter': function() { runSqlCurrent(); },
            'Cmd-Enter': function() { runSqlCurrent(); },
            'Tab': function(cm) {
                if (cm.somethingSelected()) cm.indentSelection('add');
                else cm.replaceSelection('    ', 'end');
            }
        }
    });

    resizeSqlEditor();
    setTimeout(resizeSqlEditor, 50);
    setTimeout(resizeSqlEditor, 300);
}

function initSqlResize() {
    var handle = document.getElementById('sqlResizeHandle');
    if (!handle) return;

    requestAnimationFrame(function() {
        applyEditorHeight(getInitialEditorHeight());
    });

    handle.addEventListener('mousedown', function(e) {
        e.preventDefault();
        var editorCard = document.getElementById('sqlEditorCard');
        if (!editorCard) return;

        var startY = e.clientY;
        var startH = editorCard.offsetHeight;
        handle.classList.add('active');
        document.body.classList.add('sql-resizing');

        function onMove(ev) {
            applyEditorHeight(startH + (ev.clientY - startY));
        }

        function onUp() {
            document.removeEventListener('mousemove', onMove);
            document.removeEventListener('mouseup', onUp);
            handle.classList.remove('active');
            document.body.classList.remove('sql-resizing');
            try {
                localStorage.setItem(SQL_EDITOR_HEIGHT_KEY, String(editorCard.offsetHeight));
            } catch (err) { /* ignore */ }
            resizeSqlEditor();
        }

        document.addEventListener('mousemove', onMove);
        document.addEventListener('mouseup', onUp);
    });

    window.addEventListener('resize', function() {
        applyEditorHeight(getCurrentEditorHeight());
    });
}

function getCurrentEditorHeight() {
    var editorCard = document.getElementById('sqlEditorCard');
    if (editorCard && editorCard.style.height) {
        return parseInt(editorCard.style.height, 10) || getInitialEditorHeight();
    }
    return getInitialEditorHeight();
}

function getInitialEditorHeight() {
    try {
        var saved = localStorage.getItem(SQL_EDITOR_HEIGHT_KEY);
        if (saved) {
            var h = parseInt(saved, 10);
            if (!isNaN(h) && h >= MIN_EDITOR_HEIGHT) return h;
        }
    } catch (err) { /* ignore */ }
    var workspace = document.getElementById('sqlWorkspace');
    if (workspace && workspace.clientHeight > 0) {
        return Math.round((workspace.clientHeight - SQL_RESIZE_HANDLE_HEIGHT) * 0.55);
    }
    return 320;
}

function applyEditorHeight(height) {
    var workspace = document.getElementById('sqlWorkspace');
    var editorCard = document.getElementById('sqlEditorCard');
    if (!workspace || !editorCard) return;

    var maxH = workspace.clientHeight - MIN_RESULT_HEIGHT - SQL_RESIZE_HANDLE_HEIGHT;
    if (maxH < MIN_EDITOR_HEIGHT) maxH = MIN_EDITOR_HEIGHT;
    height = Math.max(MIN_EDITOR_HEIGHT, Math.min(height, maxH));
    editorCard.style.height = height + 'px';
    resizeSqlEditor();
}

function resizeSqlEditor() {
    if (!sqlEditor) return;
    var container = document.querySelector('.sql-editor-body');
    if (!container) return;
    var h = container.clientHeight;
    if (h < 80) return;
    sqlEditor.setSize('100%', h);
    sqlEditor.refresh();
}

function parseSqlStatements(sql) {
    var list = [];
    var current = '';
    var start = 0;
    var inSingle = false;
    var inDouble = false;
    var inBacktick = false;
    var i = 0;

    while (i < sql.length) {
        var c = sql[i];
        var next = sql[i + 1];

        if (!inSingle && !inDouble && !inBacktick && c === '-' && next === '-') {
            while (i < sql.length && sql[i] !== '\n') {
                current += sql[i];
                i++;
            }
            continue;
        }
        if (!inSingle && !inDouble && !inBacktick && c === '/' && next === '*') {
            current += c; i++;
            current += sql[i]; i++;
            while (i < sql.length) {
                if (sql[i] === '*' && sql[i + 1] === '/') {
                    current += '*/';
                    i += 2;
                    break;
                }
                current += sql[i];
                i++;
            }
            continue;
        }

        if (c === "'" && !inDouble && !inBacktick) {
            if (inSingle && next === "'") {
                current += "''";
                i += 2;
                continue;
            }
            inSingle = !inSingle;
        } else if (c === '"' && !inSingle && !inBacktick) {
            inDouble = !inDouble;
        } else if (c === '`' && !inSingle && !inDouble) {
            inBacktick = !inBacktick;
        }

        if (c === ';' && !inSingle && !inDouble && !inBacktick) {
            var text = current.trim();
            if (text) list.push({ text: text, start: start, end: i + 1 });
            current = '';
            start = i + 1;
            i++;
            continue;
        }

        current += c;
        i++;
    }

    var last = current.trim();
    if (last) list.push({ text: last, start: start, end: sql.length });
    return list;
}

function getAllStatementsFromEditor() {
    if (!sqlEditor) return [];
    return parseSqlStatements(sqlEditor.getValue()).map(function(s) { return s.text; });
}

function getCurrentStatement() {
    if (!sqlEditor) return '';

    var sel = sqlEditor.getSelection();
    if (sel && sel.trim()) {
        var selected = parseSqlStatements(sel);
        if (selected.length >= 1) return selected[0].text;
    }

    var full = sqlEditor.getValue();
    var statements = parseSqlStatements(full);
    if (!statements.length) return full.trim();

    var cursor = sqlEditor.indexFromPos(sqlEditor.getCursor());
    for (var i = 0; i < statements.length; i++) {
        var s = statements[i];
        if (cursor >= s.start && cursor <= s.end) return s.text;
    }
    for (var j = statements.length - 1; j >= 0; j--) {
        if (cursor >= statements[j].start) return statements[j].text;
    }
    return statements[0].text;
}

function clearEditor() {
    if (sqlEditor) sqlEditor.setValue('');
    resultTabs = [];
    activeResultTab = 0;
    document.getElementById('resultMeta').textContent = '等待执行';
    document.getElementById('resultBody').innerHTML = '<div class="empty-tip">选择在线实例并编写 SQL 后执行</div>';
}

function setExecuting(executing) {
    document.getElementById('btnRunCurrent').disabled = executing;
    document.getElementById('btnRunAll').disabled = executing;
}

function showResultLoading(count) {
    document.getElementById('resultMeta').textContent = '执行中...';
    document.getElementById('resultBody').innerHTML =
        '<div class="empty-tip"><i class="fas fa-spinner fa-spin"></i> 正在执行' +
        (count > 1 ? '（共 ' + count + ' 条 SQL）' : '') + '...</div>';
}

async function executeOneSql(instanceId, sql) {
    return request('/db/instances/' + instanceId + '/execute-sql', {
        method: 'POST',
        body: JSON.stringify({ sql: sql })
    });
}

async function runSqlCurrent() {
    var instanceId = document.getElementById('instanceFilter').value;
    if (!instanceId) { showToast('请选择在线实例', 'warning'); return; }

    var sql = getCurrentStatement();
    if (!sql) { showToast('请输入 SQL', 'warning'); return; }

    setExecuting(true);
    showResultLoading(1);
    resultTabs = [];
    activeResultTab = 0;

    try {
        var result = await executeOneSql(instanceId, sql);
        if (!result) return;
        resultTabs.push(createTabState(sql, result, null));
        renderResultTabs();
    } catch (e) {
        resultTabs.push(createTabState(sql, null, e.message || '执行失败'));
        renderResultTabs();
    } finally {
        setExecuting(false);
    }
}

async function runSqlAll() {
    var instanceId = document.getElementById('instanceFilter').value;
    if (!instanceId) { showToast('请选择在线实例', 'warning'); return; }

    var statements = getAllStatementsFromEditor();
    if (!statements.length) { showToast('请输入 SQL', 'warning'); return; }

    setExecuting(true);
    showResultLoading(statements.length);
    resultTabs = [];
    activeResultTab = 0;

    try {
        for (var i = 0; i < statements.length; i++) {
            document.getElementById('resultMeta').textContent =
                '执行中... (' + (i + 1) + '/' + statements.length + ')';
            try {
                var result = await executeOneSql(instanceId, statements[i]);
                resultTabs.push(createTabState(statements[i], result || null, result ? null : '执行失败'));
            } catch (e) {
                resultTabs.push(createTabState(statements[i], null, e.message || '执行失败'));
            }
        }
        renderResultTabs();
    } finally {
        setExecuting(false);
    }
}

function createTabState(sql, result, error) {
    return { sql: sql, result: result, error: error, page: 1, pageSize: RESULT_PAGE_SIZE };
}

function tabLabel(index) {
    return '结果' + (index + 1);
}

function switchResultTab(index) {
    activeResultTab = index;
    renderResultTabs();
}

function goResultPage(tabIndex, page) {
    if (!resultTabs[tabIndex]) return;
    var totalPages = getTotalPages(resultTabs[tabIndex]);
    if (page < 1 || page > totalPages) return;
    resultTabs[tabIndex].page = page;
    renderResultTabs();
}

function changeResultPageSize(tabIndex, size) {
    if (!resultTabs[tabIndex]) return;
    resultTabs[tabIndex].pageSize = parseInt(size, 10) || RESULT_PAGE_SIZE;
    resultTabs[tabIndex].page = 1;
    renderResultTabs();
}

function getTotalPages(tab) {
    if (!tab.result || tab.result.type !== 'query') return 1;
    var total = (tab.result.rows || []).length;
    var size = tab.pageSize || RESULT_PAGE_SIZE;
    return Math.max(1, Math.ceil(total / size));
}

function renderResultTabs() {
    if (!resultTabs.length) {
        document.getElementById('resultMeta').textContent = '等待执行';
        document.getElementById('resultBody').innerHTML = '<div class="empty-tip">选择在线实例并编写 SQL 后执行</div>';
        return;
    }

    var tab = resultTabs[activeResultTab];
    updateResultMeta(tab, activeResultTab);

    var html = '';
    if (resultTabs.length > 1) {
        html += '<div class="sql-result-tabs">';
        resultTabs.forEach(function(t, idx) {
            var cls = 'sql-result-tab' + (idx === activeResultTab ? ' active' : '') + (t.error ? ' is-error' : '');
            html += '<div class="' + cls + '" onclick="switchResultTab(' + idx + ')" title="' +
                escapeHtml(t.sql) + '">' + escapeHtml(tabLabel(idx)) + '</div>';
        });
        html += '</div>';
    }

    html += '<div class="sql-result-panel active">' + renderTabContent(tab, activeResultTab) + '</div>';
    document.getElementById('resultBody').innerHTML = html;
}

function updateResultMeta(tab, index) {
    if (tab.error) {
        document.getElementById('resultMeta').textContent = '第 ' + (index + 1) + ' 条 · 执行失败';
        return;
    }
    if (!tab.result) {
        document.getElementById('resultMeta').textContent = '第 ' + (index + 1) + ' 条 · 无结果';
        return;
    }
    var duration = tab.result.duration != null ? tab.result.duration + ' ms' : '-';
    var prefix = resultTabs.length > 1 ? '第 ' + (index + 1) + ' 条 · ' : '';
    document.getElementById('resultMeta').textContent = prefix + (tab.result.message || '') + ' · 耗时 ' + duration;
}

function renderTabContent(tab, tabIndex) {
    if (tab.error) {
        return '<div class="msg-error">' + escapeHtml(tab.error) + '</div>';
    }
    if (!tab.result) {
        return '<div class="msg-error">执行失败</div>';
    }
    if (tab.result.type === 'query') {
        return renderQueryResult(tab, tabIndex);
    }
    return '<div class="msg-success">' + escapeHtml(tab.result.message || '执行成功') + '</div>';
}

function renderQueryResult(tab, tabIndex) {
    var cols = tab.result.columns || [];
    var allRows = tab.result.rows || [];
    if (!cols.length) {
        return '<div class="msg-success">查询成功，无返回列</div>';
    }

    var page = tab.page || 1;
    var pageSize = tab.pageSize || RESULT_PAGE_SIZE;
    var total = allRows.length;
    var totalPages = Math.max(1, Math.ceil(total / pageSize));
    if (page > totalPages) page = totalPages;
    tab.page = page;

    var start = (page - 1) * pageSize;
    var end = Math.min(start + pageSize, total);
    var pageRows = allRows.slice(start, end);

    var html = '<div class="sql-result-table-wrap"><table class="sql-result-table"><thead><tr>';
    cols.forEach(function(c) { html += '<th>' + escapeHtml(c) + '</th>'; });
    html += '</tr></thead><tbody>';
    if (!pageRows.length) {
        html += '<tr><td colspan="' + cols.length + '" style="text-align:center;color:#999;padding:24px;">无数据</td></tr>';
    } else {
        pageRows.forEach(function(row) {
            html += '<tr>';
            row.forEach(function(cell) {
                if (cell === null || cell === undefined) {
                    html += '<td class="null-val">NULL</td>';
                } else {
                    html += '<td>' + escapeHtml(String(cell)) + '</td>';
                }
            });
            html += '</tr>';
        });
    }
    html += '</tbody></table></div>';

    if (tab.result.truncated) {
        html += '<div class="sql-result-truncate-tip">服务端结果已截断，最多返回 500 行</div>';
    }

    html += renderResultPagination(tabIndex, total, page, pageSize, totalPages, start, end);
    return html;
}

function renderResultPagination(tabIndex, total, page, pageSize, totalPages, start, end) {
    var html = '<div class="sql-result-pagination pagination">';
    html += '<div class="pagination-info">显示 ' + (total > 0 ? start + 1 : 0) + '-' + end + ' 条，共 ' + total + ' 条 | 每页 ';
    html += '<select class="page-size-select" onchange="changeResultPageSize(' + tabIndex + ', this.value)">';
    [20, 50, 100, 200].forEach(function(n) {
        html += '<option value="' + n + '"' + (pageSize === n ? ' selected' : '') + '>' + n + '</option>';
    });
    html += '</select> 条</div>';
    html += '<div class="pagination-btns">';
    html += '<button ' + (page <= 1 ? 'disabled' : '') + ' onclick="goResultPage(' + tabIndex + ',' + (page - 1) + ')">‹</button>';

    buildPageNumbers(page, totalPages).forEach(function(p) {
        if (p === '...') {
            html += '<span style="padding:0 4px;color:#999;">...</span>';
        } else {
            html += '<button class="' + (p === page ? 'active' : '') + '" onclick="goResultPage(' + tabIndex + ',' + p + ')">' + p + '</button>';
        }
    });

    html += '<button ' + (page >= totalPages ? 'disabled' : '') + ' onclick="goResultPage(' + tabIndex + ',' + (page + 1) + ')">›</button>';
    html += '</div></div>';
    return html;
}

function buildPageNumbers(current, total) {
    if (total <= 7) {
        var arr = [];
        for (var i = 1; i <= total; i++) arr.push(i);
        return arr;
    }
    var pages = [1];
    if (current > 3) pages.push('...');
    for (var j = Math.max(2, current - 1); j <= Math.min(total - 1, current + 1); j++) pages.push(j);
    if (current < total - 2) pages.push('...');
    pages.push(total);
    return pages;
}

function escapeHtml(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
