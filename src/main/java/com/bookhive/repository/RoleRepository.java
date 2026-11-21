package com.bookhive.repository;

import com.bookhive.entity.Role;
import com.bookhive.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository
        extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}