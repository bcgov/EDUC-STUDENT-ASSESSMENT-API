package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentFormRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentTypeCodeRepository;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.FileInputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.v1.URL.BASE_URL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FileUploadControllerTest extends BaseAssessmentAPITest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AssessmentSessionRepository assessmentSessionRepository;
    @Autowired
    private AssessmentTypeCodeRepository assessmentTypeCodeRepository;
    @Autowired
    private AssessmentFormRepository assessmentFormRepository;
    @Autowired
    private AssessmentRepository assessmentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        assessmentFormRepository.deleteAll();
        assessmentTypeCodeRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();

        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity("LTE10"));
        var session = createMockSessionEntity();
        session.setCourseMonth("01");
        session.setCourseYear("2025");
        session.setSchoolYear("2024/2025");
        var savedSession = assessmentSessionRepository.save(session);
        assessmentRepository.save(createMockAssessmentEntity(savedSession, "LTE10"));
    }

    @Test
    void testProcessAssessmentKeysFile_givenTxtFile_WithInvalidIncomingSession_ShouldReturnBadRequest() throws Exception {
        final FileInputStream fis = new FileInputStream("src/test/resources/TRAX_202501_LTE10.txt");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));

        var file = AssessmentKeyFileUpload.builder()
                .fileContents(fileContents)
                .createUser("ABC")
                .fileName("TRAX_202501_LTE10.txt")
                .build();

        this.mockMvc.perform(post( BASE_URL + "/" + UUID.randomUUID() + "/key-file")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_ASSESSMENT_KEYS")))
                .header("correlationID", UUID.randomUUID().toString())
                .content(JsonUtil.getJsonStringFromObject(file))
                .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].message").value("Invalid assessment session."));
    }

    @Test
    void testProcessAssessmentKeysFile_givenTxtFile_WithInvalidInvalidItemType_ShouldReturnBadRequest() throws Exception {
        final FileInputStream fis = new FileInputStream("src/test/resources/TRAX_202501_LTE10_InvalidItemType.txt");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
        var savedSession = assessmentSessionRepository.findByCourseYearAndCourseMonth("2025", "01");

        var file = AssessmentKeyFileUpload.builder()
                .fileContents(fileContents)
                .createUser("ABC")
                .fileName("TRAX_202501_LTE10_InvalidItemType.txt")
                .build();

        this.mockMvc.perform(post( BASE_URL + "/" + savedSession.get().getSessionID() + "/key-file")
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_ASSESSMENT_KEYS")))
                        .header("correlationID", UUID.randomUUID().toString())
                        .content(JsonUtil.getJsonStringFromObject(file))
                        .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].message").value("Invalid item type on line 1."));
    }

    @Test
    void testProcessAssessmentKeysFile_givenTxtFile_WithInvalidFileSession_ShouldReturnBadRequest() throws Exception {
        final FileInputStream fis = new FileInputStream("src/test/resources/TRAX_202501_LTE10_InvalidSession.txt");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
        var savedSession = assessmentSessionRepository.findByCourseYearAndCourseMonth("2025", "01");

        var file = AssessmentKeyFileUpload.builder()
                .fileContents(fileContents)
                .createUser("ABC")
                .fileName("TRAX_202501_LTE10.txt")
                .build();

        this.mockMvc.perform(post( BASE_URL + "/" + savedSession.get().getSessionID() + "/key-file")
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_ASSESSMENT_KEYS")))
                        .header("correlationID", UUID.randomUUID().toString())
                        .content(JsonUtil.getJsonStringFromObject(file))
                        .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].message").value("Invalid assessment session on line 1."));
    }

    @Test
    void testProcessAssessmentKeysFile_givenTxtFile_WithInvalidAssessmentCode_ShouldReturnBadRequest() throws Exception {
        final FileInputStream fis = new FileInputStream("src/test/resources/TRAX_202501_LTE10_InvalidAssessmentCode.txt");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
        var savedSession = assessmentSessionRepository.findByCourseYearAndCourseMonth("2025", "01");

        var file = AssessmentKeyFileUpload.builder()
                .fileContents(fileContents)
                .createUser("ABC")
                .fileName("TRAX_202501_LTE10.txt")
                .build();

        this.mockMvc.perform(post( BASE_URL + "/" + savedSession.get().getSessionID() + "/key-file")
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_ASSESSMENT_KEYS")))
                        .header("correlationID", UUID.randomUUID().toString())
                        .content(JsonUtil.getJsonStringFromObject(file))
                        .contentType(APPLICATION_JSON)).andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].message").value("Invalid assessment type on line 1."));
    }

    @Test
    void testProcessAssessmentKeysFile_givenTxtFile_WithMalformedRow_ShouldReturnOK() throws Exception {
        final FileInputStream fis = new FileInputStream("src/test/resources/TRAX_202501_LTE10_MalformedRow.txt");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
        var savedSession = assessmentSessionRepository.findByCourseYearAndCourseMonth("2025", "01");

        var file = AssessmentKeyFileUpload.builder()
                .fileContents(fileContents)
                .createUser("ABC")
                .fileName("TRAX_202501_LTE10.txt")
                .build();

        this.mockMvc.perform(post( BASE_URL + "/" + savedSession.get().getSessionID() + "/key-file")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_ASSESSMENT_KEYS")))
                .header("correlationID", UUID.randomUUID().toString())
                .content(JsonUtil.getJsonStringFromObject(file))
                .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    @Test
    void testProcessAssessmentKeysFile_givenTxtFile_ShouldReturnOK() throws Exception {
        final FileInputStream fis = new FileInputStream("src/test/resources/TRAX_202501_LTE10.txt");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
        var savedSession = assessmentSessionRepository.findByCourseYearAndCourseMonth("2025", "01");

        var file = AssessmentKeyFileUpload.builder()
                .fileContents(fileContents)
                .createUser("ABC")
                .fileName("TRAX_202501_LTE10.txt")
                .build();

        this.mockMvc.perform(post( BASE_URL + "/" + savedSession.get().getSessionID() + "/key-file")
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_ASSESSMENT_KEYS")))
                        .header("correlationID", UUID.randomUUID().toString())
                        .content(JsonUtil.getJsonStringFromObject(file))
                        .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
    }

    @Test
    void testProcessAssessmentKeysFile_givenTxtFile_WithOpenEndedQues_ShouldReturnOK() throws Exception {
        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity("LTF12"));
        var savedSession = assessmentSessionRepository.findByCourseYearAndCourseMonth("2025", "01");
        assessmentRepository.save(createMockAssessmentEntity(savedSession.get(), "LTF12"));

        final FileInputStream fis = new FileInputStream("src/test/resources/TRAX_202501_LTF12.txt");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));

        var file = AssessmentKeyFileUpload.builder()
                .fileContents(fileContents)
                .createUser("ABC")
                .fileName("TRAX_202501_LTE10.txt")
                .build();

        this.mockMvc.perform(post( BASE_URL + "/" + savedSession.get().getSessionID() + "/key-file")
                .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_ASSESSMENT_KEYS")))
                .header("correlationID", UUID.randomUUID().toString())
                .content(JsonUtil.getJsonStringFromObject(file))
                .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
    }
}
