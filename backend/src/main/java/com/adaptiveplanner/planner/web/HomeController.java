package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import com.adaptiveplanner.planner.schedule.CalendarService;
import com.adaptiveplanner.planner.schedule.ProgressService;
import com.adaptiveplanner.planner.subject.SubjectService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

@Controller
public class HomeController {

    private final SubjectService subjects;
    private final CalendarService calendar;
    private final ProgressService progress;

    public HomeController(SubjectService subjects, CalendarService calendar, ProgressService progress) {
        this.subjects = subjects;
        this.calendar = calendar;
        this.progress = progress;
    }

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal TeacherPrincipal teacher) {
        model.addAttribute("teacherEmail", teacher.email());
        model.addAttribute("subjects", subjects.listForTeacher(teacher.id(), false));
        model.addAttribute("progress", progress.getDashboard(teacher.id()));
        LocalDate today = LocalDate.now();
        var upcoming = calendar.getCalendar(teacher.id(), today, today.plusDays(14));
        model.addAttribute("upcomingSessions", upcoming.days().stream()
                .flatMap(day -> day.sessions().stream()).limit(6).toList());
        model.addAttribute("pendingAdjustments", upcoming.pendingAdjustments());
        return "index";
    }
}
