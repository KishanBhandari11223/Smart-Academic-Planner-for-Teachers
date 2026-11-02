package com.adaptiveplanner.planner.web;

import com.adaptiveplanner.planner.auth.AuthProperties;
import com.adaptiveplanner.planner.auth.EmailRequestLock;
import com.adaptiveplanner.planner.auth.OtpAuthenticationService;
import com.adaptiveplanner.planner.auth.SessionTokenService;
import com.adaptiveplanner.planner.auth.TeacherPrincipal;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthRestController.class, LoginController.class})
@Import(com.adaptiveplanner.planner.auth.WebSecurityConfiguration.class)
class AuthRestControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OtpAuthenticationService authenticationService;

    @MockitoBean
    private SessionTokenService sessionTokens;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private AuthProperties authProperties;

    @MockitoBean
    private EmailRequestLock emailRequestLock;

    @BeforeEach
    void configureCookieSecurity() {
        when(authProperties.isCookieSecure()).thenReturn(false);
    }

    @Test
    void csrfEndpointReturnsTheCookieTokenForSpaRequests() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String csrfCookie = result.getResponse().getCookie("ATP-XSRF-TOKEN").getValue();
        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(csrfCookie);
        assertTrue(responseBody.contains("\"token\":\"" + csrfCookie + "\""));
    }

    @Test
    void logoutRequiresAndAcceptsTheMatchingCsrfCookieAndHeader() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        var csrfCookie = csrfResult.getResponse().getCookie("ATP-XSRF-TOKEN");
        assertNotNull(csrfCookie);
        String token = csrfCookie.getValue();
        var authentication = new UsernamePasswordAuthenticationToken(
                new TeacherPrincipal("teacher-1", "teacher@example.edu"),
                null,
                AuthorityUtils.NO_AUTHORITIES);

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", token)
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent());

        verify(sessionTokens).clear(any());
    }

    @Test
    void thymeleafLogoutFormAcceptsItsCsrfParameterAndReachesTheHandler() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        var csrfCookie = csrfResult.getResponse().getCookie("ATP-XSRF-TOKEN");
        assertNotNull(csrfCookie);
        String token = csrfCookie.getValue();
        var authentication = new UsernamePasswordAuthenticationToken(
                new TeacherPrincipal("teacher-1", "teacher@example.edu"),
                null,
                AuthorityUtils.NO_AUTHORITIES);

        mockMvc.perform(post("/logout")
                        .cookie(csrfCookie)
                        .param("_csrf", token)
                        .with(authentication(authentication)))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .redirectedUrl("/login?loggedOut"));

        verify(sessionTokens).clear(any());
    }

    @Test
    void logoutRejectsMissingCsrfBeforeCallingTheHandler() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                new TeacherPrincipal("teacher-1", "teacher@example.edu"),
                null,
                AuthorityUtils.NO_AUTHORITIES);

        mockMvc.perform(post("/api/auth/logout").with(authentication(authentication)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"error\":\"CSRF token missing or invalid. Refresh the security token and retry.\"}"));

        verifyNoInteractions(sessionTokens);
    }
}
