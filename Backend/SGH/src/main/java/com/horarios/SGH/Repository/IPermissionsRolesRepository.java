package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPermissionsRolesRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRole_RoleId(Long roleId);
    List<RolePermission> findByPermission_PermissionId(Long permissionId);
}