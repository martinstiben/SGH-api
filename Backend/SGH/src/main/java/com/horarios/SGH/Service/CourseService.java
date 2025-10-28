package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.CourseDTO;
import com.horarios.SGH.Model.courses;
import com.horarios.SGH.Model.teachers;
import com.horarios.SGH.Repository.Icourses;
import com.horarios.SGH.Repository.Iteachers;
import com.horarios.SGH.Repository.TeacherSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final Icourses courseRepo;
    private final Iteachers teacherRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;

    private static Comparator<CourseDTO> naturalOrderComparator() {
        return Comparator.comparing(dto -> Pattern.compile("(\\d+)").splitAsStream(dto.getCourseName())
                .map(part -> part.matches("\\d+") ? String.format("%010d", Integer.parseInt(part)) : part)
                .collect(Collectors.joining()));
    }

    public CourseDTO create(CourseDTO dto) {
        courses entity = new courses();
        entity.setCourseName(dto.getCourseName());

        // Solo asignar director de grado si se especifica
        if (dto.getGradeDirectorId() != null) {
            teachers director = teacherRepo.findById(dto.getGradeDirectorId()).orElseThrow();
            entity.setGradeDirector(director);
        }

        courses saved = courseRepo.save(entity);
        dto.setCourseId(saved.getId());
        return dto;
    }

    public List<CourseDTO> getAll() {
        return courseRepo.findAll().stream().map(c -> {
            CourseDTO dto = new CourseDTO();
            dto.setCourseId(c.getId());
            dto.setCourseName(c.getCourseName());
            dto.setGradeDirectorId(c.getGradeDirector() != null ? c.getGradeDirector().getId() : null);
            return dto;
        }).sorted(naturalOrderComparator()).collect(Collectors.toList());
    }

    public CourseDTO getById(int id) {
        return courseRepo.findById(id).map(c -> {
            CourseDTO dto = new CourseDTO();
            dto.setCourseId(c.getId());
            dto.setCourseName(c.getCourseName());
            dto.setGradeDirectorId(c.getGradeDirector() != null ? c.getGradeDirector().getId() : null);
            return dto;
        }).orElse(null);
    }

    public CourseDTO update(int id, CourseDTO dto) {
        courses entity = courseRepo.findById(id).orElse(null);
        if (entity == null) return null;

        entity.setCourseName(dto.getCourseName());

        if (dto.getGradeDirectorId() != null) {
            teachers director = teacherRepo.findById(dto.getGradeDirectorId()).orElseThrow();
            entity.setGradeDirector(director);
        } else {
            entity.setGradeDirector(null);
        }

        courses updated = courseRepo.save(entity);
        dto.setCourseId(updated.getId());
        return dto;
    }

    public void delete(int id) {
        courseRepo.deleteById(id);
    }

}