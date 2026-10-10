package com.cdvtc.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cdvtc.demo.common.BusinessException;
import com.cdvtc.demo.entity.User;
import com.cdvtc.demo.enums.RoleEnum;
import com.cdvtc.demo.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 从 Spring Security 上下文解析当前登录用户，所有数据归属校验都以此为准，
 * 不信任前端传入的用户 id 或角色。
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserMapper userMapper;

    public User requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, authentication.getName()));
        if (user == null) {
            throw new BusinessException(401, "用户不存在或已失效");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return user;
    }

    public User requireCurrentUser(RoleEnum requiredRole) {
        User user = requireCurrentUser();
        if (RoleEnum.of(user.getRole()) != requiredRole) {
            throw new BusinessException(403, "无权访问该资源");
        }
        return user;
    }
}
