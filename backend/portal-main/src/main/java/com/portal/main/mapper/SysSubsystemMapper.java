package com.portal.main.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.common.model.SysSubsystem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysSubsystemMapper extends BaseMapper<SysSubsystem> {

    /**
     * 获取用户有权限的子系统列表
     * 权限来源合并：
     *   1. sys_user_subsystem - 直接分配给用户的权限
     *   2. sys_role_subsystem + sys_user_role - 通过角色间接获得的权限
     * 两种方式取并集(UNION)，任一方式授权即生效
     */
    @Select("SELECT DISTINCT s.* FROM sys_subsystem s WHERE s.status = 1 AND s.id IN (" +
            "  SELECT subsystem_id FROM sys_user_subsystem WHERE user_id = #{userId} " +
            "  UNION " +
            "  SELECT rs.subsystem_id FROM sys_role_subsystem rs " +
            "    INNER JOIN sys_user_role ur ON rs.role_id = ur.role_id " +
            "    WHERE ur.user_id = #{userId}" +
            ") ORDER BY s.sort_order")
    List<SysSubsystem> selectByUserId(@Param("userId") Long userId);

    /**
     * 获取用户的有效权限类型列表（合并用户直接权限+角色权限）
     * 用于权限详情展示
     */
    @Select("SELECT subsystem_id, permission_type FROM sys_user_subsystem WHERE user_id = #{userId} " +
            "UNION " +
            "SELECT rs.subsystem_id, rs.permission_type FROM sys_role_subsystem rs " +
            "  INNER JOIN sys_user_role ur ON rs.role_id = ur.role_id " +
            "  WHERE ur.user_id = #{userId}")
    List<java.util.Map<String, Object>> selectEffectivePermissions(@Param("userId") Long userId);
}
