package com.struyen.struyen.config;

import com.struyen.struyen.entities.Role;
import com.struyen.struyen.entities.User;
import com.struyen.struyen.repositories.RoleRepository;
import com.struyen.struyen.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Role memberRole = initRoleIfNotFound("ROLE_MEMBER", "Quyen thanh vien doc truyen tieu chuan");
        Role adminRole = initRoleIfNotFound("ROLE_ADMIN", "Quyen quan tri vien he thong");
        initAdminUserIfNotFound(adminRole, memberRole);
    }

    private Role initRoleIfNotFound(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = roleRepository.save(Role.builder()
                            .name(name)
                            .description(description)
                            .build());
                    log.info("Khoi tao role mac dinh: {}", name);
                    return role;
                });
    }

    private void initAdminUserIfNotFound(Role adminRole, Role memberRole) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@struyen.com")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .userStatus("ACTIVE")
                    .roles(Set.of(adminRole, memberRole))
                    .build();
            userRepository.save(admin);
            log.info("Khoi tao tai khoan Admin mac dinh: admin / Admin@123456");
        }
    }
}
