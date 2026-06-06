package com.example.aforum.auth.service;

import com.example.aforum.auth.dto.LoginDTO;
import com.example.aforum.auth.dto.RefreshTokenDTO;
import com.example.aforum.auth.dto.RegisterDTO;
import com.example.aforum.auth.vo.LoginVO;
import com.example.aforum.auth.vo.UserInfoVO;

public interface AuthService {

    LoginVO register(RegisterDTO dto, String clientIp);

    LoginVO login(LoginDTO dto, String clientIp);

    LoginVO refreshToken(RefreshTokenDTO dto);

    void logout(String accessToken);

    boolean isTokenBlacklisted(String token);

    UserInfoVO getCurrentUserInfo();
}
