package com.portal.order.dto;

import com.portal.order.model.BizOrder;
import com.portal.order.model.BizOrderItem;
import lombok.Data;
import java.util.List;

@Data
public class OrderDetailVO {
    private BizOrder order;
    private List<BizOrderItem> items;
    private List<OrderLogisticsTraceVO> logistics;
}
