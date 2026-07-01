package com.portal.order.dto;

import lombok.Data;

@Data
public class CheckoutRequest {
    private Long addressId;
    private String customerName;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String remark;
}
