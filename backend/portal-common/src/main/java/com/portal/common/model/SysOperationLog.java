package com.portal.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String subsystemCode;
    private String level;
    private String operation;
    private String method;
    private String params;
    private String ip;
    private Integer status;
    private String errorMsg;
    private Long duration;
    private LocalDateTime createdTime;
}
