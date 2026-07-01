package com.portal.order.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.order.dto.CartItemVO;
import com.portal.order.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Autowired
    private CartService cartService;

    @GetMapping
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    public ApiResult<List<CartItemVO>> getCart() {
        return ApiResult.success(cartService.getCartItems());
    }

    @GetMapping("/count")
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    public ApiResult<Map<String, Integer>> getCartCount() {
        Map<String, Integer> result = new java.util.HashMap<>();
        result.put("count", cartService.getCartCount());
        return ApiResult.success(result);
    }

    @PostMapping
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    @OperationLog(value = "加入购物车", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> addToCart(@RequestBody Map<String, Object> body) {
        try {
            Long productId = Long.valueOf(body.get("productId").toString());
            Integer quantity = body.get("quantity") != null ? Integer.valueOf(body.get("quantity").toString()) : 1;
            return ApiResult.success(cartService.addToCart(productId, quantity));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    @OperationLog(value = "更新购物车", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> updateCartItem(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        try {
            return ApiResult.success(cartService.updateCartItem(id, body.get("quantity")));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    @OperationLog(value = "移除购物车项", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> removeCartItem(@PathVariable Long id) {
        try {
            return ApiResult.success(cartService.removeCartItem(id));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @DeleteMapping
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    @OperationLog(value = "清空购物车", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> clearCart() {
        return ApiResult.success(cartService.clearCart());
    }
}
