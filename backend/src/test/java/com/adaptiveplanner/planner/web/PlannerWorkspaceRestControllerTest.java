package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.AuthProperties;
import com.adaptiveplanner.planner.auth.OtpAuthenticationService;
import com.adaptiveplanner.planner.auth.SessionTokenService;
import com.adaptiveplanner.planner.auth.TeacherPrincipal;
import com.adaptiveplanner.planner.workspace.PlannerWorkspaceService;
import com.adaptiveplanner.planner.workspace.PlannerWorkspaceService.WorkspaceSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({PlannerWorkspaceRestController.class, AuthRestController.class})
@Import(com.adaptiveplanner.planner.auth.WebSecurityConfiguration.class)
class PlannerWorkspaceRestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlannerWorkspaceService workspaces;

    @MockitoBean
    private OtpAuthenticationService authenticationService;

    @MockitoBean
    private SessionTokenService sessionTokens;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AuthProperties authProperties;

    @BeforeEach
    void configureCookieSecurity() {
        when(authProperties.isCookieSecure()).thenReturn(false);
    }

    @Test
    void workspaceRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/planner/workspace"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"error\":\"Authentication required.\"}"));

        verifyNoInteractions(workspaces);
    }

    @Test
    void authenticatedTeacherCanReadOnlyTheirWorkspace() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {"version":1,"subjects":[],"sessions":[],"outcomes":[],"lastProposal":null}
                """);
        when(workspaces.get("teacher-1")).thenReturn(new WorkspaceSnapshot(3, data));

        mockMvc.perform(get("/api/planner/workspace")
                        .with(authentication(teacherAuthentication())))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"version":3,"data":{"version":1,"subjects":[],"sessions":[],"outcomes":[],"lastProposal":null}}
                        """));

        verify(workspaces).get("teacher-1");
    }

    @Test
    void savingWorkspaceRequiresCsrfAndPassesExpectedVersion() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        var csrfCookie = csrfResult.getResponse().getCookie("ATP-XSRF-TOKEN");
        assertNotNull(csrfCookie);
        JsonNode data = objectMapper.readTree("""
                {"version":1,"subjects":[],"sessions":[],"outcomes":[],"lastProposal":null}
                """);
        when(workspaces.save(eq("teacher-1"), eq(2L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new WorkspaceSnapshot(3, data));

        mockMvc.perform(put("/api/planner/workspace")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .with(authentication(teacherAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":2,"data":{"version":1,"subjects":[],"sessions":[],"outcomes":[],"lastProposal":null}}
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"version\":3}"));

        verify(workspaces).save(eq("teacher-1"), eq(2L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void invalidWorkspaceRequestIsRejectedBeforePersistence() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        var csrfCookie = csrfResult.getResponse().getCookie("ATP-XSRF-TOKEN");
        assertNotNull(csrfCookie);

        mockMvc.perform(put("/api/planner/workspace")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .with(authentication(teacherAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(workspaces);
    }

    private UsernamePasswordAuthenticationToken teacherAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new TeacherPrincipal("teacher-1", "teacher@example.edu"),
                null,
                AuthorityUtils.NO_AUTHORITIES);
    }
}
