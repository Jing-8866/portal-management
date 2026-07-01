package com.portal.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.portal.order.mapper.BizUserAddressMapper;
import com.portal.order.model.BizUserAddress;
import com.portal.order.util.OrderSecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AddressService {
    @Autowired
    private BizUserAddressMapper addressMapper;

    public List<BizUserAddress> listMyAddresses() {
        Long userId = OrderSecurityHelper.getCurrentUserId();
        return addressMapper.selectList(new LambdaQueryWrapper<BizUserAddress>()
                .eq(BizUserAddress::getUserId, userId)
                .orderByDesc(BizUserAddress::getIsDefault)
                .orderByDesc(BizUserAddress::getUpdatedTime));
    }

    public BizUserAddress getAddressById(Long id) {
        BizUserAddress address = addressMapper.selectById(id);
        if (address == null) throw new RuntimeException("地址不存在");
        if (!address.getUserId().equals(OrderSecurityHelper.getCurrentUserId())) {
            throw new RuntimeException("无权访问该地址");
        }
        return address;
    }

    @Transactional(transactionManager = "orderTransactionManager")
    public boolean createAddress(BizUserAddress address) {
        validateAddress(address);
        address.setUserId(OrderSecurityHelper.getCurrentUserId());
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(address.getUserId());
        } else if (countByUser(address.getUserId()) == 0) {
            address.setIsDefault(1);
        } else if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }
        return addressMapper.insert(address) > 0;
    }

    @Transactional(transactionManager = "orderTransactionManager")
    public boolean updateAddress(Long id, BizUserAddress address) {
        BizUserAddress existing = getAddressById(id);
        validateAddress(address);
        address.setId(id);
        address.setUserId(existing.getUserId());
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefault(existing.getUserId());
        } else if (address.getIsDefault() == null) {
            address.setIsDefault(existing.getIsDefault());
        }
        return addressMapper.updateById(address) > 0;
    }

    @Transactional(transactionManager = "orderTransactionManager")
    public boolean deleteAddress(Long id) {
        BizUserAddress existing = getAddressById(id);
        boolean deleted = addressMapper.deleteById(id) > 0;
        if (deleted && existing.getIsDefault() != null && existing.getIsDefault() == 1) {
            List<BizUserAddress> rest = addressMapper.selectList(
                    new LambdaQueryWrapper<BizUserAddress>()
                            .eq(BizUserAddress::getUserId, existing.getUserId())
                            .orderByDesc(BizUserAddress::getUpdatedTime)
                            .last("LIMIT 1"));
            if (!rest.isEmpty()) {
                BizUserAddress next = new BizUserAddress();
                next.setId(rest.get(0).getId());
                next.setIsDefault(1);
                addressMapper.updateById(next);
            }
        }
        return deleted;
    }

    @Transactional(transactionManager = "orderTransactionManager")
    public boolean setDefaultAddress(Long id) {
        BizUserAddress address = getAddressById(id);
        clearDefault(address.getUserId());
        BizUserAddress update = new BizUserAddress();
        update.setId(id);
        update.setIsDefault(1);
        return addressMapper.updateById(update) > 0;
    }

    private void validateAddress(BizUserAddress address) {
        if (!StringUtils.hasText(address.getReceiverName())) throw new RuntimeException("请填写收货人");
        if (!StringUtils.hasText(address.getReceiverPhone())) throw new RuntimeException("请填写联系电话");
        if (!StringUtils.hasText(address.getDetailAddress())) throw new RuntimeException("请填写详细地址");
    }

    private void clearDefault(Long userId) {
        addressMapper.update(null, new LambdaUpdateWrapper<BizUserAddress>()
                .eq(BizUserAddress::getUserId, userId)
                .set(BizUserAddress::getIsDefault, 0));
    }

    private long countByUser(Long userId) {
        return addressMapper.selectCount(new LambdaQueryWrapper<BizUserAddress>()
                .eq(BizUserAddress::getUserId, userId));
    }
}
