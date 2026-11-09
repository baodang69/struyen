package com.struyen.struyen.config;

import com.struyen.struyen.entities.Role;
import com.struyen.struyen.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        initRoleIfNotFound("ROLE_MEMBER", "Quyen thanh vien doc truyen tieu chuan");
        initRoleIfNotFound("ROLE_ADMIN", "Quyen quan tri vien he thong");
    }

    private void initRoleIfNotFound(String name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(Role.builder()
                    .name(name)
                    .description(description)
                    .build());
            log.info("Khoi tao role mac dinh: {}", name);
        }
    }
}

