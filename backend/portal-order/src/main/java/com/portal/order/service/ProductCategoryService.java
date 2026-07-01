package com.portal.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.portal.order.mapper.BizProductCategoryMapper;
import com.portal.order.mapper.BizProductMapper;
import com.portal.order.model.BizProduct;
import com.portal.order.model.BizProductCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductCategoryService {
    @Autowired
    private BizProductCategoryMapper categoryMapper;
    @Autowired
    private BizProductMapper productMapper;

    public List<BizProductCategory> listCategories(String keyword) {
        LambdaQueryWrapper<BizProductCategory> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(BizProductCategory::getName, keyword.trim());
        }
        wrapper.orderByAsc(BizProductCategory::getSortOrder).orderByAsc(BizProductCategory::getId);
        return categoryMapper.selectList(wrapper);
    }

    public List<String> listCategoryNames() {
        return listCategories(null).stream()
                .map(BizProductCategory::getName)
                .collect(Collectors.toList());
    }

    public BizProductCategory getCategoryById(Long id) {
        BizProductCategory category = categoryMapper.selectById(id);
        if (category == null) throw new RuntimeException("分类不存在");
        return category;
    }

    public void validateCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return;
        }
        Long count = categoryMapper.selectCount(new LambdaQueryWrapper<BizProductCategory>()
                .eq(BizProductCategory::getName, category.trim()));
        if (count == null || count == 0) {
            throw new RuntimeException("无效的商品分类: " + category);
        }
    }

    public boolean createCategory(BizProductCategory category) {
        if (!StringUtils.hasText(category.getName())) {
            throw new RuntimeException("分类名称不能为空");
        }
        category.setName(category.getName().trim());
        if (category.getSortOrder() == null) category.setSortOrder(0);
        Long exists = categoryMapper.selectCount(new LambdaQueryWrapper<BizProductCategory>()
                .eq(BizProductCategory::getName, category.getName()));
        if (exists != null && exists > 0) {
            throw new RuntimeException("分类名称已存在");
        }
        return categoryMapper.insert(category) > 0;
    }

    @Transactional(transactionManager = "orderTransactionManager")
    public boolean updateCategory(Long id, BizProductCategory category) {
        BizProductCategory existing = getCategoryById(id);
        if (!StringUtils.hasText(category.getName())) {
            throw new RuntimeException("分类名称不能为空");
        }
        String newName = category.getName().trim();
        if (!newName.equals(existing.getName())) {
            Long exists = categoryMapper.selectCount(new LambdaQueryWrapper<BizProductCategory>()
                    .eq(BizProductCategory::getName, newName)
                    .ne(BizProductCategory::getId, id));
            if (exists != null && exists > 0) {
                throw new RuntimeException("分类名称已存在");
            }
            productMapper.update(null, new LambdaUpdateWrapper<BizProduct>()
                    .eq(BizProduct::getCategory, existing.getName())
                    .set(BizProduct::getCategory, newName));
        }
        BizProductCategory update = new BizProductCategory();
        update.setId(id);
        update.setName(newName);
        if (category.getSortOrder() != null) {
            update.setSortOrder(category.getSortOrder());
        } else {
            update.setSortOrder(existing.getSortOrder());
        }
        return categoryMapper.updateById(update) > 0;
    }

    public boolean deleteCategory(Long id) {
        BizProductCategory existing = getCategoryById(id);
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<BizProduct>()
                .eq(BizProduct::getCategory, existing.getName()));
        if (productCount != null && productCount > 0) {
            throw new RuntimeException("该分类下仍有 " + productCount + " 个商品，无法删除");
        }
        return categoryMapper.deleteById(id) > 0;
    }

    public long countProductsByCategoryName(String categoryName) {
        if (!StringUtils.hasText(categoryName)) return 0;
        Long count = productMapper.selectCount(new LambdaQueryWrapper<BizProduct>()
                .eq(BizProduct::getCategory, categoryName));
        return count == null ? 0 : count;
    }
}
