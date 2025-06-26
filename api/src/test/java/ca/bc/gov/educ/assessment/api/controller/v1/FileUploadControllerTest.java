package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultFileUpload;
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
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.v1.URL.BASE_URL;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
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
    @Autowired
    private AssessmentQuestionRepository assessmentQuestionRepository;
    @Autowired
    StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    @Autowired
    private RestUtils restUtils;
    @Autowired
    private AssessmentComponentRepository assessmentComponentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stagedAssessmentStudentRepository.deleteAll();
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
    void testProcessAssessmentResultsFile_givenTxtFile_WithInvalidIncomingSession_ShouldReturnBadRequest() throws Exception {
        final FileInputStream fis = new FileInputStream("src/test/resources/202406_RESULTS_LTP10.txt");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));

        var file = AssessmentResultFileUpload.builder()
                .fileContents(fileContents)
                .createUser("ABC")
                .updateUser("ABC")
                .fileName("202406_RESULTS_LTE10.txt")
                .build();

        this.mockMvc.perform(post( BASE_URL + "/" + UUID.randomUUID() + "/results-file")
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_GRAD_COLLECTION")))
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

    @Test
    void testProcessAssessmentResulsFile_givenTxtFile_WithOpenEndedQues_ShouldReturnOK() throws Exception {
        assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity("LTP10"));
        var savedSession = assessmentSessionRepository.findByCourseYearAndCourseMonth("2025", "01");
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession.get(), "LTP10"));

        var savedForm = assessmentFormRepository.save(createMockAssessmentFormEntity(savedAssessment, "A"));

        var savedMultiComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "MUL_CHOICE", "NONE"));
        for(int i = 1;i < 29;i++) {
            assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedMultiComp, i, i));
        }
        
        var savedOpenEndedComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "OPEN_ENDED", "NONE"));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));

        final FileInputStream fis = new FileInputStream("src/test/resources/202406_RESULTS_LTP10.txt");
        final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));
        var student = this.createMockStudentAPIStudent();
        when(this.restUtils.getStudentByPEN(any(), anyString())).thenReturn(Optional.of(student));
        when(restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(createMockGradStudentAPIRecord());
        
        var file = AssessmentResultFileUpload.builder()
                .fileContents(fileContents)
                .createUser("ABC")
                .updateUser("ABC")
                .fileName("202406_RESULTS_LTP10.txt")
                .build();

        this.mockMvc.perform(post( BASE_URL + "/" + savedSession.get().getSessionID() + "/results-file")
                        .with(jwt().jwt(jwt -> jwt.claim("scope", "WRITE_GRAD_COLLECTION")))
                        .header("correlationID", UUID.randomUUID().toString())
                        .content(JsonUtil.getJsonStringFromObject(file))
                        .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
    }
}
