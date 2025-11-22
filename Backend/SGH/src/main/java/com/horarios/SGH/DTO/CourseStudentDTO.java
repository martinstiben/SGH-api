package com.horarios.SGH.DTO;

import com.horarios.SGH.Model.AccountStatus;
import lombok.Data;

@Data
public class CourseStudentDTO {
    private int userId;
    private String fullName;
    private String email;
    private String roleName;
    private AccountStatus accountStatus;
    private boolean isVerified;
}