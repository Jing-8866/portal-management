package com.portal.main.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.portal.common.model.SysUser;
import com.portal.main.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用启动时，自动将DDL中INIT:前缀的密码加密为BCrypt
 */
@Component
public class PasswordInitializer implements CommandLineRunner {
    @Autowired
    private SysUserMapper userMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<SysUser> users = userMapper.selectList(
            new LambdaQueryWrapper<SysUser>().likeRight(SysUser::getPassword, "INIT:")
        );
        for (SysUser user : users) {
            String rawPassword = user.getPassword().replace("INIT:", "");
            user.setPassword(passwordEncoder.encode(rawPassword));
            userMapper.updateById(user);
        }
        if (!users.isEmpty()) {
            System.out.println("[PasswordInitializer] 已加密 " + users.size() + " 个初始密码");
        }
    }
}
