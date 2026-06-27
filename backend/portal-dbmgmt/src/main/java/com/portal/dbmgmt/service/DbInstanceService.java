package com.portal.dbmgmt.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.portal.dbmgmt.mapper.BizDbInstanceMapper;
import com.portal.dbmgmt.model.BizDbInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;

@Service
public class DbInstanceService {
    @Autowired private BizDbInstanceMapper dbMapper;

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
            boolean online = checkConnectionStatus(instance);
            int newStatus = online ? 1 : 0;
            int tableCount = 0;
            if (online) {
                int realCount = getRealTableCount(instance);
                if (realCount >= 0) tableCount = realCount;
            }

            // 更新到数据库
            LambdaUpdateWrapper<BizDbInstance> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(BizDbInstance::getId, instance.getId())
                   .set(BizDbInstance::getStatus, newStatus)
                   .set(BizDbInstance::getTableCount, tableCount);
            dbMapper.update(null, wrapper);
        }
    }

    /**
     * 刷新单个实例状态并写入数据库
     */
    public BizDbInstance refreshSingleInstance(Long id) {
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("实例不存在");

        boolean online = checkConnectionStatus(instance);
        int newStatus = online ? 1 : 0;
        int tableCount = 0;
        if (online) {
            int realCount = getRealTableCount(instance);
            if (realCount >= 0) tableCount = realCount;
        }

        LambdaUpdateWrapper<BizDbInstance> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BizDbInstance::getId, id)
               .set(BizDbInstance::getStatus, newStatus)
               .set(BizDbInstance::getTableCount, tableCount);
        dbMapper.update(null, wrapper);

        instance.setStatus(newStatus);
        instance.setTableCount(tableCount);
        return instance;
    }

    public BizDbInstance getInstanceById(Long id) {
        return dbMapper.selectById(id);
    }

    public boolean createInstance(BizDbInstance instance) {
        ensureUniqueInstanceName(instance.getInstanceName(), null);
        return dbMapper.insert(instance) > 0;
    }

    public boolean updateInstance(BizDbInstance instance) {
        ensureUniqueInstanceName(instance.getInstanceName(), instance.getId());
        if (!StringUtils.hasText(instance.getDbPassword())) {
            BizDbInstance stored = dbMapper.selectById(instance.getId());
            if (stored != null) instance.setDbPassword(stored.getDbPassword());
        }
        return dbMapper.updateById(instance) > 0;
    }

    private void ensureUniqueInstanceName(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) throw new RuntimeException("实例名称不能为空");
        LambdaQueryWrapper<BizDbInstance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BizDbInstance::getInstanceName, name.trim());
        if (excludeId != null) wrapper.ne(BizDbInstance::getId, excludeId);
        if (dbMapper.selectCount(wrapper) > 0) throw new RuntimeException("实例名称已存在，请使用其他名称");
    }

    public boolean deleteInstance(Long id) {
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
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) AS cnt FROM information_schema.TABLES WHERE TABLE_SCHEMA = '" + instance.getDbName() + "'");
            if (rs.next()) {
                int cnt = rs.getInt("cnt");
                rs.close(); stmt.close();
                return cnt;
            }
            rs.close(); stmt.close();
        } catch (Exception ignored) {
        } finally {
            closeConnection(conn);
        }
        return -1;
    }

    /**
     * 连接实际数据库查询表清单（只读操作，不含行数）
     */
    public List<Map<String, Object>> getTablesFromDb(Long id) {
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("数据库实例不存在");

        List<Map<String, Object>> tables = new ArrayList<>();
        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SHOW TABLE STATUS");
            while (rs.next()) {
                Map<String, Object> table = new LinkedHashMap<>();
                table.put("tableName", rs.getString("Name"));
                table.put("engine", rs.getString("Engine"));
                table.put("dataLength", formatSize(rs.getLong("Data_length")));
                table.put("indexLength", formatSize(rs.getLong("Index_length")));
                table.put("totalSize", formatSize(rs.getLong("Data_length") + rs.getLong("Index_length")));
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

    /**
     * 获取表结构（SHOW FULL COLUMNS）
     */
    public List<Map<String, Object>> getTableStructure(Long id, String tableName) {
        validateTableName(tableName);
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("数据库实例不存在");

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

    /**
     * 获取建表DDL（SHOW CREATE TABLE）
     */
    public String getTableDDL(Long id, String tableName) {
        validateTableName(tableName);
        BizDbInstance instance = dbMapper.selectById(id);
        if (instance == null) throw new RuntimeException("数据库实例不存在");

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

        Map<String, Object> stats = new LinkedHashMap<>();
        Connection conn = null;
        try {
            conn = getConnection(instance, 5);
            Statement stmt = conn.createStatement();

            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) AS cnt FROM information_schema.TABLES WHERE TABLE_SCHEMA = '" + instance.getDbName() + "'");
            if (rs.next()) stats.put("tableCount", rs.getInt("cnt"));
            rs.close();

            rs = stmt.executeQuery(
                "SELECT SUM(DATA_LENGTH + INDEX_LENGTH) AS totalSize FROM information_schema.TABLES WHERE TABLE_SCHEMA = '" + instance.getDbName() + "'");
            if (rs.next()) stats.put("totalSize", formatSize(rs.getLong("totalSize")));
            rs.close();

            rs = stmt.executeQuery(
                "SELECT DEFAULT_CHARACTER_SET_NAME AS charset FROM information_schema.SCHEMATA WHERE SCHEMA_NAME = '" + instance.getDbName() + "'");
            if (rs.next()) stats.put("charset", rs.getString("charset"));
            rs.close();

            stmt.close();
            stats.put("status", "online");
        } catch (SQLException e) {
            stats.put("status", "offline");
            stats.put("error", e.getMessage());
        } finally {
            closeConnection(conn);
        }
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
        String url = String.format(
            "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&connectTimeout=%d&socketTimeout=%d",
            instance.getHost(), instance.getPort(),
            StringUtils.hasText(instance.getDbName()) ? instance.getDbName() : "",
            timeoutSeconds * 1000, timeoutSeconds * 1000);
        return DriverManager.getConnection(url, instance.getDbUsername(), instance.getDbPassword());
    }

    private void closeConnection(Connection conn) {
        if (conn != null) { try { conn.close(); } catch (SQLException ignored) {} }
    }

    /**
     * 防止SQL注入：验证表名只包含字母、数字、下划线
     */
    private void validateTableName(String tableName) {
        if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
            throw new RuntimeException("非法表名: " + tableName);
        }
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
