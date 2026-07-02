var DB_TYPES = ['MySQL', 'Oracle', 'PostgreSQL'];

var DB_TYPE_CONFIG = {
    MySQL: {
        port: 3306,
        hasSchema: false,
        dbNameLabel: '数据库名',
        dbNamePlaceholder: '请输入数据库名',
        hostPlaceholder: '如: localhost',
        userPlaceholder: '请输入用户名',
        hint: '连接 MySQL 实例，填写主机、端口与数据库名。'
    },
    Oracle: {
        port: 1521,
        hasSchema: true,
        schemaLabel: 'Schema',
        schemaPlaceholder: '如: SCOTT（留空则使用用户名）',
        dbNameLabel: '服务名/SID',
        dbNamePlaceholder: '如: ORCL 或 XEPDB1',
        hostPlaceholder: '如: localhost 或 scan-host',
        userPlaceholder: '请输入连接用户名',
        hint: '连接 Oracle 实例；Schema 用于限定表清单范围，留空时默认使用连接用户名。'
    },
    PostgreSQL: {
        port: 5432,
        hasSchema: true,
        schemaLabel: 'Schema',
        schemaPlaceholder: '如: public（留空则查询全部用户 Schema）',
        dbNameLabel: '数据库名',
        dbNamePlaceholder: '如: postgres',
        hostPlaceholder: '如: localhost',
        userPlaceholder: '请输入用户名',
        hint: '连接 PostgreSQL 实例；配置 Schema 后表管理仅展示该 Schema 下的表。'
    }
};

function normalizeDbType(type) {
    if (!type) return 'MySQL';
    var t = String(type).trim();
    if (t.toLowerCase() === 'mysql') return 'MySQL';
    if (t.toLowerCase() === 'oracle') return 'Oracle';
    if (t.toLowerCase() === 'postgresql' || t.toLowerCase() === 'postgres') return 'PostgreSQL';
    return t;
}

function getInstanceDbType(inst) {
    return normalizeDbType(inst && inst.dbType);
}

function collectDbTypesFromInstances(instances) {
    var types = [];
    (instances || []).forEach(function(inst) {
        var t = getInstanceDbType(inst);
        if (types.indexOf(t) < 0) types.push(t);
    });
    types.sort(function(a, b) {
        return DB_TYPES.indexOf(a) - DB_TYPES.indexOf(b);
    });
    return types;
}

function dbTypeHasSchema(dbType) {
    var cfg = DB_TYPE_CONFIG[normalizeDbType(dbType)];
    return !!(cfg && cfg.hasSchema);
}

function applyInstDbTypeForm(dbType, resetPort) {
    var type = normalizeDbType(dbType);
    var cfg = DB_TYPE_CONFIG[type] || DB_TYPE_CONFIG.MySQL;
    var dbNameLabel = document.getElementById('inst-dbname-label');
    var dbNameInput = document.getElementById('inst-dbname');
    var hostInput = document.getElementById('inst-host');
    var userInput = document.getElementById('inst-user');
    var portInput = document.getElementById('inst-port');
    var hintEl = document.getElementById('inst-type-hint');
    var schemaGroup = document.getElementById('inst-schema-group');
    var schemaLabel = document.getElementById('inst-schema-label');
    var schemaInput = document.getElementById('inst-schema');
    if (dbNameLabel) dbNameLabel.textContent = cfg.dbNameLabel + ' *';
    if (dbNameInput) dbNameInput.placeholder = cfg.dbNamePlaceholder;
    if (hostInput) hostInput.placeholder = cfg.hostPlaceholder;
    if (userInput) userInput.placeholder = cfg.userPlaceholder;
    if (hintEl) hintEl.textContent = cfg.hint;
    if (resetPort && portInput) portInput.value = cfg.port;
    if (schemaGroup) schemaGroup.style.display = cfg.hasSchema ? 'block' : 'none';
    if (schemaLabel) schemaLabel.textContent = (cfg.schemaLabel || 'Schema');
    if (schemaInput) schemaInput.placeholder = cfg.schemaPlaceholder || '请输入 Schema';
}
