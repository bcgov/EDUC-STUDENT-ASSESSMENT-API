package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentTypeCodeRepository;
import ca.bc.gov.educ.assessment.api.struct.v1.Assessment;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentApproval;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssessmentSessionControllerTest extends BaseAssessmentAPITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    AssessmentRepository assessmentRepository;

    @Autowired
    AssessmentSessionRepository assessmentSessionRepository;

    @Autowired
    AssessmentTypeCodeRepository assessmentTypeCodeRepository;

    private static final AssessmentMapper mapper = AssessmentMapper.mapper;

    @AfterEach
    public void after() {
        this.assessmentRepository.deleteAll();
        this.assessmentSessionRepository.deleteAll();
        this.assessmentTypeCodeRepository.deleteAll();
    }

    @Test
    void testAssessmentApproval_GivenInvalidSessionID_ShouldReturn400() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        AssessmentApproval assessmentApproval = new AssessmentApproval();
        assessmentApproval.setSessionID(UUID.randomUUID().toString());

        this.mockMvc.perform(
                        post(URL.SESSIONS_URL + "/approval/" + UUID.randomUUID())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessmentApproval))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAssessmentApproval_GivenValidPayload_ShouldReturnAssessment() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity(AssessmentTypeCodes.LTF12.getCode()));

        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentApproval assessmentApproval = new AssessmentApproval();
        assessmentApproval.setSessionID(session.getSessionID().toString());
        assessmentApproval.setApprovalStudentCertUserID("ABC");

        this.mockMvc.perform(
                        post(URL.SESSIONS_URL + "/approval/" + session.getSessionID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessmentApproval))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void testAssessmentApprovalSecond_GivenValidPayload_ShouldReturnAssessment() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity(AssessmentTypeCodes.LTF12.getCode()));

        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentApproval assessmentApproval = new AssessmentApproval();
        assessmentApproval.setSessionID(session.getSessionID().toString());
        assessmentApproval.setApprovalAssessmentAnalysisUserID("ABC");

        this.mockMvc.perform(
                        post(URL.SESSIONS_URL + "/approval/" + session.getSessionID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessmentApproval))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void testAssessmentApprovalThird_GivenValidPayload_ShouldReturnAssessment() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity(AssessmentTypeCodes.LTF12.getCode()));

        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentApproval assessmentApproval = new AssessmentApproval();
        assessmentApproval.setSessionID(session.getSessionID().toString());
        assessmentApproval.setApprovalAssessmentDesignUserID("ABC");

        this.mockMvc.perform(
                        post(URL.SESSIONS_URL + "/approval/" + session.getSessionID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessmentApproval))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void testAssessmentApproval_AlreadyApproved_ShouldReturn400() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity(AssessmentTypeCodes.LTF12.getCode()));
        var sess = createMockSessionEntity();
        sess.setApprovalStudentCertUserID("ABC");
        AssessmentSessionEntity session = assessmentSessionRepository.save(sess);
        AssessmentApproval assessmentApproval = new AssessmentApproval();
        assessmentApproval.setSessionID(session.getSessionID().toString());
        assessmentApproval.setApprovalStudentCertUserID("ABC");

        this.mockMvc.perform(
                        post(URL.SESSIONS_URL + "/approval/" + session.getSessionID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessmentApproval))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAssessmentApproval_AlreadyApprovedSecond_ShouldReturn400() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity(AssessmentTypeCodes.LTF12.getCode()));
        var sess = createMockSessionEntity();
        sess.setApprovalAssessmentDesignUserID("ABC");
        AssessmentSessionEntity session = assessmentSessionRepository.save(sess);
        AssessmentApproval assessmentApproval = new AssessmentApproval();
        assessmentApproval.setSessionID(session.getSessionID().toString());
        assessmentApproval.setApprovalAssessmentDesignUserID("ABC");

        this.mockMvc.perform(
                        post(URL.SESSIONS_URL + "/approval/" + session.getSessionID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessmentApproval))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAssessmentApproval_AlreadyApprovedThird_ShouldReturn400() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_ASSESSMENT_SESSIONS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity(AssessmentTypeCodes.LTF12.getCode()));
        var sess = createMockSessionEntity();
        sess.setApprovalAssessmentAnalysisUserID("ABC");
        AssessmentSessionEntity session = assessmentSessionRepository.save(sess);
        AssessmentApproval assessmentApproval = new AssessmentApproval();
        assessmentApproval.setSessionID(session.getSessionID().toString());
        assessmentApproval.setApprovalAssessmentAnalysisUserID("ABC");

        this.mockMvc.perform(
                        post(URL.SESSIONS_URL + "/approval/" + session.getSessionID().toString())
                                .contentType(APPLICATION_JSON)
                                .content(asJsonString(assessmentApproval))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

}
