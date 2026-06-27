package com.portal.common.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_subsystem")
public class SysSubsystem implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String systemName;
    private String systemCode;
    private String description;
    private String icon;
    private String color;
    private String url;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
