package com.portal.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.order.model.BizOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface BizOrderMapper extends BaseMapper<BizOrder> {
    @Select("SELECT " +
            "COUNT(*) AS total, " +
            "SUM(CASE WHEN status='completed' THEN 1 ELSE 0 END) AS completed, " +
            "SUM(CASE WHEN status='pending' THEN 1 ELSE 0 END) AS pending, " +
            "SUM(CASE WHEN status='processing' THEN 1 ELSE 0 END) AS processing, " +
            "SUM(CASE WHEN status='cancelled' THEN 1 ELSE 0 END) AS cancelled " +
            "FROM biz_order")
    Map<String, Object> getOrderStats();

    @Select("SELECT DATE(created_time) AS date, COUNT(*) AS count, IFNULL(SUM(amount),0) AS total " +
            "FROM biz_order WHERE created_time >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) " +
            "GROUP BY DATE(created_time) ORDER BY date")
    List<Map<String, Object>> getOrderTrend();

    @Select("SELECT " +
            "COUNT(*) AS monthTotal, " +
            "IFNULL(SUM(amount),0) AS monthAmount, " +
            "SUM(CASE WHEN status='pending' THEN 1 ELSE 0 END) AS pendingCount, " +
            "SUM(CASE WHEN status='processing' THEN 1 ELSE 0 END) AS processingCount, " +
            "SUM(CASE WHEN status='completed' THEN 1 ELSE 0 END) AS completedCount, " +
            "SUM(CASE WHEN status='cancelled' THEN 1 ELSE 0 END) AS cancelledCount " +
            "FROM biz_order WHERE DATE_FORMAT(created_time,'%Y-%m') = DATE_FORMAT(CURDATE(),'%Y-%m')")
    Map<String, Object> getMonthStats();

    @Select("SELECT IFNULL(MAX(CAST(SUBSTRING(order_no, 14) AS UNSIGNED)),0) AS maxSeq " +
            "FROM biz_order WHERE order_no LIKE CONCAT('ORD-',#{dateStr},'-%')")
    int getMaxSeqByDate(String dateStr);
}
