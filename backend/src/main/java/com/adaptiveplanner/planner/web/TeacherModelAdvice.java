package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class TeacherModelAdvice {
    @ModelAttribute("currentTeacherEmail")
    public String currentTeacherEmail(@AuthenticationPrincipal TeacherPrincipal teacher) {
        return teacher == null ? null : teacher.email();
    }
}
