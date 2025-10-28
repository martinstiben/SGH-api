package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.People;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IPeopleRepository extends JpaRepository<People, Integer> {
    Optional<People> findByEmail(String email);
}