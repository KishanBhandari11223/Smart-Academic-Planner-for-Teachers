package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import com.adaptiveplanner.planner.schedule.ProgressService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProgressController {
    private final ProgressService progress;

    public ProgressController(ProgressService progress) { this.progress = progress; }

    @GetMapping("/progress")
    public String view(@AuthenticationPrincipal TeacherPrincipal teacher, Model model) {
        model.addAttribute("dashboard", progress.getDashboard(teacher.id()));
        return "progress/index";
    }
}
