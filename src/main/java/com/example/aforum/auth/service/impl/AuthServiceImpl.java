package com.example.aforum.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aforum.auth.dto.LoginDTO;
import com.example.aforum.auth.dto.RefreshTokenDTO;
import com.example.aforum.auth.dto.RegisterDTO;
import com.example.aforum.auth.entity.Role;
import com.example.aforum.auth.entity.UserRole;
import com.example.aforum.auth.mapper.RoleMapper;
import com.example.aforum.auth.mapper.UserRoleMapper;
import com.example.aforum.auth.security.JwtUtils;
import com.example.aforum.auth.service.AuthService;
import com.example.aforum.auth.service.AuthorizationService;
import com.example.aforum.auth.vo.LoginVO;
import com.example.aforum.auth.vo.UserInfoVO;
import com.example.aforum.auth.vo.UserInfoVOMapper;
import com.example.aforum.common.constant.UserStatus;
import com.example.aforum.common.constant.UserType;
import com.example.aforum.common.exception.BusinessException;
import com.example.aforum.common.result.ErrorCode;
import com.example.aforum.common.util.SecurityUtils;
import com.example.aforum.user.entity.User;
import com.example.aforum.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserInfoVOMapper userInfoVOMapper;
    private final AuthorizationService authorizationService;

    private static final ConcurrentHashMap<String, Long> TOKEN_BLACKLIST = new ConcurrentHashMap<>();

    private static final long ACCESS_EXPIRATION_SECONDS = 86400L;

    @Override
    @Transactional
    public LoginVO register(RegisterDTO dto, String clientIp) {
        // 1. 校验用户名唯一
        Long count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        // 2. 校验邮箱唯一
        count = userMapper.selectCount(
            new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已被注册");
        }

        // 3. 创建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getUsername());
        user.setUserType(UserType.NORMAL.getCode());
        user.setStatus(UserStatus.ACTIVE.getCode());
        user.setLastLoginIp(clientIp);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.insert(user);

        // 4. 分配默认 USER 角色
        Role userRole = roleMapper.selectOne(
            new LambdaQueryWrapper<Role>().eq(Role::getCode, "USER")
        );
        if (userRole != null) {
            UserRole ur = new UserRole();
            ur.setUserId(user.getId());
            ur.setRoleId(userRole.getId());
            userRoleMapper.insert(ur);
        }

        // 5. 生成 token
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        // 6. 构建响应
        UserInfoVO userInfo = userInfoVOMapper.toVO(user);
        fillAuthInfo(userInfo, user.getId());

        return LoginVO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(ACCESS_EXPIRATION_SECONDS)
            .tokenType("Bearer")
            .user(userInfo)
            .build();
    }

    @Override
    public LoginVO login(LoginDTO dto, String clientIp) {
        // 1. 认证（内部调用 UserDetailsServiceImpl + BCrypt 校验）
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 2. 查询用户
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, dto.getUsername())
                .or()
                .eq(User::getEmail, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 3. 检查账户状态
        if (!user.getStatus().equals(UserStatus.ACTIVE.getCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账户已被禁用或封禁");
        }

        // 4. 更新最后登录信息
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        userMapper.updateById(user);

        // 5. 生成 token
        String accessToken = jwtUtils.generateToken(user.getId(), user.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId(), user.getUsername());

        // 6. 构建响应
        UserInfoVO userInfo = userInfoVOMapper.toVO(user);
        fillAuthInfo(userInfo, user.getId());

        return LoginVO.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(ACCESS_EXPIRATION_SECONDS)
            .tokenType("Bearer")
            .user(userInfo)
            .build();
    }

    @Override
    public LoginVO refreshToken(RefreshTokenDTO dto) {
        String refreshToken = dto.getRefreshToken();

        // 1. 验证 refresh token
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refreshToken无效或已过期");
        }

        Claims claims = jwtUtils.parseToken(refreshToken);
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "token类型错误，请使用refreshToken");
        }

        // 2. 检查是否在黑名单
        if (TOKEN_BLACKLIST.containsKey(refreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "refreshToken已失效");
        }

        // 3. 签发新 access token
        Long userId = claims.get("userId", Long.class);
        String username = claims.getSubject();

        User user = userMapper.selectById(userId);
        if (user == null || !user.getStatus().equals(UserStatus.ACTIVE.getCode())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在或已被禁用");
        }

        String newAccessToken = jwtUtils.generateToken(userId, username);

        UserInfoVO userInfo = userInfoVOMapper.toVO(user);
        fillAuthInfo(userInfo, userId);

        return LoginVO.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)
            .expiresIn(ACCESS_EXPIRATION_SECONDS)
            .tokenType("Bearer")
            .user(userInfo)
            .build();
    }

    @Override
    public void logout(String accessToken) {
        if (accessToken == null || !jwtUtils.validateToken(accessToken)) {
            return;
        }
        Claims claims = jwtUtils.parseToken(accessToken);
        long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            TOKEN_BLACKLIST.put(accessToken, claims.getExpiration().getTime());
        }
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return TOKEN_BLACKLIST.containsKey(token);
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        UserInfoVO userInfo = userInfoVOMapper.toVO(user);
        fillAuthInfo(userInfo, userId);
        return userInfo;
    }

    private void fillAuthInfo(UserInfoVO userInfo, Long userId) {
        userInfo.setRoles(authorizationService.getRoleCodesByUserId(userId));
        userInfo.setPermissions(authorizationService.getPermissionCodesByUserId(userId));
    }

    @Scheduled(fixedRate = 600000)
    public void cleanupBlacklist() {
        long now = System.currentTimeMillis();
        TOKEN_BLACKLIST.entrySet().removeIf(entry -> entry.getValue() < now);
    }
}
