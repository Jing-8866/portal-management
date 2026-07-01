package com.portal.order.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OrderLogisticsTraceVO {
    private String title;
    private String description;
    /** 物流所在位置 */
    private String location;
    private LocalDateTime eventTime;
    /** done / active / wait / failed */
    private String state;
}
