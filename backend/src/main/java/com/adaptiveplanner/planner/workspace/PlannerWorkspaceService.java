package com.adaptiveplanner.planner.workspace;

import com.adaptiveplanner.planner.account.TeacherAccount;
import com.adaptiveplanner.planner.account.TeacherAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;

@Service
public class PlannerWorkspaceService {
    private static final int MAX_JSON_LENGTH = 4 * 1024 * 1024;
    private static final int MAX_SUBJECTS = 200;
    private static final int MAX_RECORDS = 10_000;

    private final PlannerWorkspaceRepository workspaces;
    private final TeacherAccountRepository teachers;

    public PlannerWorkspaceService(PlannerWorkspaceRepository workspaces, TeacherAccountRepository teachers) {
        this.workspaces = workspaces;
        this.teachers = teachers;
    }

    @Transactional(readOnly = true)
    public WorkspaceSnapshot get(String teacherId) {
        PlannerWorkspace workspace = workspaces.findById(teacherId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Planner workspace not found."));
        return snapshot(workspace);
    }

    @Transactional
    public WorkspaceSnapshot save(String teacherId, Long expectedVersion, JsonNode data) {
        validate(data);
        PlannerWorkspace current = workspaces.findByTeacherIdForUpdate(teacherId).orElse(null);
        if (current == null) {
            if (expectedVersion != null) throw conflict();
            TeacherAccount teacher = teachers.findById(teacherId)
                    .orElseThrow(() -> new EntityNotFoundException("Teacher account not found."));
            return snapshot(workspaces.saveAndFlush(new PlannerWorkspace(teacher, data.deepCopy())));
        }
        if (expectedVersion == null || current.getVersion() != expectedVersion) throw conflict();
        current.replaceData(data.deepCopy());
        return snapshot(workspaces.saveAndFlush(current));
    }

    private void validate(JsonNode data) {
        if (data == null || !data.isObject() || !data.path("version").isIntegralNumber()
                || data.path("version").intValue() != 1) {
            throw invalid();
        }

        JsonNode subjects = data.get("subjects");
        JsonNode sessions = data.get("sessions");
        JsonNode outcomes = data.get("outcomes");
        if (!isObjectArray(subjects) || subjects.size() > MAX_SUBJECTS
                || !isObjectArray(sessions) || sessions.size() > MAX_RECORDS
                || !isObjectArray(outcomes) || outcomes.size() > MAX_RECORDS) {
            throw invalid();
        }
        JsonNode proposal = data.get("lastProposal");
        if (proposal != null && !proposal.isNull() && !proposal.isObject()) throw invalid();
        if (data.toString().getBytes(StandardCharsets.UTF_8).length > MAX_JSON_LENGTH) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Planner workspace exceeds the 4 MB limit.");
        }
    }

    private boolean isObjectArray(JsonNode node) {
        if (node == null || !node.isArray()) return false;
        for (JsonNode value : node) {
            if (!value.isObject() || !value.path("id").isTextual() || value.path("id").asText().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private WorkspaceSnapshot snapshot(PlannerWorkspace workspace) {
        return new WorkspaceSnapshot(workspace.getVersion(), workspace.getData());
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Planner workspace must contain version 1 and valid subjects, sessions, and outcomes.");
    }

    private ResponseStatusException conflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Planner data changed in another session. Reload the workspace before saving again.");
    }

    public record WorkspaceSnapshot(long version, JsonNode data) { }
}
