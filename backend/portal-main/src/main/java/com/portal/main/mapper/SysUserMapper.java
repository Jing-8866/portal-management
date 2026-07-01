package com.portal.main.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.portal.common.model.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("<script>" +
            "SELECT u.* FROM sys_user u " +
            "WHERE u.username &lt;&gt; 'admin' " +
            "AND u.id NOT IN (SELECT DISTINCT user_id FROM v_user_effective_permission) " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (u.username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR u.real_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR IFNULL(u.email, '') LIKE CONCAT('%', #{keyword}, '%') " +
            "OR IFNULL(u.phone, '') LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY u.id ASC" +
            "</script>")
    List<SysUser> selectUsersWithoutPermission(@Param("keyword") String keyword);
}
