package com.portal.dbmgmt.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("biz_db_instance")
public class BizDbInstance implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String instanceName;
    private String host;
    private Integer port;
    private String dbName;
    private String schemaName;
    private String dbType;
    private String dbUsername;
    private String dbPassword;
    private String charset;
    private Integer tableCount;
    private String storageSize;
    private Integer activeConnections;
    private Integer maxConnections;
    private Integer status;
    private String description;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
