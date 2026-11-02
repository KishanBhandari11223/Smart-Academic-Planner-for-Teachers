package com.adaptiveplanner.planner.workspace;

import com.adaptiveplanner.planner.account.TeacherAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class PlannerWorkspaceServiceTest {
    private PlannerWorkspaceRepository workspaces;
    private TeacherAccountRepository teachers;
    private PlannerWorkspaceService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        workspaces = mock(PlannerWorkspaceRepository.class);
        teachers = mock(TeacherAccountRepository.class);
        service = new PlannerWorkspaceService(workspaces, teachers);
        objectMapper = new ObjectMapper();
    }

    @Test
    void rejectsUnsupportedWorkspaceVersionBeforeDatabaseAccess() {
        var data = objectMapper.createObjectNode();
        data.put("version", 2);
        data.putArray("subjects");
        data.putArray("sessions");
        data.putArray("outcomes");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.save("teacher-1", null, data));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(workspaces, teachers);
    }

    @Test
    void rejectsWorkspaceItemsWithoutIdentifiers() {
        var data = objectMapper.createObjectNode();
        data.put("version", 1);
        data.putArray("subjects").addObject().put("name", "Missing ID");
        data.putArray("sessions");
        data.putArray("outcomes");
        data.putNull("lastProposal");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.save("teacher-1", null, data));

        assertEquals(400, exception.getStatusCode().value());
        verifyNoInteractions(workspaces, teachers);
    }
}
