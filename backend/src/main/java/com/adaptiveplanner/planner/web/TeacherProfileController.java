package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.account.TeacherAccountRepository;
import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import com.adaptiveplanner.planner.schedule.ProgressService;
import com.adaptiveplanner.planner.subject.SubjectService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;

@Controller
public class TeacherProfileController {
    private final TeacherAccountRepository teachers;
    private final SubjectService subjects;
    private final ProgressService progress;

    public TeacherProfileController(TeacherAccountRepository teachers, SubjectService subjects,
                                    ProgressService progress) {
        this.teachers = teachers;
        this.subjects = subjects;
        this.progress = progress;
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal TeacherPrincipal teacher, Model model) {
        var account = teachers.findById(teacher.id()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher account not found."));
        model.addAttribute("account", account);
        model.addAttribute("memberSince", account.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate());
        model.addAttribute("subjects", subjects.listForTeacher(teacher.id(), false));
        model.addAttribute("archivedSubjects", subjects.listForTeacher(teacher.id(), true));
        model.addAttribute("progress", progress.getDashboard(teacher.id()));
        return "profile/index";
    }
}
