package com.example.aforum.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.auth.entity.Role;
import com.example.aforum.auth.entity.UserRole;
import com.example.aforum.auth.mapper.RoleMapper;
import com.example.aforum.auth.mapper.UserRoleMapper;
import com.example.aforum.common.constant.UserStatus;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .or()
                .eq(User::getEmail, username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        List<String> roleCodes = getRoleCodesByUserId(user.getId());
        List<SimpleGrantedAuthority> authorities = roleCodes.stream()
            .map(code -> new SimpleGrantedAuthority("ROLE_" + code))
            .collect(Collectors.toList());

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            user.getStatus().equals(UserStatus.ACTIVE.getCode()),
            true, true, true,
            authorities
        );
    }

    private List<String> getRoleCodesByUserId(Long userId) {
        List<UserRole> userRoles = userRoleMapper.selectList(
            new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return List.of();
        }
        List<Long> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toList());
        List<Role> roles = roleMapper.selectBatchIds(roleIds);
        return roles.stream()
            .filter(r -> r.getStatus() == 0)
            .map(Role::getCode)
            .collect(Collectors.toList());
    }
}
