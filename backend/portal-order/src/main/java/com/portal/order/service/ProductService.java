package com.portal.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.order.mapper.BizProductMapper;
import com.portal.order.model.BizProduct;
import com.portal.order.util.OrderSecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class ProductService {
    @Autowired
    private BizProductMapper productMapper;
    @Autowired
    private ProductCategoryService categoryService;

    public List<BizProduct> getProductList(String keyword, String status, String category) {
        LambdaQueryWrapper<BizProduct> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(BizProduct::getName, keyword)
                    .or().like(BizProduct::getDescription, keyword)
                    .or().like(BizProduct::getCategory, keyword));
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(BizProduct::getCategory, category);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(BizProduct::getStatus, status);
        } else if (!OrderSecurityHelper.isAdmin()) {
            wrapper.eq(BizProduct::getStatus, "on_shelf");
        }
        wrapper.orderByDesc(BizProduct::getCreatedTime);
        return productMapper.selectList(wrapper);
    }

    public BizProduct getProductById(Long id) {
        BizProduct product = productMapper.selectById(id);
        if (product == null) throw new RuntimeException("商品不存在");
        if (!OrderSecurityHelper.isAdmin() && !"on_shelf".equals(product.getStatus())) {
            throw new RuntimeException("商品已下架");
        }
        return product;
    }

    public boolean createProduct(BizProduct product) {
        if (!StringUtils.hasText(product.getName())) throw new RuntimeException("商品名称不能为空");
        if (product.getPrice() == null) throw new RuntimeException("商品价格不能为空");
        if (product.getStock() == null) product.setStock(0);
        if (product.getStatus() == null) product.setStatus("on_shelf");
        if (!StringUtils.hasText(product.getCategory())) throw new RuntimeException("请选择商品分类");
        product.setCategory(product.getCategory().trim());
        categoryService.validateCategory(product.getCategory());
        product.setCreatedBy(OrderSecurityHelper.getCurrentUserId());
        return productMapper.insert(product) > 0;
    }

    public boolean updateProduct(Long id, BizProduct product) {
        BizProduct existing = productMapper.selectById(id);
        if (existing == null) throw new RuntimeException("商品不存在");
        if (StringUtils.hasText(product.getCategory())) {
            product.setCategory(product.getCategory().trim());
            categoryService.validateCategory(product.getCategory());
        } else if (product.getCategory() != null && product.getCategory().isEmpty()) {
            throw new RuntimeException("请选择商品分类");
        }
        product.setId(id);
        return productMapper.updateById(product) > 0;
    }

    public boolean updateProductStatus(Long id, String status) {
        if (!"on_shelf".equals(status) && !"off_shelf".equals(status)) {
            throw new RuntimeException("无效的商品状态");
        }
        BizProduct product = new BizProduct();
        product.setId(id);
        product.setStatus(status);
        return productMapper.updateById(product) > 0;
    }

    public boolean deleteProduct(Long id) {
        return productMapper.deleteById(id) > 0;
    }

    public List<String> listCategories() {
        return categoryService.listCategoryNames();
    }
}
