package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRolesRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(String roleName);
}