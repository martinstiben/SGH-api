package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.PermissionsRoles;
import com.horarios.SGH.Model.PermissionsRolesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPermissionsRolesRepository extends JpaRepository<PermissionsRoles, PermissionsRolesId> {
    List<PermissionsRoles> findByRole_RoleId(int roleId);
    List<PermissionsRoles> findByPermission_PermissionId(int permissionId);
}