package com.portal.dbmgmt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.dbmgmt.model.BizDbTableSnapshot;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BizDbTableSnapshotMapper extends BaseMapper<BizDbTableSnapshot> {

    @Delete("DELETE FROM biz_db_table_snapshot WHERE instance_name = #{instanceName}")
    int deleteByInstanceName(@Param("instanceName") String instanceName);
}
