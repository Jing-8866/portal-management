package com.portal.dbmgmt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.dbmgmt.model.BizDbInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.Map;

@Mapper
public interface BizDbInstanceMapper extends BaseMapper<BizDbInstance> {
    @Select("SELECT " +
            "COUNT(*) AS totalInstances, " +
            "IFNULL(SUM(table_count), 0) AS totalTables, " +
            "SUM(CASE WHEN status=1 THEN 1 ELSE 0 END) AS onlineCount " +
            "FROM biz_db_instance")
    Map<String, Object> getDbStats();
}
