package com.portal.order.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.order.dto.CheckoutRequest;
import com.portal.order.dto.OrderDetailVO;
import com.portal.order.model.BizOrder;
import com.portal.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @GetMapping
    public ApiResult<List<BizOrder>> getOrderList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "mine") String scope) {
        return ApiResult.success(orderService.getOrderList(keyword, status, startDate, endDate, scope));
    }

    @GetMapping("/stats")
    public ApiResult<Map<String, Object>> getOrderStats(
            @RequestParam(defaultValue = "mine") String scope) {
        return ApiResult.success(orderService.getOrderStats(scope));
    }

    @GetMapping("/settings")
    public ApiResult<Map<String, Object>> getOrderSettings() {
        return ApiResult.success(orderService.getOrderSettings());
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
    public ApiResult<OrderDetailVO> getOrderById(@PathVariable Long id) {
        try {
            return ApiResult.success(orderService.getOrderDetail(id));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PostMapping("/checkout")
    @OperationLog(value = "提交订单", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> checkout(@RequestBody CheckoutRequest request) {
        try {
            return ApiResult.success(orderService.checkout(request));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @OperationLog(value = "变更订单状态", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> updateStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            return ApiResult.success(orderService.updateOrderStatus(id, status));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/reorder-to-cart")
    @OperationLog(value = "重新加入购物车", subsystem = "ORDER_MGMT")
    public ApiResult<Map<String, Object>> reorderToCart(@PathVariable Long id) {
        try {
            return ApiResult.success(orderService.reorderToCart(id));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }
}
