package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.struct.v1.Assessment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssessmentControllerTest extends BaseAssessmentAPITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    AssessmentRepository assessmentRepository;

    @Autowired
    AssessmentSessionRepository assessmentSessionRepository;

    @Autowired
    AssessmentTypeCodeRepository assessmentTypeCodeRepository;

    @Autowired
    StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    
    @Autowired
    private AssessmentFormRepository assessmentFormRepository;

    private static final AssessmentMapper mapper = AssessmentMapper.mapper;

    @BeforeEach
    void setUp() {
        stagedAssessmentStudentRepository.deleteAll();
        assessmentFormRepository.deleteAll();
        this.assessmentRepository.deleteAll();
        this.assessmentSessionRepository.deleteAll();
    }

    @Test
    void testUpdateAssessment_GivenInvalidSessionID_ShouldReturn400() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        Assessment assessment = createMockAssessment();
        assessment.setSessionID(UUID.randomUUID().toString());

        this.mockMvc.perform(
                        put(URL.ASSESSMENTS_URL + "/" + assessment.getAssessmentID())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessment))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subErrors[?(@.field == 'sessionID')]").exists());
    }

    @Test
    void testUpdateAssessment_GivenInvalidAssessmentTypeCode_ShouldReturn400() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        Assessment assessment = createMockAssessment();
        assessment.setSessionID(session.getSessionID().toString());
        assessment.setAssessmentTypeCode("INVALID");

        this.mockMvc.perform(
                        put(URL.ASSESSMENTS_URL + "/" + assessment.getAssessmentID())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessment))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subErrors[?(@.field == 'assessmentTypeCode')]").exists());
    }

    @Test
    void testUpdateAssessment_GivenBadID_ShouldReturn400() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        Assessment assessment = createMockAssessment();
        assessment.setAssessmentID(String.valueOf(UUID.randomUUID()));

        this.mockMvc.perform(
                        put(URL.ASSESSMENTS_URL + "/" + UUID.randomUUID())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessment))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.subErrors[?(@.field == 'assessmentID')]").exists());
    }

    @Test
    void testUpdateAssessment_GivenValidPayload_ShouldReturnAssessment() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity(AssessmentTypeCodes.LTF12.getCode()));

        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
        Assessment assessment = mapper.toStructure(assessmentEntity);
        assessment.setCreateDate(null);
        assessment.setUpdateDate(null);

        this.mockMvc.perform(
                        put(URL.ASSESSMENTS_URL + "/" + assessment.getAssessmentID())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessment))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.assessmentID", equalTo(assessment.getAssessmentID())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.updateUser", equalTo(ApplicationProperties.STUDENT_ASSESSMENT_API)));
    }

    @Test
    void testDeleteAssessment_GivenInvalidID_ShouldReturn404() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        this.mockMvc.perform(
                        delete(URL.ASSESSMENTS_URL + "/" + UUID.randomUUID())
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteAssessment_GivenValidID_ShouldReturn204() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));

        this.mockMvc.perform(
                        delete(URL.ASSESSMENTS_URL + "/" + assessmentEntity.getAssessmentID())
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    void testCreateAssessment_GivenValidPayload_ShouldReturnAssessment() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity(AssessmentTypeCodes.LTF12.getCode()));
        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        Assessment assessment = createMockAssessment();
        assessment.setSessionID(session.getSessionID().toString());
        assessment.setAssessmentID(null);

        this.mockMvc.perform(post(URL.ASSESSMENTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(assessment))
                        .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.assessmentTypeCode", equalTo(assessment.getAssessmentTypeCode())))
                .andExpect(MockMvcResultMatchers.jsonPath("$.createUser", equalTo(ApplicationProperties.STUDENT_ASSESSMENT_API)));
    }

    @Test
    void testCreateAssessment_GivenInvalidPayload_ShouldReturn400() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        this.mockMvc.perform(post(URL.ASSESSMENTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(createMockAssessment()))
                        .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}
