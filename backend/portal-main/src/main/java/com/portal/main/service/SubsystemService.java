package com.portal.main.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.portal.common.model.SysSubsystem;
import com.portal.main.mapper.SysSubsystemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SubsystemService {
    @Autowired
    private SysSubsystemMapper subsystemMapper;

    /** 获取启用的子系统（普通用户/下拉选项用） */
    public List<SysSubsystem> getAllSubsystems() {
        return subsystemMapper.selectList(new LambdaQueryWrapper<SysSubsystem>()
            .eq(SysSubsystem::getStatus, 1).orderByAsc(SysSubsystem::getSortOrder));
    }

    /** 获取所有子系统含停用（管理员首页用） */
    public List<SysSubsystem> getAllSubsystemsIncludeDisabled() {
        return subsystemMapper.selectList(new LambdaQueryWrapper<SysSubsystem>()
            .orderByAsc(SysSubsystem::getSortOrder));
    }

    public List<SysSubsystem> getSubsystemsByUserId(Long userId) {
        return subsystemMapper.selectByUserId(userId);
    }

    public boolean createSubsystem(SysSubsystem subsystem) {
        if (subsystem.getStatus() == null) subsystem.setStatus(1);
        if (subsystem.getSortOrder() == null) subsystem.setSortOrder(99);
        return subsystemMapper.insert(subsystem) > 0;
    }

    public boolean updateSubsystem(SysSubsystem subsystem) {
        return subsystemMapper.updateById(subsystem) > 0;
    }

    public boolean deleteSubsystem(Long id) {
        return subsystemMapper.deleteById(id) > 0;
    }

    /** 更新子系统状态 */
    public boolean updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<SysSubsystem> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysSubsystem::getId, id).set(SysSubsystem::getStatus, status);
        return subsystemMapper.update(null, wrapper) > 0;
    }
}
