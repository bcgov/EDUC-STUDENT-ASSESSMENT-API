package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.BaseEasAPITest;
import ca.bc.gov.educ.eas.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.eas.api.constants.v1.URL;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.SessionRepository;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentKeyFileUpload;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.FileInputStream;
import java.util.Base64;

import static ca.bc.gov.educ.eas.api.properties.ApplicationProperties.EAS_API;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssessmentFileUploadControllerTest extends BaseEasAPITest {

    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    public void after() {
        this.assessmentRepository.deleteAll();
        this.sessionRepository.deleteAll();
    }

    @Test
    void testAssessmentKeyUpload_GivenIncorrectScope_ShouldReturn403() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_STUDENT";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        SessionEntity sessionEntity = createMockSessionEntity();
        sessionEntity.setCourseMonth("11");
        sessionEntity.setCourseYear("2024");
        SessionEntity session = sessionRepository.save(sessionEntity);
        assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTP12.getCode()));

        final FileInputStream fis = new FileInputStream("src/test/resources/TRAX_202411_LTP12.TAB");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
        AssessmentKeyFileUpload assessmentKeyFile = AssessmentKeyFileUpload.builder()
                .fileContents(fileContents)
                .createUser(EAS_API)
                .fileName("TRAX_202411_LTP12.TAB")
                .fileType("TAB")
                .build();
        this.mockMvc.perform(
                        post(URL.ASSESSMENTS_KEY_URL + "/" + session.getSessionID() + "/file")
                                .contentType(APPLICATION_JSON)
                                .content(JsonUtil.getJsonStringFromObject(assessmentKeyFile))
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void testAssessmentKeyUpload_InvalidFilename_ShouldReturn400() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_EAS_ASSESSMENT_KEYS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        SessionEntity sessionEntity = createMockSessionEntity();
        sessionEntity.setCourseMonth("11");
        sessionEntity.setCourseYear("2024");
        SessionEntity session = sessionRepository.save(sessionEntity);
        assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTP12.getCode()));

        final FileInputStream fis = new FileInputStream("src/test/resources/TRAX_202411_LTP12.TAB");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
        AssessmentKeyFileUpload assessmentKeyFile = AssessmentKeyFileUpload.builder()
                .fileContents(fileContents)
                .createUser(EAS_API)
                .fileName("TRAX_202412_LTP12.TAB")
                .fileType("TAB")
                .build();
        this.mockMvc.perform(
                        post(URL.ASSESSMENTS_KEY_URL + "/" + session.getSessionID() + "/file")
                                .contentType(APPLICATION_JSON)
                                .content(JsonUtil.getJsonStringFromObject(assessmentKeyFile))
                                .with(mockAuthority))
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].message").value("Invalid file name. File name must be of type \".TRAX_YYYYMM_{ASSESSMENT_TYPE}\"."));
    }

    @Test
    void testAssessmentKeyUpload_ShouldReturn200() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRITE_EAS_ASSESSMENT_KEYS";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        SessionEntity sessionEntity = createMockSessionEntity();
        sessionEntity.setCourseMonth("11");
        sessionEntity.setCourseYear("2024");
        SessionEntity session = sessionRepository.save(sessionEntity);
        assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTP12.getCode()));

        final FileInputStream fis = new FileInputStream("src/test/resources/TRAX_202411_LTP12.TAB");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
        AssessmentKeyFileUpload assessmentKeyFile = AssessmentKeyFileUpload.builder()
                .fileContents(fileContents)
                .createUser(EAS_API)
                .fileName("TRAX_202411_LTP12.TAB")
                .fileType("TAB")
                .build();
        this.mockMvc.perform(
                        post(URL.ASSESSMENTS_KEY_URL + "/" + session.getSessionID() + "/file")
                                .contentType(APPLICATION_JSON)
                                .content(JsonUtil.getJsonStringFromObject(assessmentKeyFile))
                                .with(mockAuthority))
                .andExpect(status().isOk());
    }


}
