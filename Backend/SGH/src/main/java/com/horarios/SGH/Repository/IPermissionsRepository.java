package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.Permissions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IPermissionsRepository extends JpaRepository<Permissions, Integer> {
    Optional<Permissions> findByPermissionName(String permissionName);
}