package com.portal.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartItemVO {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal productPrice;
    private Integer stock;
    private String status;
    private String imageUrl;
    private Integer quantity;
    private BigDecimal subtotal;
}
