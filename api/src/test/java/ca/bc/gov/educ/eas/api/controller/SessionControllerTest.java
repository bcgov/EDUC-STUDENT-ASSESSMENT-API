package ca.bc.gov.educ.eas.api.controller;

import ca.bc.gov.educ.eas.api.BaseEasAPITest;
import ca.bc.gov.educ.eas.api.constants.v1.StatusCodes;
import ca.bc.gov.educ.eas.api.constants.v1.URL;
import ca.bc.gov.educ.eas.api.controller.v1.SessionController;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.SessionRepository;
import ca.bc.gov.educ.eas.api.struct.v1.Session;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test cases for assessment session management.
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class SessionControllerTest extends BaseEasAPITest {

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
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_EAS_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
        this.mockMvc.perform(get(URL.SESSIONS_URL).with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void testSessionManagement_GetAllSessions_ShouldReturnOK() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_EAS_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
        SessionEntity SessionEntity = sessionRepository.save(createMockSessionEntity());
        var resultSessions = this.mockMvc.perform(
                        get(URL.SESSIONS_URL).with(mockAuthority))
                .andDo(print()).andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].courseSession").value(LocalDateTime.now().getYear() + "" + LocalDateTime.now().getMonthValue()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].courseYear").value(LocalDateTime.now().getYear()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].courseMonth").value(LocalDateTime.now().getMonthValue()));
    }

    @Test
    void testSessionManagement_UpdateSession_ShouldReturnOK() throws Exception {
        SessionEntity SessionEntity = sessionRepository.save(createMockSessionEntity());
        Session updatedSession = new Session();
        updatedSession.setActiveFromDate(LocalDateTime.now().plusDays(20));
        updatedSession.setActiveUntilDate(LocalDateTime.now().plusDays(120));
        updatedSession.setUpdateUser("test");
        ResultActions resultActions = this.mockMvc.perform(put(URL.SESSIONS_URL + "/" + SessionEntity.getAssessmentSessionID())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_EAS_SESSIONS")))
                .content(objectMapper.writeValueAsString(updatedSession))
                .contentType(APPLICATION_JSON)).andExpect(status().isOk());

        var updatedSessionEntity = sessionRepository.findById(SessionEntity.getAssessmentSessionID());
        assertThat(updatedSessionEntity).isPresent();
        assertThat(updatedSessionEntity.get().getActiveFromDate().toLocalDate()).isEqualTo(updatedSession.getActiveFromDate().toLocalDate());
        assertThat(updatedSessionEntity.get().getActiveUntilDate().toLocalDate()).isEqualTo(updatedSession.getActiveUntilDate().toLocalDate());
    }

    @Test
    void testSessionManagement_UpdateSession_ShouldReturNotFound() throws Exception {
        Session updatedSession = new Session();
        updatedSession.setActiveFromDate(LocalDateTime.now().plusDays(20));
        updatedSession.setActiveUntilDate(LocalDateTime.now().plusDays(120));
        updatedSession.setUpdateUser("test");
        this.mockMvc.perform(put(URL.SESSIONS_URL + "/" + UUID.randomUUID())
                .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_EAS_SESSIONS")))
                .content(objectMapper.writeValueAsString(updatedSession))
                .contentType(APPLICATION_JSON)).andExpect(status().isNotFound());

    }

    private SessionEntity createMockSessionEntity() {
        String courseSessionVal = LocalDateTime.now().getYear() + "" + LocalDateTime.now().getMonthValue();
        SessionEntity sessionEntity = new SessionEntity();
        sessionEntity.setAssessmentSessionID(UUID.randomUUID());
        sessionEntity.setCourseSession(Integer.valueOf(courseSessionVal));
        sessionEntity.setCourseMonth(LocalDateTime.now().getMonthValue());
        sessionEntity.setCourseYear(LocalDateTime.now().getYear());
        sessionEntity.setStatusCode(StatusCodes.OPEN.getCode());
        sessionEntity.setActiveFromDate(LocalDateTime.now());
        sessionEntity.setActiveUntilDate(LocalDateTime.now().plusDays(90));
        sessionEntity.setCreateUser("test");
        sessionEntity.setCreateDate(LocalDateTime.now());
        sessionEntity.setUpdateUser("test");
        sessionEntity.setUpdateDate(LocalDateTime.now());
        return sessionEntity;
    }

}
