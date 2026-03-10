package com.project.spring.security;

import com.project.spring.data.entity.AdminUser;
import com.project.spring.data.entity.User;
import com.project.spring.data.repository.AdminRepository;
import com.project.spring.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {

        // STEP 1 — Check admin_users table first
        AdminUser admin = adminUserRepository.findByEmail(identifier).orElse(null);
        if (admin != null) {
            return new AdminPrincipal(admin);
        }

        // STEP 2 — Check users table by email
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhoneNumber(identifier))
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with identifier: " + identifier));

        return new UserPrincipal(user);
    }
}
