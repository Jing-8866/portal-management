package com.portal.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.order.dto.OrderLogisticsTraceVO;
import com.portal.order.mapper.BizOrderLogisticsMapper;
import com.portal.order.model.BizOrder;
import com.portal.order.model.BizOrderLogistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OrderLogisticsService {

    private static final String WAREHOUSE = "上海市浦东新区华东仓储中心（金科路288号）";
    private static final String WAREHOUSE_PREP = "上海市浦东新区华东仓储中心 · 备货区";
    private static final String WAREHOUSE_SORT = "上海市浦东新区华东仓储中心 · 出库分拣区";
    private static final String PICKUP_STATION = "上海市浦东新区华东揽收站（张江镇）";
    private static final Pattern CITY_PATTERN = Pattern.compile("(?:.+?省)?([^省]+?市)");
    private static final Pattern DISTRICT_PATTERN = Pattern.compile("([^省市区县]+?[区县])");

    @Autowired
    private BizOrderLogisticsMapper logisticsMapper;

    /** 下单后不写入物流，付款后再展示 */
    public void initOnCheckout(BizOrder order) {
    }

    /** 订单状态变更后追加/更新物流轨迹 */
    public void onStatusChanged(BizOrder order, String fromStatus, String toStatus, LocalDateTime eventTime) {
        if (order == null || order.getId() == null) {
            return;
        }
        if ("cancelled".equals(toStatus) && order.getPayTime() == null) {
            logisticsMapper.delete(
                    new LambdaQueryWrapper<BizOrderLogistics>().eq(BizOrderLogistics::getOrderId, order.getId()));
            return;
        }
        LocalDateTime time = eventTime != null ? eventTime : LocalDateTime.now();
        clearPreviewNodes(order.getId());

        switch (toStatus) {
            case "paid":
                if (listByOrderId(order.getId()).isEmpty()) {
                    LocalDateTime created = order.getCreatedTime() != null ? order.getCreatedTime() : time;
                    insertTrace(order.getId(), "提交订单", "订单已提交，等待付款", WAREHOUSE, created, "done", 1);
                }
                appendEvent(order, "付款成功", "买家已付款，等待发货", WAREHOUSE_PREP, time, "done");
                break;
            case "cancelled":
                appendEvent(order, "订单已取消",
                        order.getPayTime() != null ? "订单已关闭" : "未付款，订单已关闭",
                        WAREHOUSE, time, "failed");
                return;
            case "shipped":
                appendEvent(order, "快件已揽收", "快递已从仓库发出", PICKUP_STATION, time, "done");
                appendEvent(order, "运输中", "快件途经转运中心", buildTransitHub(order), time, "done");
                break;
            case "completed":
                appendEvent(order, "到达派送站", "快件到达末端配送站", buildDeliveryStation(order), time, "done");
                appendEvent(order, "已签收", "买家已确认收货", receiveLocation(order), time, "done");
                return;
            case "refunding":
                appendEvent(order, "退款处理中", "等待商家处理退款申请", WAREHOUSE_PREP, time, "active");
                return;
            case "refunded":
                appendEvent(order, "已退款", "退款已完成，款项原路退回", WAREHOUSE_PREP, time, "failed");
                return;
            case "returning":
                appendEvent(order, "退货处理中", "快件退回商家仓库", buildDeliveryStation(order), time, "active");
                return;
            case "returned":
                appendEvent(order, "已退货", "商品已退回商家仓库", WAREHOUSE, time, "failed");
                return;
            default:
                break;
        }
        appendPreviewNodes(order, nextSortOrder(order.getId()));
    }

    /** 查询订单物流时间线（付款前不返回） */
    public List<OrderLogisticsTraceVO> getTimeline(BizOrder order) {
        if (order == null || order.getId() == null) {
            return new ArrayList<>();
        }
        if (!shouldExposeLogistics(order)) {
            return new ArrayList<>();
        }
        List<BizOrderLogistics> records = listByOrderId(order.getId());
        if (records.isEmpty()) {
            backfillFromOrder(order);
            records = listByOrderId(order.getId());
        }
        return records.stream().map(this::toVO).collect(Collectors.toList());
    }

    private boolean shouldExposeLogistics(BizOrder order) {
        if ("pending".equals(order.getStatus())) {
            return false;
        }
        return !("cancelled".equals(order.getStatus()) && order.getPayTime() == null);
    }

    private List<BizOrderLogistics> listByOrderId(Long orderId) {
        return logisticsMapper.selectList(
                new LambdaQueryWrapper<BizOrderLogistics>()
                        .eq(BizOrderLogistics::getOrderId, orderId)
                        .orderByAsc(BizOrderLogistics::getSortOrder)
                        .orderByAsc(BizOrderLogistics::getId));
    }

    private void backfillFromOrder(BizOrder order) {
        List<OrderLogisticsTraceVO> nodes = buildLegacyTimeline(order);
        int sort = 1;
        for (OrderLogisticsTraceVO node : nodes) {
            insertTrace(order.getId(), node.getTitle(), node.getDescription(), node.getLocation(),
                    node.getEventTime(), node.getState(), sort++);
        }
    }

    /** 历史订单无物流表数据时，按订单字段重建一条完整时间线并落库 */
    private List<OrderLogisticsTraceVO> buildLegacyTimeline(BizOrder order) {
        String status = order.getStatus();
        List<OrderLogisticsTraceVO> nodes = new ArrayList<>();
        nodes.add(trace("提交订单", "订单已提交，等待付款", WAREHOUSE, order.getCreatedTime(), "done"));

        if ("cancelled".equals(status)) {
            if (order.getPayTime() != null) {
                nodes.add(trace("付款成功", "买家已付款", WAREHOUSE_PREP, order.getPayTime(), "done"));
            }
            nodes.add(trace("订单已取消", order.getPayTime() != null ? "订单已关闭" : "未付款，订单已关闭",
                    WAREHOUSE, order.getUpdatedTime(), "failed"));
            return nodes;
        }
        if ("pending".equals(status)) {
            nodes.add(trace("待付款", "等待买家完成付款", WAREHOUSE_PREP, null, "active"));
            nodes.add(trace("商家发货", "等待商家发货", WAREHOUSE, null, "wait"));
            nodes.add(trace("确认收货", "等待买家确认收货", receiveLocation(order), null, "wait"));
            return nodes;
        }

        nodes.add(trace("付款成功", "买家已付款，等待发货", WAREHOUSE_PREP, order.getPayTime(), "done"));
        switch (status) {
            case "paid":
                nodes.add(trace("待发货", "商家正在备货打包", WAREHOUSE_SORT, null, "active"));
                nodes.add(trace("确认收货", "预计送达", receiveLocation(order), null, "wait"));
                break;
            case "refunding":
                nodes.add(trace("退款处理中", "等待商家处理退款申请", WAREHOUSE_PREP, null, "active"));
                break;
            case "refunded":
                nodes.add(trace("已退款", "退款已完成，款项原路退回", WAREHOUSE_PREP, order.getUpdatedTime(), "failed"));
                break;
            case "shipped":
                nodes.add(trace("快件已揽收", "快递已从仓库发出", PICKUP_STATION, order.getShipTime(), "done"));
                nodes.add(trace("运输中", "快件途经转运中心", buildTransitHub(order), order.getShipTime(), "done"));
                nodes.add(trace("派送中", "快递员正在派送",
                        buildDeliveryStation(order) + " → " + shortenAddress(receiveLocation(order)), null, "active"));
                break;
            case "returning":
                nodes.add(trace("快件已揽收", "商品已发出", PICKUP_STATION, order.getShipTime(), "done"));
                nodes.add(trace("退货处理中", "快件退回商家仓库", buildDeliveryStation(order), null, "active"));
                break;
            case "returned":
                nodes.add(trace("快件已揽收", "商品曾送达", buildDeliveryStation(order), order.getShipTime(), "done"));
                nodes.add(trace("已退货", "商品已退回商家仓库", WAREHOUSE,
                        order.getCompleteTime() != null ? order.getCompleteTime() : order.getUpdatedTime(), "failed"));
                break;
            case "completed":
                nodes.add(trace("快件已揽收", "快递已从仓库发出", PICKUP_STATION, order.getShipTime(), "done"));
                nodes.add(trace("运输中", "快件途经转运中心", buildTransitHub(order), order.getShipTime(), "done"));
                nodes.add(trace("到达派送站", "快件到达末端配送站", buildDeliveryStation(order), order.getShipTime(), "done"));
                nodes.add(trace("已签收", "买家已确认收货", receiveLocation(order), order.getCompleteTime(), "done"));
                break;
            default:
                break;
        }
        return nodes;
    }

    private void appendPreviewNodes(BizOrder order, int startSort) {
        String status = order.getStatus();
        int sort = startSort;
        switch (status) {
            case "pending":
                insertTrace(order.getId(), "待付款", "等待买家完成付款", WAREHOUSE_PREP, null, "active", sort++);
                insertTrace(order.getId(), "商家发货", "等待商家发货", WAREHOUSE, null, "wait", sort++);
                insertTrace(order.getId(), "确认收货", "等待买家确认收货", receiveLocation(order), null, "wait", sort);
                break;
            case "paid":
                insertTrace(order.getId(), "待发货", "商家正在备货打包", WAREHOUSE_SORT, null, "active", sort++);
                insertTrace(order.getId(), "确认收货", "预计送达", receiveLocation(order), null, "wait", sort);
                break;
            case "shipped":
                insertTrace(order.getId(), "派送中", "快递员正在派送",
                        buildDeliveryStation(order) + " → " + shortenAddress(receiveLocation(order)),
                        null, "active", sort);
                break;
            default:
                break;
        }
    }

    private void appendEvent(BizOrder order, String title, String desc, String location,
                             LocalDateTime time, String state) {
        insertTrace(order.getId(), title, desc, location, time, state, nextSortOrder(order.getId()));
    }

    private void clearPreviewNodes(Long orderId) {
        logisticsMapper.delete(
                new LambdaQueryWrapper<BizOrderLogistics>()
                        .eq(BizOrderLogistics::getOrderId, orderId)
                        .in(BizOrderLogistics::getState, "active", "wait"));
    }

    private int nextSortOrder(Long orderId) {
        List<BizOrderLogistics> list = listByOrderId(orderId);
        if (list.isEmpty()) {
            return 1;
        }
        return list.get(list.size() - 1).getSortOrder() + 1;
    }

    private void insertTrace(Long orderId, String title, String desc, String location,
                             LocalDateTime eventTime, String state, int sortOrder) {
        BizOrderLogistics row = new BizOrderLogistics();
        row.setOrderId(orderId);
        row.setTitle(title);
        row.setDescription(desc);
        row.setLocation(location);
        row.setEventTime(eventTime);
        row.setState(state);
        row.setSortOrder(sortOrder);
        logisticsMapper.insert(row);
    }

    private OrderLogisticsTraceVO toVO(BizOrderLogistics row) {
        OrderLogisticsTraceVO vo = new OrderLogisticsTraceVO();
        vo.setTitle(row.getTitle());
        vo.setDescription(row.getDescription());
        vo.setLocation(row.getLocation());
        vo.setEventTime(row.getEventTime());
        vo.setState(row.getState());
        return vo;
    }

    private OrderLogisticsTraceVO trace(String title, String desc, String location,
                                        LocalDateTime time, String state) {
        OrderLogisticsTraceVO vo = new OrderLogisticsTraceVO();
        vo.setTitle(title);
        vo.setDescription(desc);
        vo.setLocation(location);
        vo.setEventTime(time);
        vo.setState(state);
        return vo;
    }

    private String receiveLocation(BizOrder order) {
        if (StringUtils.hasText(order.getReceiverAddress())) {
            return order.getReceiverAddress();
        }
        return parseCity(order.getReceiverAddress()) + "（待填写收货地址）";
    }

    private String buildDeliveryStation(BizOrder order) {
        String destCity = parseCity(order.getReceiverAddress());
        String destDistrict = parseDistrict(order.getReceiverAddress());
        return destCity + "配送站" + (StringUtils.hasText(destDistrict) ? "（" + destDistrict + "）" : "");
    }

    private String buildTransitHub(BizOrder order) {
        return buildTransitHub(parseCity(order.getReceiverAddress()));
    }

    private String parseCity(String address) {
        if (!StringUtils.hasText(address)) {
            return "目的地";
        }
        Matcher m = CITY_PATTERN.matcher(address);
        if (m.find()) {
            return m.group(1);
        }
        return shortenAddress(address);
    }

    private String parseDistrict(String address) {
        if (!StringUtils.hasText(address)) {
            return "";
        }
        Matcher m = DISTRICT_PATTERN.matcher(address);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private String buildTransitHub(String destCity) {
        if (!StringUtils.hasText(destCity) || "目的地".equals(destCity)) {
            return "全国物流枢纽转运中心";
        }
        if (destCity.contains("上海")) {
            return "上海市青浦区华新转运中心";
        }
        if (destCity.contains("北京")) {
            return "北京市顺义区空港转运中心";
        }
        if (destCity.contains("广州") || destCity.contains("深圳")) {
            return "广东省广州市白云转运中心";
        }
        if (destCity.contains("杭州") || destCity.contains("宁波")) {
            return "浙江省杭州市萧山转运中心";
        }
        return destCity.replace("市", "") + "市物流转运中心";
    }

    private String shortenAddress(String address) {
        if (!StringUtils.hasText(address)) {
            return "";
        }
        return address.length() > 40 ? address.substring(0, 40) + "…" : address;
    }
}
