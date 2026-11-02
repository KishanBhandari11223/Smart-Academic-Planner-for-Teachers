package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import com.adaptiveplanner.planner.subject.InvalidPlannerInputException;
import com.adaptiveplanner.planner.subject.Subject;
import com.adaptiveplanner.planner.subject.SubjectService;
import com.adaptiveplanner.planner.subject.TeachingSectionService;
import com.adaptiveplanner.planner.syllabus.SyllabusService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SubjectController {
    private final SubjectService subjects;
    private final TeachingSectionService sections;
    private final SyllabusService syllabus;

    public SubjectController(SubjectService subjects, TeachingSectionService sections, SyllabusService syllabus) {
        this.subjects = subjects;
        this.sections = sections;
        this.syllabus = syllabus;
    }

    @GetMapping("/subjects")
    public String list(@AuthenticationPrincipal TeacherPrincipal teacher,
                       @RequestParam(defaultValue = "false") boolean archived, Model model) {
        var subjectList = subjects.listForTeacher(teacher.id(), archived);
        var sectionCounts = sections.activeSectionCounts(teacher.id(),
                subjectList.stream().map(Subject::getId).toList());
        model.addAttribute("subjects", subjectList);
        model.addAttribute("sectionCounts", sectionCounts);
        model.addAttribute("activeSectionTotal", sectionCounts.values().stream().mapToLong(Long::longValue).sum());
        model.addAttribute("showArchived", archived);
        return "subjects/list";
    }

    @GetMapping("/subjects/new")
    public String createForm(Model model) {
        model.addAttribute("subject", null);
        return "subjects/form";
    }

    @PostMapping("/subjects")
    public String create(@AuthenticationPrincipal TeacherPrincipal teacher,
                         @RequestParam String name, @RequestParam(required = false) String description,
                         RedirectAttributes redirect) {
        try {
            Subject subject = subjects.create(teacher.id(), name, description);
            redirect.addFlashAttribute("message", "Subject created.");
            return "redirect:/subjects/" + subject.getId();
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            redirect.addFlashAttribute("name", name);
            redirect.addFlashAttribute("description", description);
            return "redirect:/subjects/new";
        }
    }

    @GetMapping("/subjects/{subjectId}")
    public String detail(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                         @RequestParam(defaultValue = "false") boolean archived, Model model) {
        Subject subject = subjects.getForTeacher(subjectId, teacher.id());
        var sectionList = sections.listForSubject(subjectId, teacher.id(), archived);
        var activeSections = archived ? sections.listForSubject(subjectId, teacher.id(), false) : sectionList;
        var units = syllabus.listUnits(subjectId, teacher.id());
        int topicCount = units.stream().mapToInt(unit -> unit.getTopics().size()).sum();
        int weeklySlotCount = activeSections.stream().mapToInt(section -> section.getWeeklySlots().size()).sum();
        model.addAttribute("subject", subject);
        model.addAttribute("sections", sectionList);
        model.addAttribute("units", units);
        model.addAttribute("unitCount", units.size());
        model.addAttribute("topicCount", topicCount);
        model.addAttribute("activeSectionCount", activeSections.size());
        model.addAttribute("weeklySlotCount", weeklySlotCount);
        model.addAttribute("showArchived", archived);
        return "subjects/detail";
    }

    @GetMapping("/subjects/{subjectId}/edit")
    public String editForm(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                           Model model) {
        model.addAttribute("subject", subjects.getForTeacher(subjectId, teacher.id()));
        return "subjects/form";
    }

    @PostMapping("/subjects/{subjectId}")
    public String update(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                         @RequestParam String name, @RequestParam(required = false) String description,
                         RedirectAttributes redirect) {
        try {
            subjects.update(subjectId, teacher.id(), name, description);
            redirect.addFlashAttribute("message", "Subject updated.");
            return "redirect:/subjects/" + subjectId;
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/subjects/" + subjectId + "/edit";
        }
    }

    @PostMapping("/subjects/{subjectId}/archive")
    public String archive(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                          @RequestParam(defaultValue = "true") boolean archived, RedirectAttributes redirect) {
        subjects.setArchived(subjectId, teacher.id(), archived);
        redirect.addFlashAttribute("message", archived ? "Subject archived." : "Subject restored.");
        return "redirect:/subjects" + (archived ? "" : "?archived=true");
    }

    @PostMapping("/subjects/{subjectId}/sections")
    public String createSection(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                                @RequestParam String name, @RequestParam(required = false) String termLabel,
                                RedirectAttributes redirect) {
        try {
            var section = sections.create(subjectId, teacher.id(), name, termLabel);
            redirect.addFlashAttribute("message", "Teaching section created.");
            return "redirect:/sections/" + section.getId();
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            redirect.addFlashAttribute("sectionName", name);
            redirect.addFlashAttribute("termLabel", termLabel);
            return "redirect:/subjects/" + subjectId;
        }
    }
}
