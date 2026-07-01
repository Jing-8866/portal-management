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
     * 获取用户可进入门户的子系统（须具备 login 或 admin 权限，且子系统已启用）
     */
    @Select("SELECT DISTINCT s.* FROM sys_subsystem s WHERE s.status = 1 AND s.id IN (" +
            "  SELECT p.subsystem_id FROM v_user_effective_permission p " +
            "  WHERE p.user_id = #{userId} AND p.permission_type IN ('login', 'admin')" +
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

    @Select("SELECT DISTINCT s.system_code FROM sys_subsystem s " +
            "INNER JOIN v_user_effective_permission p ON s.id = p.subsystem_id " +
            "WHERE p.user_id = #{userId} AND p.permission_type = 'admin'")
    List<String> selectAdminSubsystemCodes(@Param("userId") Long userId);

    @Select("SELECT DISTINCT s.system_code FROM sys_subsystem s " +
            "INNER JOIN v_user_effective_permission p ON s.id = p.subsystem_id " +
            "WHERE p.user_id = #{userId} AND p.permission_type IN ('login', 'admin') " +
            "ORDER BY s.sort_order")
    List<String> selectLoginSubsystemCodes(@Param("userId") Long userId);

    @Select("SELECT DISTINCT s.system_code FROM sys_subsystem s " +
            "INNER JOIN v_user_effective_permission p ON s.id = p.subsystem_id " +
            "WHERE p.user_id = #{userId} AND p.permission_type IN ('query', 'admin') " +
            "ORDER BY s.sort_order")
    List<String> selectQuerySubsystemCodes(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM v_user_effective_permission p " +
            "INNER JOIN sys_subsystem s ON p.subsystem_id = s.id " +
            "WHERE p.user_id = #{userId} AND s.system_code = #{subsystemCode} " +
            "AND p.permission_type IN (#{type1}, #{type2})")
    int countPermissionByCode(@Param("userId") Long userId,
                              @Param("subsystemCode") String subsystemCode,
                              @Param("type1") String type1,
                              @Param("type2") String type2);

    @Select("SELECT system_code FROM sys_subsystem ORDER BY sort_order")
    List<String> selectAllSubsystemCodes();

    @Select("SELECT COUNT(*) FROM sys_user_role ur " +
            "INNER JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId} AND r.role_code = 'PLATFORM_ADMIN'")
    int countPlatformAdminRole(@Param("userId") Long userId);
}
