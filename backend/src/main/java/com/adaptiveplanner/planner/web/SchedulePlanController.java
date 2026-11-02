package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import com.adaptiveplanner.planner.schedule.GenerationSummary;
import com.adaptiveplanner.planner.schedule.SchedulePlanService;
import com.adaptiveplanner.planner.schedule.TeachingPlan;
import com.adaptiveplanner.planner.subject.InvalidPlannerInputException;
import com.adaptiveplanner.planner.subject.TeachingSection;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
public class SchedulePlanController {
    private final SchedulePlanService schedulePlans;

    public SchedulePlanController(SchedulePlanService schedulePlans) {
        this.schedulePlans = schedulePlans;
    }

    @GetMapping("/sections/{sectionId}/plans")
    public String list(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String sectionId,
                       Model model) {
        model.addAttribute("section", schedulePlans.getSectionForTeacher(sectionId, teacher.id()));
        model.addAttribute("plans", schedulePlans.listPlans(sectionId, teacher.id()));
        return "schedule/plan-list";
    }

    @GetMapping("/sections/{sectionId}/plans/new")
    public String newPlan(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String sectionId,
                          Model model) {
        TeachingSection section = schedulePlans.getSectionForTeacher(sectionId, teacher.id());
        model.addAttribute("section", section);
        model.addAttribute("availableDays", section.getWeeklySlots().stream()
                .map(slot -> slot.getDayOfWeek()).distinct().sorted(Comparator.comparingInt(DayOfWeek::getValue)).toList());
        return "schedule/plan-form";
    }

    @PostMapping("/sections/{sectionId}/plans")
    public String create(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String sectionId,
                         @RequestParam String name,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                         @RequestParam(name = "days", required = false) List<String> days,
                         RedirectAttributes redirect) {
        try {
            TeachingPlan plan = schedulePlans.createPlan(sectionId, teacher.id(), name, startDate, endDate, days);
            redirect.addFlashAttribute("message", "Draft plan created. Generate a proposal to review the schedule.");
            return "redirect:/plans/" + plan.getId();
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            redirect.addFlashAttribute("name", name);
            redirect.addFlashAttribute("startDate", startDate);
            redirect.addFlashAttribute("endDate", endDate);
            redirect.addFlashAttribute("selectedDays", days);
            return "redirect:/sections/" + sectionId + "/plans/new";
        }
    }

    @GetMapping("/plans/{planId}")
    public String detail(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String planId,
                         Model model) {
        model.addAttribute("details", schedulePlans.getPlanDetails(planId, teacher.id()));
        return "schedule/plan-detail";
    }

    @PostMapping("/plans/{planId}/blocked-dates")
    public String addBlockedDate(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String planId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam String label, @RequestParam String category,
                                 RedirectAttributes redirect) {
        try {
            schedulePlans.addBlockedDate(planId, teacher.id(), date, label, category);
            redirect.addFlashAttribute("message", "Date added to the plan's blocked dates. Regenerate the proposal to apply it.");
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/plans/" + planId;
    }

    @PostMapping("/plans/{planId}/blocked-dates/{blockedDateId}/remove")
    public String removeBlockedDate(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String planId,
                                    @PathVariable String blockedDateId, RedirectAttributes redirect) {
        try {
            schedulePlans.removeBlockedDate(planId, blockedDateId, teacher.id());
            redirect.addFlashAttribute("message", "Blocked date removed. Regenerate the proposal to apply the change.");
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/plans/" + planId;
    }

    @PostMapping("/plans/{planId}/generate")
    public String generate(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String planId,
                           RedirectAttributes redirect) {
        try {
            GenerationSummary result = schedulePlans.generateProposal(planId, teacher.id());
            redirect.addFlashAttribute("message", "Proposal generated with " + result.plannedSessions()
                    + " scheduled time blocks. " + result.topicsNotFullyScheduled()
                    + " topic(s) need more time slots (" + result.remainingEffortMinutes() + " minutes remaining).");
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/plans/" + planId;
    }

    @PostMapping("/plans/{planId}/approve")
    public String approve(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String planId,
                          RedirectAttributes redirect) {
        try {
            schedulePlans.approve(planId, teacher.id());
            redirect.addFlashAttribute("message", "Plan approved. The approved sessions are ready for the calendar workflow.");
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/plans/" + planId;
    }
}
