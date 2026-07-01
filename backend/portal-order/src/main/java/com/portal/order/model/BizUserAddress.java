package com.portal.order.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("biz_user_address")
public class BizUserAddress implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String label;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detailAddress;
    private Integer isDefault;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (province != null && !province.isEmpty()) sb.append(province);
        if (city != null && !city.isEmpty()) sb.append(city);
        if (district != null && !district.isEmpty()) sb.append(district);
        if (detailAddress != null) sb.append(detailAddress);
        return sb.toString();
    }
}
