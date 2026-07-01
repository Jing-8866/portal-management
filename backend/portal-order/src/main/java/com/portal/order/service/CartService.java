package com.portal.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.order.dto.CartItemVO;
import com.portal.order.mapper.BizCartItemMapper;
import com.portal.order.mapper.BizProductMapper;
import com.portal.order.model.BizCartItem;
import com.portal.order.model.BizProduct;
import com.portal.order.util.OrderSecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private BizCartItemMapper cartItemMapper;
    @Autowired
    private BizProductMapper productMapper;

    public List<CartItemVO> getCartItems() {
        Long userId = OrderSecurityHelper.getCurrentUserId();
        List<BizCartItem> items = cartItemMapper.selectList(
                new LambdaQueryWrapper<BizCartItem>().eq(BizCartItem::getUserId, userId)
                        .orderByDesc(BizCartItem::getUpdatedTime));
        if (items.isEmpty()) return new ArrayList<>();

        List<Long> productIds = items.stream().map(BizCartItem::getProductId).collect(Collectors.toList());
        Map<Long, BizProduct> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(BizProduct::getId, p -> p));

        List<CartItemVO> result = new ArrayList<>();
        for (BizCartItem item : items) {
            BizProduct product = productMap.get(item.getProductId());
            if (product == null) continue;
            CartItemVO vo = new CartItemVO();
            vo.setId(item.getId());
            vo.setProductId(product.getId());
            vo.setProductName(product.getName());
            vo.setProductPrice(product.getPrice());
            vo.setStock(product.getStock());
            vo.setStatus(product.getStatus());
            vo.setImageUrl(product.getImageUrl());
            vo.setQuantity(item.getQuantity());
            vo.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            result.add(vo);
        }
        return result;
    }

    public boolean addToCart(Long productId, Integer quantity) {
        if (quantity == null || quantity < 1) quantity = 1;
        BizProduct product = productMapper.selectById(productId);
        if (product == null) throw new RuntimeException("商品不存在");
        if (!"on_shelf".equals(product.getStatus())) throw new RuntimeException("商品已下架");
        if (product.getStock() < quantity) throw new RuntimeException("库存不足");

        Long userId = OrderSecurityHelper.getCurrentUserId();
        BizCartItem existing = cartItemMapper.selectOne(
                new LambdaQueryWrapper<BizCartItem>()
                        .eq(BizCartItem::getUserId, userId)
                        .eq(BizCartItem::getProductId, productId));
        if (existing != null) {
            int newQty = existing.getQuantity() + quantity;
            if (newQty > product.getStock()) throw new RuntimeException("库存不足");
            existing.setQuantity(newQty);
            return cartItemMapper.updateById(existing) > 0;
        }
        BizCartItem item = new BizCartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return cartItemMapper.insert(item) > 0;
    }

    public boolean updateCartItem(Long id, Integer quantity) {
        if (quantity == null || quantity < 1) throw new RuntimeException("数量无效");
        Long userId = OrderSecurityHelper.getCurrentUserId();
        BizCartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) throw new RuntimeException("购物车项不存在");
        BizProduct product = productMapper.selectById(item.getProductId());
        if (product == null || !"on_shelf".equals(product.getStatus())) throw new RuntimeException("商品已下架");
        if (product.getStock() < quantity) throw new RuntimeException("库存不足");
        item.setQuantity(quantity);
        return cartItemMapper.updateById(item) > 0;
    }

    public boolean removeCartItem(Long id) {
        Long userId = OrderSecurityHelper.getCurrentUserId();
        BizCartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) throw new RuntimeException("购物车项不存在");
        return cartItemMapper.deleteById(id) > 0;
    }

    public boolean clearCart() {
        Long userId = OrderSecurityHelper.getCurrentUserId();
        return cartItemMapper.delete(new LambdaQueryWrapper<BizCartItem>().eq(BizCartItem::getUserId, userId)) > 0;
    }

    public int getCartCount() {
        Long userId = OrderSecurityHelper.getCurrentUserId();
        List<BizCartItem> items = cartItemMapper.selectList(
                new LambdaQueryWrapper<BizCartItem>().eq(BizCartItem::getUserId, userId));
        return items.stream().mapToInt(BizCartItem::getQuantity).sum();
    }
}
