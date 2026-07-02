package com.portal.dbmgmt.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("biz_db_table_snapshot")
public class BizDbTableSnapshot implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String instanceName;
    private String tableName;
    private String schemaName;
    private String tableComment;
    private String engine;
    private Long dataBytes;
    private String dataLength;
    private String createTime;
    private LocalDateTime syncedTime;
}
