package com.horarios.SGH.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.horarios.SGH.Model.Roles;
import com.horarios.SGH.Model.users;

public interface Iusers extends JpaRepository<users, Integer> {
    Optional<users> findByPerson_Email(String email);
    boolean existsByPerson_Email(String email);
    long count();
    List<users> findByRole(Roles role);

    // Para compatibilidad con autenticación
    default Optional<users> findByUserName(String userName) {
        return findByPerson_Email(userName);
    }

    default boolean existsByUserName(String userName) {
        return existsByPerson_Email(userName);
    }
}