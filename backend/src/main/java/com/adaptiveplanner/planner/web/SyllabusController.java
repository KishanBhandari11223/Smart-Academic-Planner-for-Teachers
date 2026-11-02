package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import com.adaptiveplanner.planner.subject.InvalidPlannerInputException;
import com.adaptiveplanner.planner.subject.Subject;
import com.adaptiveplanner.planner.syllabus.SyllabusService;
import com.adaptiveplanner.planner.syllabus.SyllabusTopic;
import com.adaptiveplanner.planner.syllabus.SyllabusUnit;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class SyllabusController {
    private final SyllabusService syllabus;

    public SyllabusController(SyllabusService syllabus) {
        this.syllabus = syllabus;
    }

    @GetMapping("/subjects/{subjectId}/syllabus")
    public String syllabus(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                           Model model) {
        Subject subject = syllabus.getSubject(subjectId, teacher.id());
        List<SyllabusUnit> units = syllabus.listUnits(subjectId, teacher.id());
        model.addAttribute("subject", subject);
        model.addAttribute("units", units);
        model.addAttribute("topicCount", units.stream().mapToInt(unit -> unit.getTopics().size()).sum());
        return "syllabus/index";
    }

    @PostMapping("/subjects/{subjectId}/syllabus/units")
    public String createUnit(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                             @RequestParam String name, @RequestParam(required = false) String description,
                             RedirectAttributes redirect) {
        try {
            syllabus.createUnit(subjectId, teacher.id(), name, description);
            redirect.addFlashAttribute("message", "Unit added to the syllabus.");
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/subjects/" + subjectId + "/syllabus";
    }

    @GetMapping("/subjects/{subjectId}/syllabus/units/{unitId}/edit")
    public String editUnit(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                           @PathVariable String unitId, Model model) {
        model.addAttribute("subject", syllabus.getSubject(subjectId, teacher.id()));
        model.addAttribute("unit", syllabus.getUnitForSubject(unitId, subjectId, teacher.id()));
        return "syllabus/unit-form";
    }

    @PostMapping("/subjects/{subjectId}/syllabus/units/{unitId}")
    public String updateUnit(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                             @PathVariable String unitId, @RequestParam String name,
                             @RequestParam(required = false) String description, RedirectAttributes redirect) {
        try {
            syllabus.updateUnit(subjectId, unitId, teacher.id(), name, description);
            redirect.addFlashAttribute("message", "Unit updated.");
            return "redirect:/subjects/" + subjectId + "/syllabus";
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/subjects/" + subjectId + "/syllabus/units/" + unitId + "/edit";
        }
    }

    @PostMapping("/subjects/{subjectId}/syllabus/units/{unitId}/move")
    public String moveUnit(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                           @PathVariable String unitId, @RequestParam String direction,
                           RedirectAttributes redirect) {
        try {
            boolean moved = syllabus.moveUnit(subjectId, unitId, teacher.id(), direction);
            redirect.addFlashAttribute("message", moved ? "Unit order updated." : "This unit is already at that end of the list.");
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/subjects/" + subjectId + "/syllabus";
    }

    @GetMapping("/subjects/{subjectId}/syllabus/units/{unitId}/topics/new")
    public String newTopic(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                           @PathVariable String unitId, Model model) {
        model.addAttribute("subject", syllabus.getSubject(subjectId, teacher.id()));
        model.addAttribute("unit", syllabus.getUnitForSubject(unitId, subjectId, teacher.id()));
        model.addAttribute("topic", null);
        return "syllabus/topic-form";
    }

    @PostMapping("/subjects/{subjectId}/syllabus/units/{unitId}/topics")
    public String createTopic(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                              @PathVariable String unitId, @RequestParam String title,
                              @RequestParam(required = false) String description, @RequestParam int effortMinutes,
                              @RequestParam(defaultValue = "false") boolean important,
                              RedirectAttributes redirect) {
        try {
            syllabus.createTopic(subjectId, unitId, teacher.id(), title, description, effortMinutes, important);
            redirect.addFlashAttribute("message", "Topic added to the syllabus.");
            return "redirect:/subjects/" + subjectId + "/syllabus";
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            redirect.addFlashAttribute("topicTitle", title);
            redirect.addFlashAttribute("topicDescription", description);
            redirect.addFlashAttribute("effortMinutes", effortMinutes);
            redirect.addFlashAttribute("important", important);
            return "redirect:/subjects/" + subjectId + "/syllabus/units/" + unitId + "/topics/new";
        }
    }

    @GetMapping("/subjects/{subjectId}/syllabus/units/{unitId}/topics/{topicId}/edit")
    public String editTopic(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                            @PathVariable String unitId, @PathVariable String topicId, Model model) {
        model.addAttribute("subject", syllabus.getSubject(subjectId, teacher.id()));
        model.addAttribute("unit", syllabus.getUnitForSubject(unitId, subjectId, teacher.id()));
        model.addAttribute("topic", syllabus.getTopic(subjectId, unitId, topicId, teacher.id()));
        return "syllabus/topic-form";
    }

    @PostMapping("/subjects/{subjectId}/syllabus/units/{unitId}/topics/{topicId}")
    public String updateTopic(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                              @PathVariable String unitId, @PathVariable String topicId,
                              @RequestParam String title, @RequestParam(required = false) String description,
                              @RequestParam int effortMinutes, @RequestParam(defaultValue = "false") boolean important,
                              RedirectAttributes redirect) {
        try {
            syllabus.updateTopic(subjectId, unitId, topicId, teacher.id(), title, description,
                    effortMinutes, important);
            redirect.addFlashAttribute("message", "Topic updated.");
            return "redirect:/subjects/" + subjectId + "/syllabus";
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/subjects/" + subjectId + "/syllabus/units/" + unitId + "/topics/" + topicId + "/edit";
        }
    }

    @PostMapping("/subjects/{subjectId}/syllabus/units/{unitId}/topics/{topicId}/move")
    public String moveTopic(@AuthenticationPrincipal TeacherPrincipal teacher, @PathVariable String subjectId,
                            @PathVariable String unitId, @PathVariable String topicId,
                            @RequestParam String direction, RedirectAttributes redirect) {
        try {
            boolean moved = syllabus.moveTopic(subjectId, unitId, topicId, teacher.id(), direction);
            redirect.addFlashAttribute("message", moved ? "Topic order updated." : "This topic is already at that end of the list.");
        } catch (InvalidPlannerInputException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/subjects/" + subjectId + "/syllabus";
    }
}
