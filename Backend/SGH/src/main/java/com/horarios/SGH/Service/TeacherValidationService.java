package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.TeacherDTO;
import com.horarios.SGH.Repository.Isubjects;
import com.horarios.SGH.Repository.Iteachers;
import org.springframework.stereotype.Service;

@Service
public class TeacherValidationService {

    private final Isubjects subjectRepo;
    private final Iteachers teacherRepo;

    public TeacherValidationService(Isubjects subjectRepo, Iteachers teacherRepo) {
        this.subjectRepo = subjectRepo;
        this.teacherRepo = teacherRepo;
    }

    public void validateForCreate(TeacherDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Teacher data is required");
        ValidationUtils.validateName(dto.getTeacherName());
        Integer subjectId = dto.getSubjectId();
        if (subjectId != null && subjectId > 0) {
            subjectRepo.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        }
    }

    public void validateForUpdate(int id, TeacherDTO dto) {
        if (dto == null) throw new IllegalArgumentException("Teacher data is required");
        if (!teacherRepo.existsById(id)) throw new IllegalArgumentException("Profesor no encontrado");
        ValidationUtils.validateName(dto.getTeacherName());
        Integer subjectId = dto.getSubjectId();
        if (subjectId != null && subjectId > 0) {
            subjectRepo.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
        }
    }
}
