package com.portal.order.controller;

import com.portal.common.annotation.OperationLog;
import com.portal.common.dto.ApiResult;
import com.portal.order.model.BizUserAddress;
import com.portal.order.service.AddressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    @Autowired
    private AddressService addressService;

    @GetMapping
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    public ApiResult<List<BizUserAddress>> listAddresses() {
        return ApiResult.success(addressService.listMyAddresses());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    public ApiResult<BizUserAddress> getAddress(@PathVariable Long id) {
        try {
            return ApiResult.success(addressService.getAddressById(id));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PostMapping
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    @OperationLog(value = "新增收货地址", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> createAddress(@RequestBody BizUserAddress address) {
        try {
            return ApiResult.success(addressService.createAddress(address));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    @OperationLog(value = "更新收货地址", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> updateAddress(@PathVariable Long id, @RequestBody BizUserAddress address) {
        try {
            return ApiResult.success(addressService.updateAddress(id, address));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    @OperationLog(value = "删除收货地址", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> deleteAddress(@PathVariable Long id) {
        try {
            return ApiResult.success(addressService.deleteAddress(id));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/default")
    @PreAuthorize("@subsystemAuth.hasLogin('ORDER_MGMT')")
    @OperationLog(value = "设置默认地址", subsystem = "ORDER_MGMT")
    public ApiResult<Boolean> setDefault(@PathVariable Long id) {
        try {
            return ApiResult.success(addressService.setDefaultAddress(id));
        } catch (RuntimeException e) {
            return ApiResult.error(e.getMessage());
        }
    }
}
