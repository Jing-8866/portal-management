package com.portal.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.order.model.BizOrder;
import com.portal.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping
    public ApiResult<Page<BizOrder>> getOrderList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResult.success(orderService.getOrderList(page, size, keyword, status));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> getOrderStats() {
        return ApiResult.success(orderService.getOrderStats());
    }

    @GetMapping("/trend")
    public ApiResult<List<Map<String, Object>>> getOrderTrend() {
        return ApiResult.success(orderService.getOrderTrend());
    }

    @GetMapping("/month-stats")
    public ApiResult<Map<String, Object>> getMonthStats() {
        return ApiResult.success(orderService.getMonthStats());
    }

    @GetMapping("/{id}")
    public ApiResult<BizOrder> getOrderById(@PathVariable Long id) {
        return ApiResult.success(orderService.getOrderById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "\u521b\u5efa\u8ba2\u5355", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> createOrder(@RequestBody BizOrder order) {
        return ApiResult.success(orderService.createOrder(order));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "\u53d8\u66f4\u8ba2\u5355\u72b6\u6001", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ApiResult.success(orderService.updateOrderStatus(id, status));
    }
}
