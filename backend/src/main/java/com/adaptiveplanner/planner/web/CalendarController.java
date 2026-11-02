package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import com.adaptiveplanner.planner.schedule.CalendarService;
import com.adaptiveplanner.planner.schedule.ClassOutcome;
import com.adaptiveplanner.planner.schedule.InvalidCalendarActionException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class CalendarController {
    private final CalendarService calendar;

    public CalendarController(CalendarService calendar) { this.calendar = calendar; }

    @GetMapping("/calendar")
    public String view(@AuthenticationPrincipal TeacherPrincipal teacher,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model, RedirectAttributes redirect) {
        LocalDate start = from == null ? LocalDate.now().minusDays(14) : from;
        LocalDate end = to == null ? LocalDate.now().plusDays(60) : to;
        try {
            model.addAttribute("calendar", calendar.getCalendar(teacher.id(), start, end));
            model.addAttribute("today", LocalDate.now());
            return "schedule/calendar";
        } catch (InvalidCalendarActionException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/calendar";
        }
    }

    @GetMapping("/calendar/sessions/{sessionId}/log")
    public String logForm(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String sessionId,
                          Model model) {
        model.addAttribute("session", calendar.getSessionForTeacher(sessionId, teacher.id()));
        return "schedule/class-update";
    }

    @PostMapping("/calendar/sessions/{sessionId}/log")
    public String log(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String sessionId,
                      @RequestParam(required = false) ClassOutcome outcome,
                      @RequestParam(required = false) Integer coveredMinutes,
                      @RequestParam(required = false) String notes,
                      RedirectAttributes redirect) {
        try {
            var result = calendar.recordOutcome(sessionId, teacher.id(), outcome, coveredMinutes, notes);
            redirect.addFlashAttribute("message", result.message());
            if (result.hasProposal()) redirect.addFlashAttribute("proposalId", result.proposalId());
        } catch (InvalidCalendarActionException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/calendar";
    }

    @PostMapping("/schedule-adjustments/{proposalId}/apply")
    public String apply(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String proposalId,
                        RedirectAttributes redirect) {
        try {
            calendar.applyAdjustment(proposalId, teacher.id());
            redirect.addFlashAttribute("message", "Follow-up session added to the calendar.");
        } catch (InvalidCalendarActionException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/calendar";
    }

    @PostMapping("/schedule-adjustments/{proposalId}/dismiss")
    public String dismiss(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String proposalId,
                          RedirectAttributes redirect) {
        try {
            calendar.dismissAdjustment(proposalId, teacher.id());
            redirect.addFlashAttribute("message", "The original schedule was kept.");
        } catch (InvalidCalendarActionException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/calendar";
    }
}
