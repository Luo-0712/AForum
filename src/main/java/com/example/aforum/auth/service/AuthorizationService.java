package com.example.aforum.auth.service;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

public interface AuthorizationService {

    List<String> getRoleCodesByUserId(Long userId);

    List<String> getPermissionCodesByUserId(Long userId);

    Collection<? extends GrantedAuthority> getAuthoritiesByUserId(Long userId);

    boolean hasRole(Long userId, String roleCode);
}
