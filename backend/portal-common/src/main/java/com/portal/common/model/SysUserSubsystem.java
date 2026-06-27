package com.portal.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user_subsystem")
public class SysUserSubsystem implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long subsystemId;
    private String permissionType;
    private LocalDateTime createdTime;
}
