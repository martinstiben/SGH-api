package com.horarios.SGH.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.horarios.SGH.Model.AccountStatus;
import com.horarios.SGH.Model.Roles;
import com.horarios.SGH.Model.users;

public interface Iusers extends JpaRepository<users, Integer> {
    Optional<users> findByPerson_Email(String email);
    boolean existsByPerson_Email(String email);
    long count();
    List<users> findByRole(Roles role);

    @Query("SELECT u FROM users u LEFT JOIN FETCH u.person p LEFT JOIN FETCH u.role r WHERE u.accountStatus = :status")
    List<users> findByAccountStatusWithDetails(@Param("status") AccountStatus status);

    @Query("SELECT u FROM users u LEFT JOIN FETCH u.person p LEFT JOIN FETCH u.role r WHERE u.role.roleName = :roleName")
    List<users> findByRoleNameWithDetails(@Param("roleName") String roleName);

    // Para compatibilidad con autenticación
    default Optional<users> findByUserName(String userName) {
        return findByPerson_Email(userName);
    }

    default boolean existsByUserName(String userName) {
        return existsByPerson_Email(userName);
    }
}