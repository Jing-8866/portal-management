package com.portal.order.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.order.model.BizProductCategory;
import com.portal.order.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-categories")
public class ProductCategoryController {
    @Autowired
    private ProductCategoryService categoryService;

    @GetMapping
    public ApiResult<List<BizProductCategory>> listCategories(
            @RequestParam(required = false) String keyword) {
        return ApiResult.success(categoryService.listCategories(keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    public ApiResult<BizProductCategory> getCategoryById(@PathVariable Long id) {
        try {
            return ApiResult.success(categoryService.getCategoryById(id));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "新增商品分类", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> createCategory(@RequestBody BizProductCategory category) {
        try {
            return ApiResult.success(categoryService.createCategory(category));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "更新商品分类", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> updateCategory(@PathVariable Long id, @RequestBody BizProductCategory category) {
        try {
            return ApiResult.success(categoryService.updateCategory(id, category));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "删除商品分类", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> deleteCategory(@PathVariable Long id) {
        try {
            return ApiResult.success(categoryService.deleteCategory(id));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }
}
