package com.struyen.struyen.repositories;

import com.struyen.struyen.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesByUsername(String username);

    @EntityGraph(attributePaths = {"roles"})
    Optional<User> findWithRolesByEmail(String email);
}

