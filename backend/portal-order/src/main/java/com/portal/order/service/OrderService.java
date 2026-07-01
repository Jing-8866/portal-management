package com.portal.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.order.dto.CheckoutRequest;
import com.portal.order.dto.OrderDetailVO;
import com.portal.order.mapper.*;
import com.portal.order.model.*;
import com.portal.order.util.OrderSecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {
    @Autowired
    private BizOrderMapper orderMapper;
    @Autowired
    private BizOrderItemMapper orderItemMapper;
    @Autowired
    private BizCartItemMapper cartItemMapper;
    @Autowired
    private BizProductMapper productMapper;
    @Autowired
    private AddressService addressService;
    @Autowired
    private OrderLogisticsService orderLogisticsService;

    @Value("${order.payment-timeout-minutes:30}")
    private int paymentTimeoutMinutes;

    public List<BizOrder> getOrderList(String keyword, String status,
                                       String startDate, String endDate, String scope) {
        LambdaQueryWrapper<BizOrder> wrapper = buildOrderQuery(keyword, status, startDate, endDate);
        boolean viewAll = "all".equals(scope) && OrderSecurityHelper.isAdmin();
        if (!viewAll) {
            wrapper.eq(BizOrder::getUserId, OrderSecurityHelper.getCurrentUserId());
        }
        wrapper.orderByDesc(BizOrder::getCreatedTime);
        return orderMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<BizOrder> buildOrderQuery(String keyword, String status,
                                                         String startDate, String endDate) {
        LambdaQueryWrapper<BizOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(BizOrder::getOrderNo, keyword)
                    .or().like(BizOrder::getCustomerName, keyword)
                    .or().like(BizOrder::getReceiverName, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizOrder::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(BizOrder::getCreatedTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.lt(BizOrder::getCreatedTime, LocalDate.parse(endDate).plusDays(1).atStartOfDay());
        }
        return wrapper;
    }

    public Map<String, Object> getOrderStats(String scope) {
        if ("all".equals(scope) && OrderSecurityHelper.isAdmin()) {
            return orderMapper.getOrderStats();
        }
        Long userId = OrderSecurityHelper.getCurrentUserId();
        List<BizOrder> orders = orderMapper.selectList(
                new LambdaQueryWrapper<BizOrder>().eq(BizOrder::getUserId, userId));
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", orders.size());
        stats.put("pending", countByStatus(orders, "pending"));
        stats.put("paid", countByStatus(orders, "paid"));
        stats.put("shipped", countByStatus(orders, "shipped"));
        stats.put("completed", countByStatus(orders, "completed"));
        stats.put("cancelled", countByStatus(orders, "cancelled"));
        stats.put("refund", countByStatuses(orders, "refunding", "refunded"));
        stats.put("returning", countByStatuses(orders, "returning", "returned"));
        return stats;
    }

    private long countByStatus(List<BizOrder> orders, String status) {
        return orders.stream().filter(o -> status.equals(o.getStatus())).count();
    }

    private long countByStatuses(List<BizOrder> orders, String... statuses) {
        Set<String> set = new HashSet<>(Arrays.asList(statuses));
        return orders.stream().filter(o -> set.contains(o.getStatus())).count();
    }

    public OrderDetailVO getOrderDetail(Long id) {
        BizOrder order = getOrderWithAccess(id);
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrder(order);
        vo.setItems(orderItemMapper.selectList(
                new LambdaQueryWrapper<BizOrderItem>().eq(BizOrderItem::getOrderId, id)));
        vo.setLogistics(orderLogisticsService.getTimeline(order));
        return vo;
    }

    public BizOrder getOrderById(Long id) {
        return getOrderWithAccess(id);
    }

    private BizOrder getOrderWithAccess(Long id) {
        BizOrder order = orderMapper.selectById(id);
        if (order == null) throw new RuntimeException("订单不存在");
        expireOrderIfNeeded(order);
        order = orderMapper.selectById(id);
        if (!OrderSecurityHelper.isAdmin()) {
            Long userId = OrderSecurityHelper.getCurrentUserId();
            if (order.getUserId() == null || !order.getUserId().equals(userId)) {
                throw new RuntimeException("无权查看该订单");
            }
        }
        return order;
    }

    /**
     * 取消超过支付时限仍未付款的待支付订单，并回补库存。
     */
    @Transactional(transactionManager = "orderTransactionManager")
    public int cancelExpiredUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(paymentTimeoutMinutes);
        List<BizOrder> expiredOrders = orderMapper.selectList(
                new LambdaQueryWrapper<BizOrder>()
                        .eq(BizOrder::getStatus, "pending")
                        .lt(BizOrder::getCreatedTime, deadline));
        int count = 0;
        for (BizOrder order : expiredOrders) {
            if (cancelPendingOrderInternal(order.getId())) {
                count++;
            }
        }
        return count;
    }

    private void expireOrderIfNeeded(BizOrder order) {
        if (order == null || !"pending".equals(order.getStatus())) {
            return;
        }
        if (order.getCreatedTime() != null
                && order.getCreatedTime().isBefore(LocalDateTime.now().minusMinutes(paymentTimeoutMinutes))) {
            cancelPendingOrderInternal(order.getId());
        }
    }

    private boolean cancelPendingOrderInternal(Long orderId) {
        BizOrder order = orderMapper.selectById(orderId);
        if (order == null || !"pending".equals(order.getStatus())) {
            return false;
        }
        restoreStock(orderId);
        BizOrder update = new BizOrder();
        update.setId(orderId);
        update.setStatus("cancelled");
        boolean ok = orderMapper.updateById(update) > 0;
        if (ok) {
            BizOrder cancelled = orderMapper.selectById(orderId);
            orderLogisticsService.onStatusChanged(cancelled, "pending", "cancelled", LocalDateTime.now());
        }
        return ok;
    }

    @Transactional(transactionManager = "orderTransactionManager")
    public boolean checkout(CheckoutRequest request) {
        Long userId = OrderSecurityHelper.getCurrentUserId();
        List<BizCartItem> cartItems = cartItemMapper.selectList(
                new LambdaQueryWrapper<BizCartItem>().eq(BizCartItem::getUserId, userId));
        if (cartItems.isEmpty()) throw new RuntimeException("购物车为空");

        String receiverName = request.getReceiverName();
        String receiverPhone = request.getReceiverPhone();
        String receiverAddress = request.getReceiverAddress();
        if (request.getAddressId() != null) {
            BizUserAddress address = addressService.getAddressById(request.getAddressId());
            receiverName = address.getReceiverName();
            receiverPhone = address.getReceiverPhone();
            receiverAddress = address.getFullAddress();
        }
        if (!StringUtils.hasText(receiverName)) throw new RuntimeException("请填写收货人");
        if (!StringUtils.hasText(receiverPhone)) throw new RuntimeException("请填写联系电话");
        if (!StringUtils.hasText(receiverAddress)) throw new RuntimeException("请填写收货地址");

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BizOrderItem> orderItems = new ArrayList<>();

        for (BizCartItem cartItem : cartItems) {
            BizProduct product = productMapper.selectById(cartItem.getProductId());
            if (product == null) throw new RuntimeException("商品不存在: " + cartItem.getProductId());
            if (!"on_shelf".equals(product.getStatus())) throw new RuntimeException("商品已下架: " + product.getName());
            if (product.getStock() < cartItem.getQuantity()) throw new RuntimeException("库存不足: " + product.getName());

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            BizOrderItem item = new BizOrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductPrice(product.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setSubtotal(subtotal);
            orderItems.add(item);

            product.setStock(product.getStock() - cartItem.getQuantity());
            productMapper.updateById(product);
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int maxSeq = orderMapper.getMaxSeqByDate(dateStr);
        String orderNo = String.format("ORD-%s-%03d", dateStr, maxSeq + 1);

        BizOrder order = new BizOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setCreatedBy(userId);
        order.setCustomerName(StringUtils.hasText(request.getCustomerName()) ? request.getCustomerName() : receiverName);
        order.setReceiverName(receiverName);
        order.setReceiverPhone(receiverPhone);
        order.setReceiverAddress(receiverAddress);
        order.setAmount(totalAmount);
        order.setStatus("pending");
        order.setRemark(request.getRemark());
        orderMapper.insert(order);

        for (BizOrderItem item : orderItems) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }

        orderLogisticsService.initOnCheckout(order);

        cartItemMapper.delete(new LambdaQueryWrapper<BizCartItem>().eq(BizCartItem::getUserId, userId));
        return true;
    }

    @Transactional(transactionManager = "orderTransactionManager")
    public boolean updateOrderStatus(Long id, String newStatus) {
        BizOrder order = getOrderWithAccess(id);
        String current = order.getStatus();
        boolean admin = OrderSecurityHelper.isAdmin();

        if ("paid".equals(newStatus) && "cancelled".equals(current)) {
            throw new RuntimeException("订单已超时自动取消，请重新下单");
        }
        validateStatusTransition(current, newStatus, admin);

        BizOrder update = new BizOrder();
        update.setId(id);
        update.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        if ("paid".equals(newStatus)) update.setPayTime(now);
        if ("shipped".equals(newStatus)) update.setShipTime(now);
        if ("completed".equals(newStatus) || "returned".equals(newStatus)) update.setCompleteTime(now);

        if ("cancelled".equals(newStatus) && "pending".equals(current)) {
            restoreStock(id);
        }
        if ("refunded".equals(newStatus)) {
            restoreStock(id);
        }

        orderMapper.updateById(update);
        BizOrder updated = orderMapper.selectById(id);
        orderLogisticsService.onStatusChanged(updated, current, newStatus, now);
        return true;
    }

    private void validateStatusTransition(String current, String newStatus, boolean admin) {
        Map<String, Set<String>> userTransitions = new HashMap<>();
        userTransitions.put("pending", new HashSet<>(Arrays.asList("paid", "cancelled")));
        userTransitions.put("paid", new HashSet<>(Collections.singletonList("refunding")));
        userTransitions.put("shipped", new HashSet<>(Arrays.asList("completed", "refunding", "returning")));
        userTransitions.put("completed", new HashSet<>(Arrays.asList("returning", "refunding")));

        Map<String, Set<String>> adminTransitions = new HashMap<>();
        adminTransitions.put("pending", new HashSet<>(Arrays.asList("paid", "cancelled")));
        adminTransitions.put("paid", new HashSet<>(Arrays.asList("shipped", "cancelled", "refunding", "refunded")));
        adminTransitions.put("shipped", new HashSet<>(Arrays.asList("completed", "refunding", "returning")));
        adminTransitions.put("completed", new HashSet<>(Arrays.asList("refunding", "returning")));
        adminTransitions.put("refunding", new HashSet<>(Arrays.asList("refunded", "paid", "shipped", "completed")));
        adminTransitions.put("returning", new HashSet<>(Arrays.asList("returned", "completed", "shipped")));
        adminTransitions.put("cancelled", new HashSet<>());
        adminTransitions.put("refunded", new HashSet<>());
        adminTransitions.put("returned", new HashSet<>());

        Set<String> allowed = admin ? adminTransitions.getOrDefault(current, new HashSet<>())
                : userTransitions.getOrDefault(current, new HashSet<>());
        if (!allowed.contains(newStatus)) {
            throw new RuntimeException("不允许的状态变更: " + current + " -> " + newStatus);
        }
        if (!admin && ("shipped".equals(newStatus) || "refunded".equals(newStatus) || "returned".equals(newStatus))) {
            throw new RuntimeException("无权执行该操作");
        }
    }

    private void restoreStock(Long orderId) {
        List<BizOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<BizOrderItem>().eq(BizOrderItem::getOrderId, orderId));
        for (BizOrderItem item : items) {
            BizProduct product = productMapper.selectById(item.getProductId());
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productMapper.updateById(product);
            }
        }
    }

    public List<Map<String, Object>> getOrderTrend() {
        return orderMapper.getOrderTrend();
    }

    public Map<String, Object> getMonthStats() {
        return orderMapper.getMonthStats();
    }

    public Map<String, Object> getOrderSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("paymentTimeoutMinutes", paymentTimeoutMinutes);
        return settings;
    }

    /**
     * 将已取消订单的商品重新加入购物车（仅订单所属用户可操作）。
     */
    @Transactional(transactionManager = "orderTransactionManager")
    public Map<String, Object> reorderToCart(Long orderId) {
        BizOrder order = getOrderWithAccess(orderId);
        if (!"cancelled".equals(order.getStatus())) {
            throw new RuntimeException("仅已取消的订单可重新加入购物车");
        }
        Long userId = OrderSecurityHelper.getCurrentUserId();
        if (order.getUserId() == null || !order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该订单");
        }

        List<BizOrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<BizOrderItem>().eq(BizOrderItem::getOrderId, orderId));
        if (items.isEmpty()) {
            throw new RuntimeException("订单无商品");
        }

        int addedCount = 0;
        List<String> skipped = new ArrayList<>();
        for (BizOrderItem item : items) {
            String productName = StringUtils.hasText(item.getProductName()) ? item.getProductName() : "未知商品";
            if (item.getProductId() == null) {
                skipped.add(productName + "：商品已不存在");
                continue;
            }
            BizProduct product = productMapper.selectById(item.getProductId());
            if (product == null) {
                skipped.add(productName + "：商品已不存在");
                continue;
            }
            if (!"on_shelf".equals(product.getStatus())) {
                skipped.add(productName + "：商品已下架");
                continue;
            }
            int qty = item.getQuantity() != null && item.getQuantity() > 0 ? item.getQuantity() : 1;
            BizCartItem existing = cartItemMapper.selectOne(
                    new LambdaQueryWrapper<BizCartItem>()
                            .eq(BizCartItem::getUserId, userId)
                            .eq(BizCartItem::getProductId, item.getProductId()));
            int targetQty = existing != null ? existing.getQuantity() + qty : qty;
            if (targetQty > product.getStock()) {
                skipped.add(productName + "：库存不足（需 " + qty + "，可用 " + product.getStock() + "）");
                continue;
            }
            if (existing != null) {
                existing.setQuantity(targetQty);
                cartItemMapper.updateById(existing);
            } else {
                BizCartItem cartItem = new BizCartItem();
                cartItem.setUserId(userId);
                cartItem.setProductId(item.getProductId());
                cartItem.setQuantity(qty);
                cartItemMapper.insert(cartItem);
            }
            addedCount++;
        }

        if (addedCount == 0) {
            throw new RuntimeException(String.join("；", skipped));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("addedCount", addedCount);
        result.put("skipped", skipped);
        return result;
    }
}
