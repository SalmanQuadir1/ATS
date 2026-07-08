package com.stie.service;

import com.stie.model.PermissionModule;
import com.stie.repository.PermissionModuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes which sidebar nav modules the current authenticated user can see,
 * based on their granted Spring Security authorities (permissions from roles).
 */
@Service
public class ModuleAccessService {

    @Autowired
    private PermissionModuleRepository permissionModuleRepository;

    /**
     * Returns the ordered list of PermissionModule nav items visible to
     * the authenticated user. Super admins see all modules.
     */
    public List<PermissionModule> getVisibleNavModules(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return Collections.emptyList();
        }

        Set<String> authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        boolean isSuperAdmin = authorities.contains("ROLE_SUPER_ADMIN");

        List<PermissionModule> allNavModules = permissionModuleRepository.findByIsNavItemTrueOrderByNavOrder();

        if (isSuperAdmin) {
            return allNavModules;
        }

        // Filter to modules where the user has the matching permission
        return allNavModules.stream()
                .filter(m -> authorities.contains(m.getName()))
                .collect(Collectors.toList());
    }
}
