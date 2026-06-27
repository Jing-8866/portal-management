package com.portal.order.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_order")
public class BizOrder implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String customerName;
    private BigDecimal amount;
    private String status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
