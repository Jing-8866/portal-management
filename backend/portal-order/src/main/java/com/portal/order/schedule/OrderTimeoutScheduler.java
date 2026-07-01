package com.portal.order.schedule;

import com.portal.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderTimeoutScheduler {
    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    @Autowired
    private OrderService orderService;

    /** 每分钟扫描一次超时未支付订单 */
    @Scheduled(fixedRate = 60_000)
    public void cancelExpiredUnpaidOrders() {
        try {
            int count = orderService.cancelExpiredUnpaidOrders();
            if (count > 0) {
                log.info("自动取消超时未支付订单 {} 笔", count);
            }
        } catch (Exception e) {
            log.error("自动取消超时订单任务失败", e);
        }
    }
}
