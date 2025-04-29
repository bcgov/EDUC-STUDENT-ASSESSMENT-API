package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.model.v1.SessionEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.SessionRepository;
import ca.bc.gov.educ.assessment.api.struct.v1.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test cases for assessment session management.
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SessionControllerTest extends BaseAssessmentAPITest {

    protected static final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    @Autowired
    SessionController sessionController;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void afterEach() {
        this.sessionRepository.deleteAll();
    }

    @Test
    void testSessionManagement_WithWrongScope_ShouldReturnForbidden() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "WRONG_SCOPE";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
        this.mockMvc.perform(get(URL.SESSIONS_URL).with(mockAuthority))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void testSessionManagement_WithValidScope_ShouldReturnOK() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
        this.mockMvc.perform(get(URL.SESSIONS_URL).with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void testSessionManagement_GetAllSessions_ShouldReturnOK() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
        sessionRepository.save(createMockSessionEntity());
        this.mockMvc.perform(
                        get(URL.SESSIONS_URL).with(mockAuthority))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].schoolYear").value(LocalDateTime.now().getYear()))
                .andExpect(jsonPath("$.[0].courseYear").value(LocalDateTime.now().getYear()))
                .andExpect(jsonPath("$.[0].courseMonth").value(LocalDateTime.now().getMonthValue()));
    }

    @Test
    void testSessionManagement_GetActiveSessions_ShouldReturnOK() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
        sessionRepository.save(createMockSessionEntity());
        this.mockMvc.perform(
                        get(URL.SESSIONS_URL+"/active").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].schoolYear").value(LocalDateTime.now().getYear()))
                .andExpect(jsonPath("$.[0].courseYear").value(LocalDateTime.now().getYear()))
                .andExpect(jsonPath("$.[0].courseMonth").value(LocalDateTime.now().getMonthValue()));
    }

    @Test
    void testSessionManagement_GetSessionsBySchoolYear_ShouldReturnOK() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
        SessionEntity sessionEntity = sessionRepository.save(createMockSessionEntity());
        this.mockMvc.perform(
                        get(URL.SESSIONS_URL+"/school-year/"+sessionEntity.getSchoolYear()).with(mockAuthority))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].schoolYear").value(LocalDateTime.now().getYear()))
                .andExpect(jsonPath("$.[0].courseYear").value(LocalDateTime.now().getYear()))
                .andExpect(jsonPath("$.[0].courseMonth").value(LocalDateTime.now().getMonthValue()));
    }

    @Test
    void testSessionManagement_UpdateSession_ShouldReturnOK() throws Exception {
        SessionEntity sessionEntity = sessionRepository.save(createMockSessionEntity());
        Session updatedSession = new Session();
        updatedSession.setActiveFromDate(LocalDateTime.now().plusDays(20).toString());
        updatedSession.setActiveUntilDate(LocalDateTime.now().plusDays(120).toString());
        updatedSession.setUpdateUser("test");
        this.mockMvc.perform(put(URL.SESSIONS_URL + "/" + sessionEntity.getSessionID())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_ASSESSMENT_SESSIONS")))
                .content(objectMapper.writeValueAsString(updatedSession))
                .contentType(APPLICATION_JSON)).andExpect(status().isOk());

        var updatedSessionEntity = sessionRepository.findById(sessionEntity.getSessionID());
        assertThat(updatedSessionEntity).isPresent();
        assertThat(updatedSessionEntity.get().getActiveFromDate().toString().substring(0,10)).isEqualTo(updatedSession.getActiveFromDate().substring(0,10));
        assertThat(updatedSessionEntity.get().getActiveUntilDate().toString().substring(0,10)).isEqualTo(updatedSession.getActiveUntilDate().substring(0,10));
    }

    @Test
    void testSessionManagement_UpdateSession_ShouldReturNotFound() throws Exception {
        Session updatedSession = new Session();
        updatedSession.setActiveFromDate(LocalDateTime.now().plusDays(20).toString());
        updatedSession.setActiveUntilDate(LocalDateTime.now().plusDays(120).toString());
        updatedSession.setUpdateUser("test");
        this.mockMvc.perform(put(URL.SESSIONS_URL + "/" + UUID.randomUUID())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_ASSESSMENT_SESSIONS")))
                .content(objectMapper.writeValueAsString(updatedSession))
                .contentType(APPLICATION_JSON)).andExpect(status().isNotFound());

    }

}
