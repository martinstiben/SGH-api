package com.horarios.SGH.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.horarios.SGH.Model.courses;
import java.util.List;

public interface Icourses extends JpaRepository<courses, Integer> {
    List<courses> findByGradeDirector_Id(Integer teacherId);
}