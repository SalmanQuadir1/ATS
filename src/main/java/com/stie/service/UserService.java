package com.stie.service;

import com.stie.model.Tenant;
import com.stie.model.User;
import com.stie.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.stie.repository.RoleRepository roleRepository;

    public User getCurrentUser() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || auth instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }

    public Tenant getCurrentSite() {
        User user = getCurrentUser();
        return (user != null) ? user.getTenant() : null;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    /**
     * Creates a user scoped to a specific site.
     * Called by Site Admins to provision their team members.
     */
    public User createUserForSite(Tenant site, String username, String rawPassword,
                                   Long roleId, String displayName, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        com.stie.model.Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));
        User user = new User(username, passwordEncoder.encode(rawPassword),
                null, site, displayName, email);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    /** Lists all users in a site (for Site Admin user management panel). */
    public List<User> getUsersBySite(Tenant site) {
        return userRepository.findByTenant(site);
    }

    public void updatePassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null) {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
        }
    }

    public void save(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // Keep for backwards compatibility
    public Tenant getCurrentTenant() {
        return getCurrentSite();
    }
}

