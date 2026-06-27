package com.portal.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.order.mapper.BizOrderMapper;
import com.portal.order.model.BizOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    @Autowired
    private BizOrderMapper orderMapper;

    public Page<BizOrder> getOrderList(int page, int size, String keyword, String status) {
        LambdaQueryWrapper<BizOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(BizOrder::getOrderNo, keyword).or().like(BizOrder::getCustomerName, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizOrder::getStatus, status);
        }
        wrapper.orderByDesc(BizOrder::getCreatedTime);
        return orderMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> getOrderStats() {
        return orderMapper.getOrderStats();
    }

    public BizOrder getOrderById(Long id) { return orderMapper.selectById(id); }

    public boolean createOrder(BizOrder order) {
        // 自动生成订单号 ORD-yyyyMMdd-XXX
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int maxSeq = orderMapper.getMaxSeqByDate(dateStr);
        String orderNo = String.format("ORD-%s-%03d", dateStr, maxSeq + 1);
        order.setOrderNo(orderNo);
        if (order.getStatus() == null) order.setStatus("pending");
        return orderMapper.insert(order) > 0;
    }

    public boolean updateOrderStatus(Long id, String status) {
        BizOrder order = new BizOrder();
        order.setId(id);
        order.setStatus(status);
        return orderMapper.updateById(order) > 0;
    }

    public List<Map<String, Object>> getOrderTrend() {
        return orderMapper.getOrderTrend();
    }

    public Map<String, Object> getMonthStats() {
        return orderMapper.getMonthStats();
    }
}
