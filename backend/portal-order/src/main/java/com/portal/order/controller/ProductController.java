package com.portal.order.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.order.model.BizProduct;
import com.portal.order.service.ProductImageService;
import com.portal.order.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    @Autowired
    private ProductService productService;
    @Autowired
    private ProductImageService productImageService;

    @GetMapping
    public ApiResult<List<BizProduct>> getProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        return ApiResult.success(productService.getProductList(keyword, status, category));
    }

    @GetMapping("/categories")
    public ApiResult<java.util.List<String>> listCategories() {
        return ApiResult.success(productService.listCategories());
    }

    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "上传商品图片", subsystem = "ORDER_MGMT")
    public ApiResult<Map<String, String>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = productImageService.saveImage(file);
            Map<String, String> result = new HashMap<>();
            result.put("url", url);
            return ApiResult.success(result);
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        } catch (Exception e) {
            return ApiResult.error("图片上传失败");
        }
    }

    @GetMapping("/{id}")
    public ApiResult<BizProduct> getProductById(@PathVariable Long id) {
        return ApiResult.success(productService.getProductById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "创建商品", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> createProduct(@RequestBody BizProduct product) {
        try {
            return ApiResult.success(productService.createProduct(product));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "更新商品", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> updateProduct(@PathVariable Long id, @RequestBody BizProduct product) {
        try {
            return ApiResult.success(productService.updateProduct(id, product));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "变更商品状态", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> updateProductStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            return ApiResult.success(productService.updateProductStatus(id, status));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN','SUBSYSTEM_ADMIN')")
    @OperationLog(value = "删除商品", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> deleteProduct(@PathVariable Long id) {
        return ApiResult.success(productService.deleteProduct(id));
    }
}
