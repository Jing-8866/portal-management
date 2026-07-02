package com.portal.dbmgmt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.portal.dbmgmt.mapper.BizDbInstanceMapper;
import com.portal.dbmgmt.mapper.BizDbTableSnapshotMapper;
import com.portal.dbmgmt.model.BizDbInstance;
import com.portal.dbmgmt.model.BizDbTableSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DbInstanceService {
    @Autowired private BizDbInstanceMapper dbMapper;
    @Autowired private BizDbTableSnapshotMapper tableSnapshotMapper;

    /**
     * 获取所有实例（纯数据库查询，不做JDBC连接）
     */
    public List<BizDbInstance> getAllInstances() {
        return dbMapper.selectList(new LambdaQueryWrapper<BizDbInstance>().orderByAsc(BizDbInstance::getId));
    }

    /**
     * 获取统计数据（从数据库已存储的数据统计）
     */
    public Map<String, Object> getDbStats() {
        return dbMapper.getDbStats();
    }

    /**
     * 进入子系统时调用：批量刷新所有实例的状态和表数量，结果写入数据库
     */
    public void refreshAllInstances() {
        List<BizDbInstance> list = dbMapper.selectList(null);
        for (BizDbInstance instance : list) {
            try {
                refreshInstanceCore(instance.getId(), true);
            } catch (RuntimeException ignored) {
                // 批量刷新时单个失败不阻断其余实例
            }
        }
    }

    /**
     * 刷新单个实例：仅检测连接状态并更新表数量（不重建表清单快照）
     */
    public BizDbInstance refreshSingleInstance(Long id) {
        return refreshInstanceCore(id, false);
    }

    private BizDbInstance refreshInstanceCore(Long id, boolean syncTableSnapshot) {
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("实例不存在");

        boolean online = checkConnectionStatus(instance);
        int newStatus = online ? 1 : 0;
        LambdaUpdateWrapper<BizDbInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BizDbInstance::getId, id).set(BizDbInstance::getStatus, newStatus);
        dbMapper.update(null, wrapper);
        instance.setStatus(newStatus);

        if (online) {
            if (syncTableSnapshot) {
                syncTableSnapshotFromSource(instance);
            } else {
                int tableCount = getRealTableCount(instance);
                if (tableCount < 0) {
                    tableCount = countSnapshotRows(instance.getInstanceName());
                }
                wrapper = new LambdaUpdateWrapper<>();
                wrapper.eq(BizDbInstance::getId, id).set(BizDbInstance::getTableCount, Math.max(tableCount, 0));
                dbMapper.update(null, wrapper);
            }
        } else {
            if (syncTableSnapshot) {
                clearTableSnapshot(instance.getInstanceName());
            }
            wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(BizDbInstance::getId, id).set(BizDbInstance::getTableCount, 0);
            dbMapper.update(null, wrapper);
        }
        return dbMapper.selectById(id);
    }

    public BizDbInstance getInstanceById(Long id) {
        return dbMapper.selectById(id);
    }

    public boolean createInstance(BizDbInstance instance) {
        normalizeAndValidateInstance(instance);
        ensureUniqueInstanceName(instance.getInstanceName(), null);
        return dbMapper.insert(instance) > 0;
    }

    public boolean updateInstance(BizDbInstance instance) {
        normalizeAndValidateInstance(instance);
        ensureUniqueInstanceName(instance.getInstanceName(), instance.getId());
        BizDbInstance stored = dbMapper.selectById(instance.getId());
        if (!StringUtils.hasText(instance.getDbPassword())) {
            if (stored != null) instance.setDbPassword(stored.getDbPassword());
        }
        boolean updated = dbMapper.updateById(instance) > 0;
        if (updated && stored != null && !stored.getInstanceName().equals(instance.getInstanceName())) {
            clearTableSnapshot(stored.getInstanceName());
        }
        return updated;
    }

    private void ensureUniqueInstanceName(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) throw new RuntimeException("实例名称不能为空");
        LambdaQueryWrapper<BizDbInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizDbInstance::getInstanceName, name.trim());
        if (excludeId != null) wrapper.ne(BizDbInstance::getId, excludeId);
        if (dbMapper.selectCount(wrapper) > 0) throw new RuntimeException("实例名称已存在，请使用其他名称");
    }

    public boolean deleteInstance(Long id) {
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance != null) {
            clearTableSnapshot(instance.getInstanceName());
        }
        return dbMapper.deleteById(id) > 0;
    }

    /**
     * 检测数据库连接状态（重试2次，每次超时3秒）
     */
    public boolean checkConnectionStatus(BizDbInstance instance) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            Connection conn = null;
            try {
                conn = getConnection(instance, 3);
                if (conn != null && !conn.isClosed()) return true;
            } catch (Exception ignored) {
            } finally {
                closeConnection(conn);
            }
        }
        return false;
    }

    /**
     * 获取真实表数量
     */
    private int getRealTableCount(BizDbInstance instance) {
        Connection conn = null;
        try {
            conn = getConnection(instance, 3);
            String dbType = normalizeDbType(instance.getDbType());
            String sql;
            if ("PostgreSQL".equals(dbType)) {
                String schema = resolvePostgreSqlConfiguredSchema(instance);
                if (StringUtils.hasText(schema)) {
                    sql = "SELECT COUNT(*) AS cnt FROM information_schema.tables WHERE table_type = 'BASE TABLE' AND table_schema = ?";
                    return querySingleInt(conn, sql, schema);
                }
                sql = "SELECT COUNT(*) AS cnt FROM information_schema.tables WHERE table_type = 'BASE TABLE' AND " + PG_USER_SCHEMA_FILTER;
                return querySingleInt(conn, sql);
            }
            if ("Oracle".equals(dbType)) {
                sql = "SELECT COUNT(*) AS cnt FROM all_tables WHERE owner = ?";
                return querySingleInt(conn, sql, resolveOracleOwner(instance).toUpperCase());
            }
            sql = "SELECT COUNT(*) AS cnt FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
            return querySingleInt(conn, sql, instance.getDbName());
        } catch (Exception ignored) {
        } finally {
            closeConnection(conn);
        }
        return -1;
    }

    /**
     * 从底表读取表清单（静态快照）
     */
    public List<Map<String, Object>> getTablesFromDb(Long id) {
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("数据库实例不存在");
        List<BizDbTableSnapshot> rows = tableSnapshotMapper.selectList(
                new LambdaQueryWrapper<BizDbTableSnapshot>()
                        .eq(BizDbTableSnapshot::getInstanceName, instance.getInstanceName())
                        .orderByAsc(BizDbTableSnapshot::getTableName));
        return rows.stream().map(this::toTableMap).collect(Collectors.toList());
    }

    /**
     * 从源库拉取表清单并写入底表（按实例名称 delete 再 insert）
     */
    public List<Map<String, Object>> refreshTableSnapshot(Long id) {
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("数据库实例不存在");
        if (instance.getStatus() == null || instance.getStatus() != 1) {
            throw new RuntimeException("实例未在线，无法刷新表清单");
        }
        syncTableSnapshotFromSource(instance);
        return getTablesFromDb(id);
    }

    private void syncTableSnapshotFromSource(BizDbInstance instance) {
        List<Map<String, Object>> sourceTables = fetchTablesFromSource(instance);
        String charset = fetchCharsetFromSource(instance);
        tableSnapshotMapper.deleteByInstanceName(instance.getInstanceName());
        LocalDateTime syncedTime = LocalDateTime.now();
        long totalBytes = 0;
        for (Map<String, Object> row : sourceTables) {
            BizDbTableSnapshot snapshot = new BizDbTableSnapshot();
            snapshot.setInstanceName(instance.getInstanceName());
            snapshot.setTableName(Objects.toString(row.get("tableName"), ""));
            Object schema = row.get("schema");
            snapshot.setSchemaName(schema != null ? schema.toString() : null);
            Object comment = row.get("comment");
            snapshot.setTableComment(comment != null ? comment.toString() : null);
            Object engine = row.get("engine");
            snapshot.setEngine(engine != null ? engine.toString() : null);
            long dataBytes = toLong(row.get("dataBytes"));
            totalBytes += dataBytes;
            snapshot.setDataBytes(dataBytes);
            Object dataLength = row.get("dataLength");
            snapshot.setDataLength(dataLength != null ? dataLength.toString() : formatSize(dataBytes));
            Object createTime = row.get("createTime");
            snapshot.setCreateTime(createTime != null ? createTime.toString() : null);
            snapshot.setSyncedTime(syncedTime);
            tableSnapshotMapper.insert(snapshot);
        }
        LambdaUpdateWrapper<BizDbInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BizDbInstance::getId, instance.getId())
                .set(BizDbInstance::getTableCount, sourceTables.size())
                .set(BizDbInstance::getStorageSize, formatSize(totalBytes))
                .set(BizDbInstance::getCharset, charset);
        dbMapper.update(null, wrapper);
    }

    private void clearTableSnapshot(String instanceName) {
        if (!StringUtils.hasText(instanceName)) return;
        tableSnapshotMapper.deleteByInstanceName(instanceName.trim());
    }

    private int countSnapshotRows(String instanceName) {
        if (!StringUtils.hasText(instanceName)) return 0;
        Long count = tableSnapshotMapper.selectCount(
                new LambdaQueryWrapper<BizDbTableSnapshot>()
                        .eq(BizDbTableSnapshot::getInstanceName, instanceName.trim()));
        return count != null ? count.intValue() : 0;
    }

    private Map<String, Object> toTableMap(BizDbTableSnapshot snapshot) {
        Map<String, Object> table = new LinkedHashMap<>();
        table.put("tableName", snapshot.getTableName());
        if (StringUtils.hasText(snapshot.getSchemaName())) {
            table.put("schema", snapshot.getSchemaName());
        }
        table.put("engine", snapshot.getEngine());
        table.put("dataLength", snapshot.getDataLength());
        table.put("createTime", snapshot.getCreateTime());
        table.put("comment", snapshot.getTableComment());
        return table;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private List<Map<String, Object>> fetchTablesFromSource(BizDbInstance instance) {
        String dbType = normalizeDbType(instance.getDbType());
        if ("PostgreSQL".equals(dbType)) return getPostgreSqlTables(instance);
        if ("Oracle".equals(dbType)) return getOracleTables(instance);
        return getMySqlTables(instance);
    }

    private String fetchCharsetFromSource(BizDbInstance instance) {
        String dbType = normalizeDbType(instance.getDbType());
        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            if ("PostgreSQL".equals(dbType)) {
                return querySingleString(conn, "SHOW server_encoding");
            }
            if ("Oracle".equals(dbType)) {
                return null;
            }
            return querySingleString(conn,
                    "SELECT DEFAULT_CHARACTER_SET_NAME AS charset FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = ?",
                    instance.getDbName());
        } catch (SQLException ignored) {
            return null;
        } finally {
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> getMySqlTables(BizDbInstance instance) {
        List<Map<String, Object>> tables = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLE STATUS");
            while (rs.next()) {
                Map<String, Object> table = new LinkedHashMap<>();
                long dataBytes = rs.getLong("Data_length") + rs.getLong("Index_length");
                table.put("tableName", rs.getString("Name"));
                table.put("engine", rs.getString("Engine"));
                table.put("dataBytes", dataBytes);
                table.put("dataLength", formatSize(dataBytes));
                table.put("indexLength", formatSize(rs.getLong("Index_length")));
                table.put("totalSize", formatSize(dataBytes));
                table.put("autoIncrement", rs.getString("Auto_increment"));
                table.put("createTime", rs.getString("Create_time"));
                table.put("collation", rs.getString("Collation"));
                table.put("comment", rs.getString("Comment"));
                tables.add(table);
            }
            rs.close(); stmt.close();
        } catch (SQLException e) {
            throw new RuntimeException("连接数据库失败: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        return tables;
    }

    private static final String PG_USER_SCHEMA_FILTER =
            "table_schema NOT IN ('pg_catalog', 'information_schema') " +
            "AND table_schema NOT LIKE 'pg_toast%' AND table_schema NOT LIKE 'pg_temp%'";

    private static final String PG_USER_NAMESPACE_FILTER =
            "n.nspname NOT IN ('pg_catalog', 'information_schema') " +
            "AND n.nspname NOT LIKE 'pg_toast%' AND n.nspname NOT LIKE 'pg_temp%'";

    private List<Map<String, Object>> getPostgreSqlTables(BizDbInstance instance) {
        List<Map<String, Object>> tables = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            String configuredSchema = resolvePostgreSqlConfiguredSchema(instance);
            String sql;
            PreparedStatement ps;
            if (StringUtils.hasText(configuredSchema)) {
                sql = "SELECT n.nspname AS table_schema, c.relname AS table_name, " +
                        "pg_total_relation_size(c.oid) AS total_bytes, obj_description(c.oid) AS table_comment " +
                        "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace " +
                        "WHERE c.relkind = 'r' AND n.nspname = ? ORDER BY c.relname";
                ps = conn.prepareStatement(sql);
                ps.setString(1, configuredSchema);
            } else {
                sql = "SELECT n.nspname AS table_schema, c.relname AS table_name, " +
                        "pg_total_relation_size(c.oid) AS total_bytes, obj_description(c.oid) AS table_comment " +
                        "FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace " +
                        "WHERE c.relkind = 'r' AND " + PG_USER_NAMESPACE_FILTER + " " +
                        "ORDER BY n.nspname, c.relname";
                ps = conn.prepareStatement(sql);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String schema = rs.getString("table_schema");
                String name = rs.getString("table_name");
                Map<String, Object> table = new LinkedHashMap<>();
                table.put("tableName", StringUtils.hasText(configuredSchema) ? name : buildPostgreSqlDisplayName(schema, name));
                table.put("schema", schema);
                table.put("engine", "PostgreSQL");
                long totalBytes = rs.getLong("total_bytes");
                table.put("dataBytes", totalBytes);
                table.put("dataLength", formatSize(totalBytes));
                table.put("totalSize", formatSize(totalBytes));
                table.put("createTime", "-");
                table.put("comment", rs.getString("table_comment"));
                tables.add(table);
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("连接数据库失败: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        return tables;
    }

    private List<Map<String, Object>> getOracleTables(BizDbInstance instance) {
        List<Map<String, Object>> tables = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            String owner = resolveOracleOwner(instance).toUpperCase();
            String sql = "SELECT t.TABLE_NAME, NVL(c.COMMENTS, '') AS TABLE_COMMENT, o.CREATED " +
                    "FROM ALL_TABLES t " +
                    "LEFT JOIN ALL_TAB_COMMENTS c ON c.OWNER = t.OWNER AND c.TABLE_NAME = t.TABLE_NAME AND c.TABLE_TYPE = 'TABLE' " +
                    "LEFT JOIN ALL_OBJECTS o ON o.OWNER = t.OWNER AND o.OBJECT_NAME = t.TABLE_NAME AND o.OBJECT_TYPE = 'TABLE' " +
                    "WHERE t.OWNER = ? ORDER BY t.TABLE_NAME";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, owner);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> table = new LinkedHashMap<>();
                table.put("tableName", rs.getString("TABLE_NAME"));
                table.put("engine", "Oracle");
                table.put("dataBytes", 0L);
                table.put("dataLength", "-");
                table.put("totalSize", "-");
                Timestamp created = rs.getTimestamp("CREATED");
                table.put("createTime", created != null ? created.toString() : "-");
                table.put("comment", rs.getString("TABLE_COMMENT"));
                tables.add(table);
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("连接数据库失败: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        return tables;
    }

    /**
     * 获取表结构（SHOW FULL COLUMNS）
     */
    public List<Map<String, Object>> getTableStructure(Long id, String tableName) {
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("数据库实例不存在");

        String dbType = normalizeDbType(instance.getDbType());
        validateTableName(tableName, dbType);
        if ("PostgreSQL".equals(dbType)) return getPostgreSqlTableStructure(instance, tableName);
        if ("Oracle".equals(dbType)) return getOracleTableStructure(instance, tableName);
        return getMySqlTableStructure(instance, tableName);
    }

    private List<Map<String, Object>> getMySqlTableStructure(BizDbInstance instance, String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW FULL COLUMNS FROM `" + tableName + "`");
            while (rs.next()) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("field", rs.getString("Field"));
                col.put("type", rs.getString("Type"));
                col.put("collation", rs.getString("Collation"));
                col.put("nullable", rs.getString("Null"));
                col.put("key", rs.getString("Key"));
                col.put("defaultValue", rs.getString("Default"));
                col.put("extra", rs.getString("Extra"));
                col.put("comment", rs.getString("Comment"));
                columns.add(col);
            }
            rs.close(); stmt.close();
        } catch (SQLException e) {
            throw new RuntimeException("查询表结构失败: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        return columns;
    }

    private List<Map<String, Object>> getPostgreSqlTableStructure(BizDbInstance instance, String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        String[] ref = resolvePostgreSqlTableRef(tableName, instance);
        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            String sql = "SELECT column_name, data_type, is_nullable, column_default, '' AS col_key, " +
                    "col_description((quote_ident(table_schema)||'.'||quote_ident(table_name))::regclass, ordinal_position) AS column_comment " +
                    "FROM information_schema.columns " +
                    "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, ref[0]);
            ps.setString(2, ref[1]);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("field", rs.getString("column_name"));
                col.put("type", rs.getString("data_type"));
                col.put("nullable", rs.getString("is_nullable"));
                col.put("key", rs.getString("col_key"));
                col.put("defaultValue", rs.getString("column_default"));
                col.put("comment", rs.getString("column_comment"));
                columns.add(col);
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("查询表结构失败: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        return columns;
    }

    private List<Map<String, Object>> getOracleTableStructure(BizDbInstance instance, String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            String owner = resolveOracleOwner(instance).toUpperCase();
            String sql = "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.NULLABLE, c.DATA_DEFAULT, " +
                    "CASE WHEN pk.COLUMN_NAME IS NOT NULL THEN 'PRI' ELSE '' END AS COL_KEY, cc.COMMENTS " +
                    "FROM ALL_TAB_COLUMNS c " +
                    "LEFT JOIN (SELECT acc.OWNER, acc.TABLE_NAME, acc.COLUMN_NAME FROM ALL_CONS_COLUMNS acc " +
                    "JOIN ALL_CONSTRAINTS ac ON ac.CONSTRAINT_NAME = acc.CONSTRAINT_NAME AND ac.OWNER = acc.OWNER " +
                    "WHERE ac.CONSTRAINT_TYPE = 'P') pk ON pk.OWNER = c.OWNER AND pk.TABLE_NAME = c.TABLE_NAME AND pk.COLUMN_NAME = c.COLUMN_NAME " +
                    "LEFT JOIN ALL_COL_COMMENTS cc ON cc.OWNER = c.OWNER AND cc.TABLE_NAME = c.TABLE_NAME AND cc.COLUMN_NAME = c.COLUMN_NAME " +
                    "WHERE c.OWNER = ? AND c.TABLE_NAME = ? ORDER BY c.COLUMN_ID";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, owner);
            ps.setString(2, tableName.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("field", rs.getString("COLUMN_NAME"));
                col.put("type", rs.getString("DATA_TYPE"));
                col.put("nullable", rs.getString("NULLABLE"));
                col.put("key", rs.getString("COL_KEY"));
                col.put("defaultValue", rs.getString("DATA_DEFAULT"));
                col.put("comment", rs.getString("COMMENTS"));
                columns.add(col);
            }
            rs.close(); ps.close();
        } catch (SQLException e) {
            throw new RuntimeException("查询表结构失败: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        return columns;
    }

    /**
     * 获取建表DDL（SHOW CREATE TABLE）
     */
    public String getTableDDL(Long id, String tableName) {
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("数据库实例不存在");

        String dbType = normalizeDbType(instance.getDbType());
        validateTableName(tableName, dbType);
        if ("PostgreSQL".equals(dbType) || "Oracle".equals(dbType)) {
            return "-- 当前数据库类型(" + dbType + ")暂不支持自动生成 DDL，请查看字段信息";
        }

        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + tableName + "`");
            if (rs.next()) {
                String ddl = rs.getString(2); // 第二列是Create Table
                rs.close(); stmt.close();
                return ddl;
            }
            rs.close(); stmt.close();
        } catch (SQLException e) {
            throw new RuntimeException("获取DDL失败: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
        return "";
    }

    /**
     * 连接实际数据库获取统计信息（只读操作）
     */
    public Map<String, Object> getDbStatsFromDb(Long id) {
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("数据库实例不存在");

        List<BizDbTableSnapshot> rows = tableSnapshotMapper.selectList(
                new LambdaQueryWrapper<BizDbTableSnapshot>()
                        .eq(BizDbTableSnapshot::getInstanceName, instance.getInstanceName()));
        long totalBytes = rows.stream()
                .mapToLong(row -> row.getDataBytes() != null ? row.getDataBytes() : 0L)
                .sum();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("tableCount", rows.size());
        stats.put("totalSize", formatSize(totalBytes));
        stats.put("charset", StringUtils.hasText(instance.getCharset()) ? instance.getCharset() : "-");
        stats.put("status", instance.getStatus() != null && instance.getStatus() == 1 ? "online" : "offline");
        return stats;
    }

    /**
     * 测试数据库连接
     */
    public boolean testConnection(BizDbInstance instance) {
        fillPasswordIfMissing(instance);
        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        } finally {
            closeConnection(conn);
        }
    }

    /** 密码为空且存在实例 ID 时，回退使用数据库已保存的密码；否则使用请求中的密码 */
    private void fillPasswordIfMissing(BizDbInstance instance) {
        if (StringUtils.hasText(instance.getDbPassword())) return;
        if (instance.getId() == null) return;
        BizDbInstance stored = dbMapper.selectById(instance.getId());
        if (stored != null && StringUtils.hasText(stored.getDbPassword())) {
            instance.setDbPassword(stored.getDbPassword());
        }
    }

    private Connection getConnection(BizDbInstance instance, int timeoutSeconds) throws SQLException {
        String dbType = normalizeDbType(instance.getDbType());
        try {
            loadJdbcDriver(dbType);
        } catch (ClassNotFoundException e) {
            throw new SQLException("缺少 " + dbType + " JDBC 驱动", e);
        }
        return DriverManager.getConnection(buildJdbcUrl(instance, dbType, timeoutSeconds),
                instance.getDbUsername(), instance.getDbPassword());
    }

    private String buildJdbcUrl(BizDbInstance instance, String dbType, int timeoutSeconds) {
        int timeoutMs = timeoutSeconds * 1000;
        switch (dbType) {
            case "PostgreSQL":
                return String.format("jdbc:postgresql://%s:%d/%s?connectTimeout=%d&socketTimeout=%d",
                        instance.getHost(), instance.getPort(), instance.getDbName(), timeoutMs, timeoutMs);
            case "Oracle":
                return String.format("jdbc:oracle:thin:@//%s:%d/%s",
                        instance.getHost(), instance.getPort(), instance.getDbName());
            case "MySQL":
            default:
                return String.format(
                        "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&connectTimeout=%d&socketTimeout=%d",
                        instance.getHost(), instance.getPort(),
                        StringUtils.hasText(instance.getDbName()) ? instance.getDbName() : "",
                        timeoutMs, timeoutMs);
        }
    }

    private void loadJdbcDriver(String dbType) throws ClassNotFoundException {
        switch (dbType) {
            case "PostgreSQL":
                Class.forName("org.postgresql.Driver");
                break;
            case "Oracle":
                Class.forName("oracle.jdbc.OracleDriver");
                break;
            case "MySQL":
            default:
                Class.forName("com.mysql.jdbc.Driver");
                break;
        }
    }

    private String normalizeDbType(String dbType) {
        if (!StringUtils.hasText(dbType)) return "MySQL";
        String type = dbType.trim();
        if ("mysql".equalsIgnoreCase(type)) return "MySQL";
        if ("oracle".equalsIgnoreCase(type)) return "Oracle";
        if ("postgresql".equalsIgnoreCase(type) || "postgres".equalsIgnoreCase(type)) return "PostgreSQL";
        throw new RuntimeException("不支持的数据库类型: " + dbType);
    }

    private void normalizeAndValidateInstance(BizDbInstance instance) {
        if (instance == null) throw new RuntimeException("实例信息不能为空");
        instance.setDbType(normalizeDbType(instance.getDbType()));
        if (!StringUtils.hasText(instance.getHost())) throw new RuntimeException("主机地址不能为空");
        if (instance.getPort() == null || instance.getPort() <= 0) throw new RuntimeException("端口不能为空");
        if (!StringUtils.hasText(instance.getDbName())) {
            throw new RuntimeException("Oracle".equals(instance.getDbType()) ? "服务名/SID 不能为空" : "数据库名不能为空");
        }
        if (!StringUtils.hasText(instance.getDbUsername())) throw new RuntimeException("用户名不能为空");
        if ("MySQL".equals(instance.getDbType())) {
            instance.setSchemaName(null);
        } else if (StringUtils.hasText(instance.getSchemaName())) {
            validateSchemaName(instance.getSchemaName().trim());
            instance.setSchemaName(instance.getSchemaName().trim());
        } else {
            instance.setSchemaName(null);
        }
    }

    private String resolveOracleOwner(BizDbInstance instance) {
        if (StringUtils.hasText(instance.getSchemaName())) {
            return instance.getSchemaName().trim();
        }
        return StringUtils.hasText(instance.getDbUsername()) ? instance.getDbUsername().trim() : instance.getDbName();
    }

    private String resolvePostgreSqlConfiguredSchema(BizDbInstance instance) {
        if (StringUtils.hasText(instance.getSchemaName())) {
            return instance.getSchemaName().trim();
        }
        return null;
    }

    private void validateSchemaName(String schemaName) {
        if (!schemaName.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("Schema 名称仅支持字母、数字、下划线");
        }
    }

    private int querySingleInt(Connection conn, String sql, String... params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        bindParams(ps, params);
        ResultSet rs = ps.executeQuery();
        int value = rs.next() ? rs.getInt(1) : 0;
        rs.close();
        ps.close();
        return value;
    }

    private long querySingleLong(Connection conn, String sql, String... params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        bindParams(ps, params);
        ResultSet rs = ps.executeQuery();
        long value = rs.next() ? rs.getLong(1) : 0L;
        rs.close();
        ps.close();
        return value;
    }

    private String querySingleString(Connection conn, String sql, String... params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        bindParams(ps, params);
        ResultSet rs = ps.executeQuery();
        String value = rs.next() ? rs.getString(1) : null;
        rs.close();
        ps.close();
        return value;
    }

    private void bindParams(PreparedStatement ps, String... params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            ps.setString(i + 1, params[i]);
        }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
    }

    /**
     * 防止 SQL 注入：校验表名格式
     */
    private void validateTableName(String tableName, String dbType) {
        if (!StringUtils.hasText(tableName)) {
            throw new RuntimeException("非法表名: " + tableName);
        }
        if ("PostgreSQL".equals(dbType)) {
            if (!tableName.matches("^[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)?$")) {
                throw new RuntimeException("非法表名: " + tableName);
            }
            return;
        }
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("非法表名: " + tableName);
        }
    }

    private String buildPostgreSqlDisplayName(String schema, String tableName) {
        if (!StringUtils.hasText(schema) || "public".equalsIgnoreCase(schema)) {
            return tableName;
        }
        return schema + "." + tableName;
    }

    private String[] resolvePostgreSqlTableRef(String tableName, BizDbInstance instance) {
        int dot = tableName.indexOf('.');
        if (dot > 0) {
            return new String[] { tableName.substring(0, dot), tableName.substring(dot + 1) };
        }
        String schema = resolvePostgreSqlConfiguredSchema(instance);
        return new String[] { StringUtils.hasText(schema) ? schema : "public", tableName };
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int idx = 0;
        double size = bytes;
        while (size >= 1024 && idx < units.length - 1) { size /= 1024; idx++; }
        return String.format("%.1f %s", size, units[idx]);
    }

    private static final int SQL_MAX_ROWS = 500;

    /**
     * 在指定在线实例上执行单条 SQL
     */
    public com.portal.dbmgmt.dto.SqlExecuteResult executeSql(Long id, String sql) {
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("实例不存在");
        if (instance.getStatus() == null || instance.getStatus() != 1) {
            throw new RuntimeException("实例未在线，无法执行 SQL");
        }
        if (!StringUtils.hasText(sql)) throw new RuntimeException("SQL 不能为空");

        sql = normalizeSql(sql);
        long start = System.currentTimeMillis();
        Connection conn = null;
        try {
            conn = getConnection(instance, 30);
            Statement stmt = conn.createStatement();
            stmt.setMaxRows(SQL_MAX_ROWS + 1);
            boolean hasResultSet = stmt.execute(sql);
            if (hasResultSet) {
                return executeQuerySql(stmt, stmt.getResultSet(), start);
            }
            return executeUpdateSql(stmt, stmt.getUpdateCount(), start);
        } catch (SQLException e) {
            throw new RuntimeException("SQL 执行失败: " + e.getMessage());
        } finally {
            closeConnection(conn);
        }
    }

    private String normalizeSql(String sql) {
        sql = sql.trim();
        if (sql.endsWith(";")) sql = sql.substring(0, sql.length() - 1).trim();
        if (sql.contains(";")) throw new RuntimeException("仅支持执行单条 SQL 语句");
        if (sql.isEmpty()) throw new RuntimeException("SQL 不能为空");
        return sql;
    }

    private com.portal.dbmgmt.dto.SqlExecuteResult executeQuerySql(Statement stmt, ResultSet rs, long start) throws SQLException {
        com.portal.dbmgmt.dto.SqlExecuteResult result = new com.portal.dbmgmt.dto.SqlExecuteResult();
        result.setType("query");
        if (rs == null) {
            result.setMessage("查询成功，无返回结果");
            result.setDuration(System.currentTimeMillis() - start);
            stmt.close();
            return result;
        }
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= colCount; i++) columns.add(meta.getColumnLabel(i));
        result.setColumns(columns);

        List<List<Object>> rows = new ArrayList<>();
        int count = 0;
        boolean truncated = false;
        while (rs.next()) {
            if (count >= SQL_MAX_ROWS) { truncated = true; break; }
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                Object val = rs.getObject(i);
                row.add(formatCellValue(val));
            }
            rows.add(row);
            count++;
        }
        rs.close();
        stmt.close();
        result.setRows(rows);
        result.setRowCount(count);
        result.setTruncated(truncated);
        result.setDuration(System.currentTimeMillis() - start);
        result.setMessage(truncated ? "查询成功（结果已截断，最多 " + SQL_MAX_ROWS + " 行）" : "查询成功，共 " + count + " 行");
        return result;
    }

    private com.portal.dbmgmt.dto.SqlExecuteResult executeUpdateSql(Statement stmt, int affected, long start) throws SQLException {
        com.portal.dbmgmt.dto.SqlExecuteResult result = new com.portal.dbmgmt.dto.SqlExecuteResult();
        result.setType("update");
        stmt.close();
        result.setAffectedRows(affected);
        result.setDuration(System.currentTimeMillis() - start);
        result.setMessage("执行成功，影响 " + affected + " 行");
        return result;
    }

    private Object formatCellValue(Object val) {
        if (val == null) return null;
        if (val instanceof byte[]) return "[BINARY]";
        return val.toString();
    }
}
