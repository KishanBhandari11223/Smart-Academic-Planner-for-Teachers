package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import com.adaptiveplanner.planner.subject.InvalidPlannerInputException;
import com.adaptiveplanner.planner.subject.TeachingSection;
import com.adaptiveplanner.planner.subject.TeachingSectionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class TeachingSectionController {
    private final TeachingSectionService sections;

    public TeachingSectionController(TeachingSectionService sections) {
        this.sections = sections;
    }

    @GetMapping("/sections/{sectionId}")
    public String detail(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String sectionId,
                         Model model) {
        TeachingSection section = sections.getForTeacher(sectionId, teacher.id());
        model.addAttribute("section", section);
        return "subjects/section-detail";
    }

    @PostMapping("/sections/{sectionId}")
    public String update(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String sectionId,
                         @RequestParam String name, @RequestParam(required = false) String termLabel,
                         RedirectAttributes redirect) {
        try {
            sections.update(sectionId, teacher.id(), name, termLabel);
            redirect.addFlashAttribute("message", "Teaching section updated.");
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/sections/" + sectionId;
    }

    @PostMapping("/sections/{sectionId}/archive")
    public String archive(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String sectionId,
                          @RequestParam(defaultValue = "true") boolean archived, RedirectAttributes redirect) {
        String subjectId = sections.setArchived(sectionId, teacher.id(), archived);
        redirect.addFlashAttribute("message", archived ? "Teaching section archived." : "Teaching section restored.");
        return "redirect:/subjects/" + subjectId + "?archived=" + archived;
    }

    @PostMapping("/sections/{sectionId}/weekly-slots")
    public String addSlot(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String sectionId,
                          @RequestParam String dayOfWeek, @RequestParam String startTime,
                          @RequestParam String endTime, @RequestParam(required = false) String location,
                          RedirectAttributes redirect) {
        try {
            sections.addWeeklySlot(sectionId, teacher.id(), dayOfWeek, startTime, endTime, location);
            redirect.addFlashAttribute("message", "Weekly teaching time added.");
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/sections/" + sectionId;
    }

    @PostMapping("/sections/{sectionId}/weekly-slots/{slotId}/delete")
    public String removeSlot(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String sectionId,
                             @PathVariable String slotId, RedirectAttributes redirect) {
        try {
            sections.removeWeeklySlot(sectionId, teacher.id(), slotId);
            redirect.addFlashAttribute("message", "Weekly teaching time removed.");
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/sections/" + sectionId;
    }
}
