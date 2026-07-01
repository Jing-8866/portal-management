package com.portal.order.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("biz_order_logistics")
public class BizOrderLogistics implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private String title;
    private String description;
    /** 物流所在位置 */
    private String location;
    /** 发生时间，未发生节点可为空 */
    private LocalDateTime eventTime;
    /** done / active / wait / failed */
    private String state;
    private Integer sortOrder;
    private LocalDateTime createdTime;
}
