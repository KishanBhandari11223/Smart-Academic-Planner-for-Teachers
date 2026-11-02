package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import com.adaptiveplanner.planner.workspace.PlannerWorkspaceService;
import com.adaptiveplanner.planner.workspace.PlannerWorkspaceService.WorkspaceSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/planner/workspace")
public class PlannerWorkspaceRestController {
    private final PlannerWorkspaceService workspaces;

    public PlannerWorkspaceRestController(PlannerWorkspaceService workspaces) {
        this.workspaces = workspaces;
    }

    @GetMapping
    public WorkspaceSnapshot get(@AuthenticationPrincipal TeacherPrincipal teacher) {
        return workspaces.get(teacher.id());
    }

    @PutMapping
    public WorkspaceSnapshot save(@AuthenticationPrincipal TeacherPrincipal teacher,
                                  @Valid @RequestBody WorkspaceRequest request) {
        return workspaces.save(teacher.id(), request.version(), request.data());
    }

    public record WorkspaceRequest(Long version, @NotNull JsonNode data) { }
}
