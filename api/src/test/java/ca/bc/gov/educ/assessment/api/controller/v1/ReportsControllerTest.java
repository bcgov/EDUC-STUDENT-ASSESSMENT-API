package ca.bc.gov.educ.assessment.api.controller.v1;


import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentStudentReportTypeCode;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.DOARReportService;
import ca.bc.gov.educ.assessment.api.service.v1.DOARStagingReportService;
import ca.bc.gov.educ.assessment.api.service.v1.XAMFileService;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMerge;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResultSagaData;
import ca.bc.gov.educ.assessment.api.struct.v1.TransferOnApprovalSagaData;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.ReportGradStudentData;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.SimpleHeadcountResultsTable;
import ca.bc.gov.educ.assessment.api.struct.external.PaginatedResponse;
import ca.bc.gov.educ.assessment.api.struct.external.sdc.v1.Collection;
import ca.bc.gov.educ.assessment.api.struct.external.sdc.v1.SdcSchoolCollectionStudent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportsControllerTest extends BaseAssessmentAPITest {

    protected static final ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    @Autowired
    AssessmentStudentRepository studentRepository;
    @Autowired
    private AssessmentComponentRepository assessmentComponentRepository;
    @Autowired
    AssessmentSessionRepository assessmentSessionRepository;
    @Autowired
    private AssessmentQuestionRepository assessmentQuestionRepository;
    @Autowired
    AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private RestUtils restUtils;
    @Autowired
    StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    @Autowired
    private AssessmentFormRepository assessmentFormRepository;
    @Autowired
    private DOARReportService doarReportService;
    @Autowired
    AssessmentChoiceRepository  assessmentChoiceRepository;
    @Autowired
    private DOARStagingReportService doarStagingReportService;
    @Autowired
    private StagedStudentResultRepository stagedStudentResultRepository;
    @SpyBean
    private XAMFileService xamFileService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        stagedStudentResultRepository.deleteAll();
        assessmentStudentHistoryRepository.deleteAll();
        stagedAssessmentStudentRepository.deleteAll();
        assessmentChoiceRepository.deleteAll();
        assessmentFormRepository.deleteAll();
        this.studentRepository.deleteAll();
        assessmentQuestionRepository.deleteAll();
        assessmentComponentRepository.deleteAll();
        this.assessmentRepository.deleteAll();
        this.assessmentSessionRepository.deleteAll();
    }

    @ParameterizedTest
    @CsvSource({
            "/%s/testing/download/JANE",
            "/%s/school/%s/testing/download",
            "/student/%s/INVALID_TYPE/download",
            "/student-pen/117379339/INVALID_TYPE/download",
            "/%s/INVALID_TYPE",
            "/%s/INVALID_TYPE/available",
            "/%s/school/%s/INVALID_TYPE/available",
            "/student/%s/INVALID_TYPE/available"
    })
    void testReportEndpoints_WithWrongType_ShouldReturnBadRequest(String urlTemplate) throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        String url = String.format(urlTemplate, UUID.randomUUID(), UUID.randomUUID());

        this.mockMvc.perform(get(URL.BASE_URL_REPORT + url).with(mockAuthority))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetDownloadableReport_WithValidButUnhandledType_ShouldReturnEmptyResponse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        // SESSION_RESULTS is a valid AssessmentReportTypeCode, but it's only handled by the
        // school-scoped endpoint, not this session-level one -- it should fall to the default arm
        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + UUID.randomUUID() + "/" + SESSION_RESULTS.getCode() + "/download/TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isNull();
        assertThat(summary.getDocumentData()).isNull();
    }

    @Test
    void testGetMinistryReport_ValidType_ShouldReturnReportData() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        studentRepository.save(student);

        AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
        AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
        studentRepository.save(student2);

        AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
        studentRepository.save(student3);

        var resultActions1 = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + assessmentSessionEntity.getSessionID() + "/" + AssessmentReportTypeCode.ALL_SESSION_REGISTRATIONS.getCode() + "/download/JANE").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
        });

        assertThat(summary1).isNotNull();
        assertThat(summary1.getReportType()).isEqualTo(AssessmentReportTypeCode.ALL_SESSION_REGISTRATIONS.getCode());
    }

    @Test
    void testGetMinistryRandomSessionZip_ShouldReturnZip() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        studentRepository.save(student);

        AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
        AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
        studentRepository.save(student2);

        AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
        studentRepository.save(student3);

        var resultActions1 = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + assessmentSessionEntity.getSessionID() + "/randomSessionSchoolsZip").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary1 = resultActions1.andReturn().getResponse().getContentAsByteArray();

        assertThat(summary1).isNotNull();
    }

    @Test
    void testGetMinistryReport_AllDetailedStudentsInSessionType_ShouldReturnReportData() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        studentRepository.save(student);

        AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
        AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
        studentRepository.save(student2);

        AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
        studentRepository.save(student3);

        var resultActions1 = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + assessmentSessionEntity.getSessionID() + "/" + AssessmentReportTypeCode.ALL_DETAILED_STUDENTS_IN_SESSION_CSV.getCode() + "/download/JANE").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
        });

        assertThat(summary1).isNotNull();
        assertThat(summary1.getReportType()).isEqualTo(AssessmentReportTypeCode.ALL_DETAILED_STUDENTS_IN_SESSION_CSV.getCode());
    }

    @Test
    void testGetMinistryReport_SummaryByGradeInSessionType_ShouldReturnReportData() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        studentRepository.save(student);

        AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
        AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
        studentRepository.save(student2);

        AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
        studentRepository.save(student3);

        var resultActions1 = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + assessmentSessionEntity.getSessionID() + "/" + AssessmentReportTypeCode.SUMMARY_BY_GRADE_FOR_SESSION.getCode() + "/download/JANE").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
        });

        assertThat(summary1).isNotNull();
        assertThat(summary1.getReportType()).isEqualTo(AssessmentReportTypeCode.SUMMARY_BY_GRADE_FOR_SESSION.getCode());
    }

    @Test
    void testGetMinistryReport_SummaryByFormInSessionType_ShouldReturnReportData() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        studentRepository.save(student);

        AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
        AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
        studentRepository.save(student2);

        AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
        studentRepository.save(student3);

        var resultActions1 = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + assessmentSessionEntity.getSessionID() + "/" + AssessmentReportTypeCode.SUMMARY_BY_FORM_FOR_SESSION.getCode() + "/download/JANE").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
        });

        assertThat(summary1).isNotNull();
        assertThat(summary1.getReportType()).isEqualTo(AssessmentReportTypeCode.SUMMARY_BY_FORM_FOR_SESSION.getCode());
    }
    
    @Test
    void testGetMinistryReport_ValidTypeAttempts_ShouldReturnReportData() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        studentRepository.save(student);

        AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));
        AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
        studentRepository.save(student2);

        AssessmentStudentEntity student3 = createMockStudentEntity(assessment2);
        studentRepository.save(student3);

        var resultActions1 = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + assessmentSessionEntity.getSessionID() + "/" + AssessmentReportTypeCode.ATTEMPTS.getCode() + "/download/JANE").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
        });

        assertThat(summary1).isNotNull();
        assertThat(summary1.getReportType()).isEqualTo(AssessmentReportTypeCode.ATTEMPTS.getCode());
    }

    @Test
    void testGetMinistryReport_PenMerges_ShouldReturnReportData() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime fromDate = LocalDate.now().minusMonths(13).atStartOfDay();
        LocalDateTime toDate = LocalDate.now().atStartOfDay();

        String createDateStart = fromDate.format(formatter);
        String createDateEnd = toDate.format(formatter);
        List<StudentMerge> mockMergedStudents = List.of(
                StudentMerge.builder().studentMergeID(UUID.randomUUID().toString()).studentID(UUID.randomUUID().toString()).mergeStudentID(UUID.randomUUID().toString()).studentMergeDirectionCode("FROM").studentMergeSourceCode("MI").build(),
                StudentMerge.builder().studentMergeID(UUID.randomUUID().toString()).studentID(UUID.randomUUID().toString()).mergeStudentID(UUID.randomUUID().toString()).studentMergeDirectionCode("TO").studentMergeSourceCode("API").build()
        );
        when(restUtils.getMergedStudentsForDateRange(any(UUID.class), eq(createDateStart), eq(createDateEnd)))
                .thenReturn(mockMergedStudents);

        AssessmentSessionEntity session = createMockSessionEntity();
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);

        var resultActions1 = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + assessmentSessionEntity.getSessionID() + "/" + AssessmentReportTypeCode.PEN_MERGES.getCode() + "/download/JANE").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());
        val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
        });

        assertThat(summary1).isNotNull();
        assertThat(summary1.getReportType()).isEqualTo(AssessmentReportTypeCode.PEN_MERGES.getCode());
    }

    @Test
    void testGetDownloadableReportForSchool_WithValidButUnhandledType_ShouldReturnEmptyResponse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        // ALL_SESSION_REGISTRATIONS is a valid AssessmentReportTypeCode, but it's only handled by the
        // session-level endpoint, not this school-scoped one -- it should fall to the default arm
        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + UUID.randomUUID() + "/school/" + UUID.randomUUID() + "/" + ALL_SESSION_REGISTRATIONS.getCode() + "/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isNull();
        assertThat(summary.getDocumentData()).isNull();
    }

    @Test
    void testGetDownloadableReportForSchool_ValidTypeXam_ShouldReturnXamFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/XAM_FILE/download")
                        .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(school.getMincode() + "-" + sessionEntity.getCourseYear() + sessionEntity.getCourseMonth() + "-Results" + ".xam");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReportForSchool_XamFile_WhenServiceThrows_ShouldReturnBadRequest() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        doThrow(new RuntimeException("Simulated XAM generation failure")).when(xamFileService).generateXamReport(any(UUID.class), any(UUID.class));

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + UUID.randomUUID() + "/school/" + UUID.randomUUID() + "/XAM_FILE/download")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetDownloadableReportForSchool_NoSessionResults_ShouldReturn428() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.randomUUID());
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/SCHOOL_STUDENTS_BY_ASSESSMENT/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isPreconditionRequired());
    }

    @Test
    void testGetDownloadableReportForSchool_ValidTypeSessionResults_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/SESSION_RESULTS/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(school.getMincode() + "-" + sessionEntity.getCourseYear() + sessionEntity.getCourseMonth() + "-Results.csv");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReportForSchool_CSF_ValidTypeSessionResults_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        school.setSchoolReportingRequirementCode("CSF");
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/SESSION_RESULTS/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(school.getMincode() + "-" + sessionEntity.getCourseYear() + sessionEntity.getCourseMonth() + "-Results.csv");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReportForSchool_SchoolNotFound_ShouldReturnNotFound() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.empty());

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);

        var randomSchoolId = UUID.randomUUID();

        this.mockMvc.perform(
                get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + randomSchoolId + "/download")
                        .with(mockAuthority))
                .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    void testGetDownloadableReportForSchoolSession_ValidTypeSessionResults_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var district = this.createMockDistrict();
        when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        session.setCompletionDate(LocalDateTime.now().minusMinutes(1));
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/SCHOOL_STUDENTS_IN_SESSION/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(SCHOOL_STUDENTS_IN_SESSION.getCode());
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReportForSchoolSession_NoSessionResults_ShouldReturn428() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var district = this.createMockDistrict();
        when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.randomUUID());
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/SCHOOL_STUDENTS_IN_SESSION/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isPreconditionRequired());
    }

    @Test
    void testGetDownloadableReportForSchoolByAssessment_ValidTypeSessionResults_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var district = this.createMockDistrict();
        when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        session.setCompletionDate(LocalDateTime.now().minusMinutes(1));
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/SCHOOL_STUDENTS_BY_ASSESSMENT/download" + "?assessmentID=" + assessment.getAssessmentID())
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(SCHOOL_STUDENTS_BY_ASSESSMENT.getCode());
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReportForISR_ValidTypeSessionResults_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var district = this.createMockDistrict();
        when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));
        when(restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(createMockGradStudentAPIRecord()));
        when(this.restUtils.getStudents(any(UUID.class), any())).thenReturn(List.of(this.createMockStudentAPIStudent()));
        
        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setProficiencyScore(2);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/student/" + student.getStudentID() + "/ISR/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(AssessmentStudentReportTypeCode.ISR.getCode());
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReportForISR_ValidTypeSessionResults_ShouldReturn428() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var district = this.createMockDistrict();
        when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));
        when(restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(createMockGradStudentAPIRecord()));
        when(this.restUtils.getStudents(any(UUID.class), any())).thenReturn(List.of(this.createMockStudentAPIStudent()));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        this.mockMvc.perform(get(URL.BASE_URL_REPORT + "/student/" + student.getStudentID() + "/ISR/download")
                                .with(mockAuthority))
                                .andDo(print()).andExpect(status().isPreconditionRequired());
    }

    @Test
    void testGetDownloadableReportForISRByPEN_ValidTypeSessionResults_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var district = this.createMockDistrict();
        when(this.restUtils.getDistrictByDistrictID(anyString())).thenReturn(Optional.of(district));
        when(restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(createMockGradStudentAPIRecord()));
        var stud = this.createMockStudentAPIStudent();
        when(this.restUtils.getStudentByPEN(any(UUID.class), any())).thenReturn(Optional.of(stud));
        when(this.restUtils.getStudents(any(UUID.class), any())).thenReturn(List.of(stud));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setPen("117379339");
        student.setStudentID(UUID.fromString(stud.getStudentID()));
        student.setProficiencyScore(2);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/student-pen/117379339/ISR/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(AssessmentStudentReportTypeCode.ISR.getCode());
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetSummaryReports_WithUnhandledValidType_ShouldReturnEmptyTable() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);

        // ALL_SESSION_REGISTRATIONS is a valid AssessmentReportTypeCode, but getSummaryReports only
        // handles REGISTRATION_SUMMARY -- it should fall to the default arm
        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + ALL_SESSION_REGISTRATIONS.getCode())
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), SimpleHeadcountResultsTable.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getHeaders()).isNull();
        assertThat(summary.getRows()).isNull();
    }

    @Test
    void testGetSummaryReports_Type_REGISTRATION_SUMMARY_ShouldReturnOK() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setGradeAtRegistration("10");
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/registration-summary")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), SimpleHeadcountResultsTable.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getRows()).isNotNull();
    }

    @Test
    void testGetDownloadableReport_RegistrationDetailReport_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/registration-detail-csv/download/" + "TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("registration-detail-csv");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_PenIssuesReport_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        StagedAssessmentStudentEntity student1 = createMockStagedStudentEntity(assessment);
        student1.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student1.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        student1.setStagedAssessmentStudentStatus("NOPENFOUND");

        StagedAssessmentStudentEntity student2 = createMockStagedStudentEntity(assessment);
        student2.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student2.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        student2.setStagedAssessmentStudentStatus("MERGED");
        student2.setMergedPen("456789111");

        stagedAssessmentStudentRepository.saveAll(List.of(student1, student2));

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/pen-issues-csv/download/" + "TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("pen-issues-csv");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_RegistrationSummaryBySchool_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
        student1.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        student1.setGradeAtRegistration("10");
        AssessmentStudentEntity student2 = createMockStudentEntity(assessment);
        student2.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        student2.setGradeAtRegistration("10");
        AssessmentStudentEntity student3 = createMockStudentEntity(assessment);
        student3.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        student3.setGradeAtRegistration("12");

        studentRepository.saveAll(List.of(student1, student2, student3));

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/registration-summary-by-school/download/" + "TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("registration-summary-by-school");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @ParameterizedTest
    @CsvSource({
            "yukon-summary-report",
            "yukon-student-report"
    })
    void testGetDownloadableReport_Yukon_ShouldReturnCSVFile(String urlTypeCode) throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        var district = this.createMockDistrict();
        when(this.restUtils.getYukonDistrict()).thenReturn(Optional.of(district));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
        student1.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        student1.setGradeAtRegistration("10");
        AssessmentStudentEntity student2 = createMockStudentEntity(assessment);
        student2.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        student2.setGradeAtRegistration("10");
        AssessmentStudentEntity student3 = createMockStudentEntity(assessment);
        student3.setSchoolOfRecordSchoolID(UUID.fromString(school.getSchoolId()));
        student3.setGradeAtRegistration("12");

        studentRepository.saveAll(List.of(student1, student2, student3));

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/" + urlTypeCode + "/download/" + "TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(urlTypeCode);
        assertThat(summary.getDocumentData()).isNotBlank();
    }


    @ParameterizedTest
    @CsvSource({
            "NME10,nme-detailed-doar",
            "NMF10,nmf-detailed-doar",
            "LTE10,lte10-detailed-doar",
            "LTP10,ltp10-detailed-doar",
            "LTE12,lte12-detailed-doar",
            "LTP12,ltp12-detailed-doar",
            "LTF12,ltf12-detailed-doar"
    })
    void testGetDownloadableReport_DetailedDOARBySchool_ShouldReturnCSVFile(String assessmentTypeCode, String urlTypeCode) throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, assessmentTypeCode));

        var savedForm = assessmentFormRepository.save(createMockAssessmentFormEntity(assessment, "A"));

        var savedMultiComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "MUL_CHOICE", "NONE"));
        for(int i = 1;i < 29;i++) {
            assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedMultiComp, i, i));
        }

        var savedOpenEndedComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "OPEN_ENDED", "NONE"));
        var oe1 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        var oe4 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));

        var studentEntity1 = createMockStudentEntity(assessment);
        studentEntity1.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        studentEntity1.setProvincialSpecialCaseCode("X");
        var componentEntity1 = createMockAssessmentStudentComponentEntity(studentEntity1, savedMultiComp.getAssessmentComponentID());
        var componentEntity2 = createMockAssessmentStudentComponentEntity(studentEntity1, savedOpenEndedComp.getAssessmentComponentID());

        var multiQues = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(savedMultiComp.getAssessmentComponentID());
        for(int i = 1;i < multiQues.size() ;i++) {
            if(i % 2 == 0) {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ZERO, componentEntity1));
            } else {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ONE, componentEntity1));

            }
        }

        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe1.getAssessmentQuestionID(), BigDecimal.ONE, componentEntity2));
        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe4.getAssessmentQuestionID(), new BigDecimal(9999), componentEntity2));

        studentEntity1.getAssessmentStudentComponentEntities().addAll(List.of(componentEntity1, componentEntity2));
        studentEntity1.setAssessmentFormID(savedForm.getAssessmentFormID());
        studentRepository.save(studentEntity1);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/" + urlTypeCode + "/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(assessmentTypeCode);
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_DetailedDOARBySchool_WithSpecialCase_A_ShouldReturnBadRequest() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "NMF10"));

        var savedForm = assessmentFormRepository.save(createMockAssessmentFormEntity(assessment, "A"));

        var savedMultiComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "MUL_CHOICE", "NONE"));
        for(int i = 1;i < 29;i++) {
            assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedMultiComp, i, i));
        }

        var savedOpenEndedComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "OPEN_ENDED", "NONE"));
        var oe1 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        var oe4 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));

        var studentEntity1 = createMockStudentEntity(assessment);
        studentEntity1.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        studentEntity1.setProvincialSpecialCaseCode("A");
        var componentEntity1 = createMockAssessmentStudentComponentEntity(studentEntity1, savedMultiComp.getAssessmentComponentID());
        var componentEntity2 = createMockAssessmentStudentComponentEntity(studentEntity1, savedOpenEndedComp.getAssessmentComponentID());

        var multiQues = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(savedMultiComp.getAssessmentComponentID());
        for(int i = 1;i < multiQues.size() ;i++) {
            if(i % 2 == 0) {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ZERO, componentEntity1));
            } else {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ONE, componentEntity1));

            }
        }

        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe1.getAssessmentQuestionID(), BigDecimal.ONE, componentEntity2));
        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe4.getAssessmentQuestionID(), new BigDecimal(9999), componentEntity2));

        studentEntity1.getAssessmentStudentComponentEntities().addAll(List.of(componentEntity1, componentEntity2));
        studentEntity1.setAssessmentFormID(savedForm.getAssessmentFormID());
        studentRepository.save(studentEntity1);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/nmf-detailed-doar/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isPreconditionRequired());
    }

    @ParameterizedTest
    @CsvSource({
            "LTF12",
            "NME10",
            "NMF10",
            "LTE10"
    })
    void testGetDownloadableReport_DOARSummaryBySchool_ShouldReturnPreconditionRequired(String assessmentTypeCode) throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(restUtils.getDistrictByDistrictID(school.getDistrictId())).thenReturn(Optional.of(district));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, assessmentTypeCode));

        var savedForm = assessmentFormRepository.save(createMockAssessmentFormEntity(assessment, "A"));

        var savedMultiComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "MUL_CHOICE", "NONE"));
        for(int i = 1;i < 29;i++) {
            assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedMultiComp, i, i));
        }

        var savedOpenEndedComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "OPEN_ENDED", "NONE"));
        var oe1 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        var oe4 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));

        var studentEntity1 = createMockStudentEntity(assessment);
        studentEntity1.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        var componentEntity1 = createMockAssessmentStudentComponentEntity(studentEntity1, savedMultiComp.getAssessmentComponentID());
        var componentEntity2 = createMockAssessmentStudentComponentEntity(studentEntity1, savedOpenEndedComp.getAssessmentComponentID());

        var multiQues = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(savedMultiComp.getAssessmentComponentID());
        for(int i = 1;i < multiQues.size() ;i++) {
            if(i % 2 == 0) {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ZERO, componentEntity1));
            } else {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ONE, componentEntity1));

            }
        }

        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe1.getAssessmentQuestionID(), BigDecimal.ONE, componentEntity2));
        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe4.getAssessmentQuestionID(), new BigDecimal(9999), componentEntity2));

        studentEntity1.getAssessmentStudentComponentEntities().addAll(List.of(componentEntity1, componentEntity2));
        studentEntity1.setAssessmentFormID(savedForm.getAssessmentFormID());
        var student = studentRepository.save(studentEntity1);

        var sagaData = TransferOnApprovalSagaData
                .builder()
                .stagedStudentAssessmentID(UUID.randomUUID().toString())
                .studentID(String.valueOf(student.getStudentID()))
                .assessmentID(String.valueOf(student.getAssessmentEntity().getAssessmentID()))
                .build();
        doarReportService.createAndPopulateDOARSummaryCalculations(sagaData);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/doar-summary/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isPreconditionRequired());
    }

    @Test
    void testGetDownloadableReport_DOARSummaryBySchool_LTE12_ShouldReturnPDFFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setSchoolReportingRequirementCode("CSF");
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        when(restUtils.getDistrictByDistrictID(school.getDistrictId())).thenReturn(Optional.of(district));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTE12"));

        var savedForm = assessmentFormRepository.save(createMockAssessmentFormEntity(assessment, "A"));

        var savedMultiComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "MUL_CHOICE", "NONE"));
        for(int i = 1;i < 29;i++) {
            assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedMultiComp, i, i));
        }

        var savedOpenEndedComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "OPEN_ENDED", "NONE"));
        var oe1 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        var oe4 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));

        var studentEntity1 = createMockStudentEntity(assessment);
        studentEntity1.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        studentEntity1.setProficiencyScore(1);
        var componentEntity1 = createMockAssessmentStudentComponentEntity(studentEntity1, savedMultiComp.getAssessmentComponentID());
        var componentEntity2 = createMockAssessmentStudentComponentEntity(studentEntity1, savedOpenEndedComp.getAssessmentComponentID());

        var multiQues = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(savedMultiComp.getAssessmentComponentID());
        for(int i = 1;i < multiQues.size() ;i++) {
            if(i % 2 == 0) {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ZERO, componentEntity1));
            } else {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ONE, componentEntity1));

            }
        }

        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe1.getAssessmentQuestionID(), BigDecimal.ONE, componentEntity2));
        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe4.getAssessmentQuestionID(), new BigDecimal(9999), componentEntity2));

        studentEntity1.getAssessmentStudentComponentEntities().addAll(List.of(componentEntity1, componentEntity2));
        studentEntity1.setAssessmentFormID(savedForm.getAssessmentFormID());
        var student = studentRepository.save(studentEntity1);

        var sagaData = TransferOnApprovalSagaData
                .builder()
                .stagedStudentAssessmentID(UUID.randomUUID().toString())
                .studentID(String.valueOf(student.getStudentID()))
                .assessmentID(String.valueOf(student.getAssessmentEntity().getAssessmentID()))
                .build();
        doarReportService.createAndPopulateDOARSummaryCalculations(sagaData);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/doar-summary/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @ParameterizedTest
    @CsvSource({
            "NMF10",
            "LTE10",
            "LTP10",
            "LTP12",
            "LTE12",
            "NME10",
            "LTF12"
    })
    void testGetDownloadableReport_DOARProvincialSummary_ShouldReturnPDFFile(String assessmentTypeCode) throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));
        when(restUtils.getDistrictByDistrictID(school.getDistrictId())).thenReturn(Optional.of(district));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        session.setCompletionDate(LocalDateTime.now().minusDays(1));
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, assessmentTypeCode));

        var savedForm = assessmentFormRepository.save(createMockAssessmentFormEntity(assessment, "A"));

        var savedMultiComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "MUL_CHOICE", "NONE"));
        for(int i = 1;i < 29;i++) {
            assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedMultiComp, i, i));
        }

        var savedOpenEndedComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "OPEN_ENDED", "NONE"));
        var oe1 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        var oe4 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));

        var studentEntity1 = createMockStudentEntity(assessment);
        studentEntity1.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        studentEntity1.setProficiencyScore(2);
        var componentEntity1 = createMockAssessmentStudentComponentEntity(studentEntity1, savedMultiComp.getAssessmentComponentID());
        var componentEntity2 = createMockAssessmentStudentComponentEntity(studentEntity1, savedOpenEndedComp.getAssessmentComponentID());

        var multiQues = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(savedMultiComp.getAssessmentComponentID());
        for(int i = 1;i < multiQues.size() ;i++) {
            if(i % 2 == 0) {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ZERO, componentEntity1));
            } else {
                componentEntity1.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ONE, componentEntity1));

            }
        }

        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe1.getAssessmentQuestionID(), BigDecimal.ONE, componentEntity2));
        componentEntity2.getAssessmentStudentAnswerEntities().add(createMockAssessmentStudentAnswerEntity(oe4.getAssessmentQuestionID(), new BigDecimal(9999), componentEntity2));

        studentEntity1.getAssessmentStudentComponentEntities().addAll(List.of(componentEntity1, componentEntity2));
        studentEntity1.setAssessmentFormID(savedForm.getAssessmentFormID());
        var student = studentRepository.save(studentEntity1);

        var sagaData = TransferOnApprovalSagaData
                .builder()
                .stagedStudentAssessmentID(UUID.randomUUID().toString())
                .studentID(String.valueOf(student.getStudentID()))
                .assessmentID(String.valueOf(student.getAssessmentEntity().getAssessmentID()))
                .build();
        doarReportService.createAndPopulateDOARSummaryCalculations(sagaData);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/doar-prov-summary/download/"+ "TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @ParameterizedTest
    @CsvSource({
            "NMF10",
            "LTE10",
            "LTP10",
            "LTP12",
            "LTE12",
            "NME10",
            "LTF12"
    })
    void testGetDownloadableReport_DOARProvincialSummary_From_Staging_ShouldReturnPDFFile(String assessmentTypeCode) throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));
        when(restUtils.getDistrictByDistrictID(school.getDistrictId())).thenReturn(Optional.of(district));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, assessmentTypeCode));

        var savedForm = assessmentFormRepository.save(createMockAssessmentFormEntity(assessment, "A"));

        var savedMultiComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "MUL_CHOICE", "NONE"));
        for(int i = 1;i < 29;i++) {
            assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedMultiComp, i, i));
        }

        var savedOpenEndedComp = assessmentComponentRepository.save(createMockAssessmentComponentEntity(savedForm, "OPEN_ENDED", "NONE"));
        var oe1 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        var oe4 = assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));

        var studentEntity1 = createMockStagedStudentEntity(assessment);
        studentEntity1.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        studentEntity1.setProficiencyScore(2);
        var componentEntity1 = createMockStagedAssessmentStudentComponentEntity(studentEntity1, savedMultiComp.getAssessmentComponentID());
        var componentEntity2 = createMockStagedAssessmentStudentComponentEntity(studentEntity1, savedOpenEndedComp.getAssessmentComponentID());

        var multiQues = assessmentQuestionRepository.findByAssessmentComponentEntity_AssessmentComponentID(savedMultiComp.getAssessmentComponentID());
        for(int i = 1;i < multiQues.size() ;i++) {
            if(i % 2 == 0) {
                componentEntity1.getStagedAssessmentStudentAnswerEntities().add(createMockStagedAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ZERO, componentEntity1));
            } else {
                componentEntity1.getStagedAssessmentStudentAnswerEntities().add(createMockStagedAssessmentStudentAnswerEntity(multiQues.get(i).getAssessmentQuestionID(), BigDecimal.ONE, componentEntity1));

            }
        }

        componentEntity2.getStagedAssessmentStudentAnswerEntities().add(createMockStagedAssessmentStudentAnswerEntity(oe1.getAssessmentQuestionID(), BigDecimal.ONE, componentEntity2));
        componentEntity2.getStagedAssessmentStudentAnswerEntities().add(createMockStagedAssessmentStudentAnswerEntity(oe4.getAssessmentQuestionID(), new BigDecimal(9999), componentEntity2));

        studentEntity1.getStagedAssessmentStudentComponentEntities().addAll(List.of(componentEntity1, componentEntity2));
        studentEntity1.setAssessmentFormID(savedForm.getAssessmentFormID());
        var student = stagedAssessmentStudentRepository.save(studentEntity1);

        var sagaData = StudentResultSagaData
                .builder()
                .assessmentID(String.valueOf(student.getAssessmentEntity().getAssessmentID()))
                .pen(student.getPen())
                .build();
        doarStagingReportService.createAndPopulateDOARStagingSummaryCalculations(sagaData);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/doar-prov-summary/download/"+ "TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @ParameterizedTest
    @CsvSource({
            "NME10,nme-key-summary",
            "NMF10,nmf-key-summary",
            "LTE10,lte10-key-summary",
            "LTE12,lte12-key-summary",
            "LTP10,ltp10-key-summary",
            "LTP12,ltp12-key-summary",
            "LTF12,ltf12-key-summary"
    })
    void testGetDownloadableReport_KeySummary_ShouldReturnCSVFile(String assessmentTypeCode, String urlTypeCode) throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        assessmentRepository.save(createMockAssessmentEntity(sessionEntity, assessmentTypeCode));

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/" + urlTypeCode + "/download/" + "TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(assessmentTypeCode + "-key-summary");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @ParameterizedTest
    @CsvSource({
            "NME10",
            "NMF10",
            "LTE10",
            "LTE12",
            "LTP10",
            "LTP12",
            "LTF12"
    })
    void testGetDownloadableReport_ItemAnalysis_ShouldReturnCSVFile(String assessmentTypeCode) throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var mockCollection = createMockCollection();
        var mockSdcStudents = List.of(createMockSdcSchoolCollectionStudent());
        var mockPaginatedResponse = createMockPaginatedResponse(List.of(mockCollection));

        when(restUtils.getLastFourCollections(any())).thenReturn(mockPaginatedResponse);
        when(restUtils.get1701DataForStudents(anyString(), anyList())).thenReturn(mockSdcStudents);

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("04");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, assessmentTypeCode));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setPen("123456789");
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/" + assessmentTypeCode + "/download/TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(assessmentTypeCode + "-data-item-analysis");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    private Collection createMockCollection() {
        var collection = new Collection();
        collection.setCollectionID("test-collection-id");
        collection.setCollectionTypeCode("SEPTEMBER");
        collection.setSnapshotDate("2024-09-30");
        return collection;
    }

    private SdcSchoolCollectionStudent createMockSdcSchoolCollectionStudent() {
        var sdcStudent = new SdcSchoolCollectionStudent();
        sdcStudent.setAssignedPen("123456789");
        sdcStudent.setGender("M");
        sdcStudent.setNativeAncestryInd("N");
        sdcStudent.setEnrolledProgramCodes("1705");
        return sdcStudent;
    }

    private PaginatedResponse<Collection> createMockPaginatedResponse(List<Collection> collections) {
        return new PaginatedResponse<>(collections, PageRequest.of(0, collections.size()), collections.size());
    }

    @Test
    void testGetAssessmentStudentSearchReport_NoFilters_ShouldReturnAllStudents() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
        student1.setPen("123456789");
        student1.setSurname("SMITH");
        student1.setGivenName("JOHN");
        student1.setGradeAtRegistration("10");
        student1.setProficiencyScore(3);
        studentRepository.save(student1);

        AssessmentStudentEntity student2 = createMockStudentEntity(assessment);
        student2.setPen("987654321");
        student2.setSurname("DOE");
        student2.setGivenName("JANE");
        student2.setGradeAtRegistration("10");
        student2.setProficiencyScore(4);
        studentRepository.save(student2);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/assessment-students/search/download")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"));

        String csvContent = resultActions.andReturn().getResponse().getContentAsString();
        String contentDisposition = resultActions.andReturn().getResponse().getHeader("Content-Disposition");

        assertThat(csvContent).isNotBlank();
        assertThat(csvContent).contains("PEN,Surname,Given Name,Grade,School of Record Code,School of Record Name,School at Write Code,School at Write Name,Assessment Code,Assessment Session,Proficiency Score,Special Case");
        assertThat(csvContent.split("\n").length).isGreaterThanOrEqualTo(1);
        assertThat(contentDisposition).startsWith("attachment; filename=");
    }

    @Test
    void testGetAssessmentStudentSearchReport_WithSearchCriteria_ShouldReturnFilteredStudents() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        // Create student with grade 10
        AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
        student1.setPen("123456789");
        student1.setSurname("SMITH");
        student1.setGivenName("JOHN");
        student1.setGradeAtRegistration("10");
        student1.setProficiencyScore(3);
        studentRepository.save(student1);

        // Create student with grade 12
        AssessmentStudentEntity student2 = createMockStudentEntity(assessment);
        student2.setPen("987654321");
        student2.setSurname("DOE");
        student2.setGivenName("JANE");
        student2.setGradeAtRegistration("12");
        student2.setProficiencyScore(4);
        studentRepository.save(student2);

        // Search for grade 10 only
        String searchCriteria = "[{\"searchCriteriaList\":[{\"key\":\"gradeAtRegistration\",\"operation\":\"eq\",\"value\":\"10\",\"valueType\":\"STRING\",\"condition\":\"AND\"}]}]";

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/assessment-students/search/download")
                                .param("searchCriteriaList", searchCriteria)
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"));

        String csvContent = resultActions.andReturn().getResponse().getContentAsString();

        assertThat(csvContent).isNotBlank();
        assertThat(csvContent).contains("PEN,Surname,Given Name,Grade,School of Record Code,School of Record Name,School at Write Code,School at Write Name,Assessment Code,Assessment Session,Proficiency Score,Special Case");
        assertThat(csvContent).contains("123456789"); // Student 1 PEN
        assertThat(csvContent).contains("SMITH"); // Student 1 surname
        assertThat(csvContent).doesNotContain("987654321"); // Student 2 should not be in results
    }

    @Test
    void testGetAssessmentStudentSearchReport_WithProficiencyScoreFilter_ShouldReturnMatchingStudents() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTE10.getCode()));

        AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
        student1.setPen("111111111");
        student1.setProficiencyScore(4);
        studentRepository.save(student1);

        AssessmentStudentEntity student2 = createMockStudentEntity(assessment);
        student2.setPen("222222222");
        student2.setProficiencyScore(3);
        studentRepository.save(student2);

        AssessmentStudentEntity student3 = createMockStudentEntity(assessment);
        student3.setPen("333333333");
        student3.setProficiencyScore(4);
        studentRepository.save(student3);

        // Filter by proficiency score 4
        String searchCriteria = "[{\"searchCriteriaList\":[{\"key\":\"proficiencyScore\",\"operation\":\"eq\",\"value\":\"4\",\"valueType\":\"INTEGER\",\"condition\":\"AND\"}]}]";

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/assessment-students/search/download")
                                .param("searchCriteriaList", searchCriteria)
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"));

        String csvContent = resultActions.andReturn().getResponse().getContentAsString();

        assertThat(csvContent).isNotBlank();
        assertThat(csvContent).contains("111111111"); // Student 1 with score 4
        assertThat(csvContent).contains("333333333"); // Student 3 with score 4
        assertThat(csvContent).doesNotContain("222222222"); // Student 2 has score 3, should not be included
    }

    @Test
    void testGetAssessmentStudentSearchReport_WithSessionFilter_ShouldReturnStudentsInSession() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        // Create first session
        AssessmentSessionEntity session1 = createMockSessionEntity();
        session1.setCourseMonth("01");
        session1.setCourseYear("2024");
        AssessmentSessionEntity assessmentSession1 = assessmentSessionRepository.save(session1);
        AssessmentEntity assessment1 = assessmentRepository.save(createMockAssessmentEntity(assessmentSession1, AssessmentTypeCodes.NME10.getCode()));

        AssessmentStudentEntity student1 = createMockStudentEntity(assessment1);
        student1.setPen("444444444");
        studentRepository.save(student1);

        // Create second session
        AssessmentSessionEntity session2 = createMockSessionEntity();
        session2.setCourseMonth("06");
        session2.setCourseYear("2024");
        AssessmentSessionEntity assessmentSession2 = assessmentSessionRepository.save(session2);
        AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(assessmentSession2, AssessmentTypeCodes.NME10.getCode()));

        AssessmentStudentEntity student2 = createMockStudentEntity(assessment2);
        student2.setPen("555555555");
        studentRepository.save(student2);

        // Filter by first session
        String searchCriteria = "[{\"searchCriteriaList\":[{\"key\":\"assessmentEntity.assessmentSessionEntity.sessionID\",\"operation\":\"eq\",\"value\":\"" + assessmentSession1.getSessionID() + "\",\"valueType\":\"UUID\",\"condition\":\"AND\"}]}]";

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/assessment-students/search/download")
                                .param("searchCriteriaList", searchCriteria)
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"));

        String csvContent = resultActions.andReturn().getResponse().getContentAsString();

        assertThat(csvContent).isNotBlank();
        assertThat(csvContent).contains("444444444"); // Student from session 1
        assertThat(csvContent).doesNotContain("555555555"); // Student from session 2 should not be included
        assertThat(csvContent).contains("2024/01"); // Session 1 year/month
    }

    @Test
    void testGetAssessmentStudentSearchReport_WithSpecialCaseCode_ShouldReturnStudentsWithSpecialCase() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP12.getCode()));

        // Student with special case
        AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
        student1.setPen("666666666");
        student1.setProvincialSpecialCaseCode("A"); // AEGROTAT
        studentRepository.save(student1);

        // Student without special case
        AssessmentStudentEntity student2 = createMockStudentEntity(assessment);
        student2.setPen("777777777");
        student2.setProvincialSpecialCaseCode(null);
        studentRepository.save(student2);

        // Filter by special case code
        String searchCriteria = "[{\"searchCriteriaList\":[{\"key\":\"provincialSpecialCaseCode\",\"operation\":\"eq\",\"value\":\"A\",\"valueType\":\"STRING\",\"condition\":\"AND\"}]}]";

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/assessment-students/search/download")
                                .param("searchCriteriaList", searchCriteria)
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"));

        String csvContent = resultActions.andReturn().getResponse().getContentAsString();

        assertThat(csvContent).isNotBlank();
        assertThat(csvContent).contains("666666666"); // Student with special case A
        assertThat(csvContent).doesNotContain("777777777"); // Student without special case
        assertThat(csvContent).contains("AEG"); // Special case description
    }

    @Test
    void testGetAssessmentStudentSearchReport_WithMultipleFilters_ShouldReturnMatchingStudents() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));

        AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
        student1.setPen("888888888");
        student1.setGradeAtRegistration("12");
        student1.setProficiencyScore(3);
        studentRepository.save(student1);

        AssessmentStudentEntity student2 = createMockStudentEntity(assessment);
        student2.setPen("999999999");
        student2.setGradeAtRegistration("12");
        student2.setProficiencyScore(4);
        studentRepository.save(student2);

        AssessmentStudentEntity student3 = createMockStudentEntity(assessment);
        student3.setPen("000000000");
        student3.setGradeAtRegistration("10");
        student3.setProficiencyScore(3);
        studentRepository.save(student3);

        // Filter by grade 12 AND proficiency score 3
        String searchCriteria = "[{\"searchCriteriaList\":[{\"key\":\"gradeAtRegistration\",\"operation\":\"eq\",\"value\":\"12\",\"valueType\":\"STRING\",\"condition\":\"AND\"},{\"key\":\"proficiencyScore\",\"operation\":\"eq\",\"value\":\"3\",\"valueType\":\"INTEGER\",\"condition\":\"AND\"}]}]";

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/assessment-students/search/download")
                                .param("searchCriteriaList", searchCriteria)
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"));

        String csvContent = resultActions.andReturn().getResponse().getContentAsString();

        assertThat(csvContent).isNotBlank();
        assertThat(csvContent).contains("888888888"); // Student 1: grade 12, score 3 - should match
        assertThat(csvContent).doesNotContain("999999999"); // Student 2: grade 12, score 4 - wrong score
        assertThat(csvContent).doesNotContain("000000000"); // Student 3: grade 10, score 3 - wrong grade
    }

    @Test
    void testGetAssessmentStudentSearchReport_WithoutPermission_ShouldReturn403() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRONG_PERMISSION";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/assessment-students/search/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isForbidden());
    }

    @Test
    void testGetAssessmentRegistrationSearchReport_NoFilters_ShouldReturnAllRegistrations() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var schoolOfRecord = this.createMockSchool();
        var assessmentCentre = this.createMockSchool();
        UUID schoolOfRecordId = UUID.randomUUID();
        UUID assessmentCentreId = UUID.randomUUID();
        schoolOfRecord.setSchoolId(schoolOfRecordId.toString());
        schoolOfRecord.setDisplayName("12345678 - School Of Record");
        assessmentCentre.setSchoolId(assessmentCentreId.toString());
        assessmentCentre.setDisplayName("87654321 - Assessment Centre");
        when(this.restUtils.getSchoolBySchoolID(schoolOfRecordId.toString())).thenReturn(Optional.of(schoolOfRecord));
        when(this.restUtils.getSchoolBySchoolID(assessmentCentreId.toString())).thenReturn(Optional.of(assessmentCentre));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseYear("2026");
        session.setCourseMonth("04");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTE10.getCode()));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setPen("123456789");
        student.setLocalID("A001");
        student.setSurname("SMITH");
        student.setGivenName("JOHN");
        student.setSchoolOfRecordSchoolID(schoolOfRecordId);
        student.setAssessmentCenterSchoolID(assessmentCentreId);
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/assessment-registrations/search/download")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"))
                .andExpect(header().exists("Content-Disposition"));

        String csvContent = resultActions.andReturn().getResponse().getContentAsString();

        assertThat(csvContent)
                .contains("Session,Assessment Code,PEN,Local ID,Surname,Given Name,School of Record,Assessment Centre")
                .contains("2026/04,LTE10,123456789,A001,SMITH,JOHN,12345678 - School Of Record,87654321 - Assessment Centre");
    }

    @Test
    void testGetAssessmentRegistrationSearchReport_WithSearchCriteria_ShouldReturnFilteredRegistrations() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_STUDENT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        UUID schoolId = UUID.randomUUID();
        school.setSchoolId(schoolId.toString());
        school.setDisplayName("12345678 - Filter School");
        when(this.restUtils.getSchoolBySchoolID(schoolId.toString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseYear("2026");
        session.setCourseMonth("06");
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.NME10.getCode()));

        AssessmentStudentEntity student1 = createMockStudentEntity(assessment);
        student1.setPen("111111111");
        student1.setSurname("ALPHA");
        student1.setSchoolOfRecordSchoolID(schoolId);
        studentRepository.save(student1);

        AssessmentStudentEntity student2 = createMockStudentEntity(assessment);
        student2.setPen("222222222");
        student2.setSurname("BETA");
        student2.setSchoolOfRecordSchoolID(schoolId);
        studentRepository.save(student2);

        String searchCriteria = "[{\"searchCriteriaList\":[{\"key\":\"surname\",\"operation\":\"eq\",\"value\":\"ALPHA\",\"valueType\":\"STRING\",\"condition\":\"AND\"}]}]";

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/assessment-registrations/search/download")
                                .param("searchCriteriaList", searchCriteria)
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv;charset=UTF-8"));

        String csvContent = resultActions.andReturn().getResponse().getContentAsString();

        assertThat(csvContent).contains("111111111");
        assertThat(csvContent).doesNotContain("222222222");
    }

    @Test
    void testGetAssessmentRegistrationSearchReport_WithoutPermission_ShouldReturn403() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRONG_PERMISSION";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/assessment-registrations/search/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isForbidden());
    }

    @Test
    void testCheckSchoolReportAvailability_WhenSchoolHasResults_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));

        var schoolID = UUID.randomUUID();
        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(schoolID);
        student.setProficiencyScore(2);
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/school/" + schoolID + "/results/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSchoolReportAvailability_WhenSchoolHasNoResults_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/school/" + UUID.randomUUID() + "/results/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckSchoolReportAvailability_WithAssessmentTypeCode_WhenResultsExist_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.LTE10.getCode()));

        var schoolID = UUID.randomUUID();
        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(schoolID);
        student.setProvincialSpecialCaseCode("X");
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/school/" + schoolID + "/results/available")
                                .param("assessmentTypeCode", AssessmentTypeCodes.LTE10.getCode())
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSchoolReportAvailability_WithAssessmentTypeCode_WhenNoResultsForType_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));

        var schoolID = UUID.randomUUID();

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/school/" + schoolID + "/results/available")
                                .param("assessmentTypeCode", AssessmentTypeCodes.NME10.getCode())
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckSchoolReportAvailability_WithAssessmentTypeCode_WhenTypeNotInSession_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/school/" + UUID.randomUUID() + "/results/available")
                                .param("assessmentTypeCode", AssessmentTypeCodes.LTP10.getCode())
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckSchoolReportAvailability_WithoutPermission_ShouldReturn403() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRONG_PERMISSION";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + UUID.randomUUID() + "/school/" + UUID.randomUUID() + "/results/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAssessmentCompletionCurrentStudentsReportForSchool_ShouldReturnCsvFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(school.getSchoolId())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTE10.getCode()));
        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setPen("123456789");
        student.setProficiencyScore(3);
        studentRepository.save(student);

        PaginatedResponse<ReportGradStudentData> gradPage = new PaginatedResponse<>(
                List.of(ReportGradStudentData.builder()
                        .graduationStudentRecordId(UUID.randomUUID())
                        .schoolOfRecordId(UUID.fromString(school.getSchoolId()))
                        .pen("123456789")
                        .localID("L123")
                        .lastName("DOE")
                        .firstName("JANE")
                        .middleName("Q")
                        .dob("2008/01/02")
                        .studentGrade("12")
                        .programCode("2023-EN")
                        .studentStatus("CUR")
                        .build()),
                PageRequest.of(0, 1000),
                1L
        );
        when(this.restUtils.getGradStudentReportPage(anyString(), anyInt(), anyInt())).thenReturn(gradPage);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/school/" + school.getSchoolId() + "/assessment-completions/current-students/download")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PEN,Local ID,Last Name,First Name,Middle Name,Birthdate,Grade,Program,LTE10,NME10,NMF10,LTE12,LTF12,LTP10,LTP12")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("123456789,L123,DOE,JANE,Q,2008/01/02,12,2023-EN,Yes,No,No,No,No,No,No")));
    }

    @Test
    void testGetAssessmentCompletionCurrentStudentsReportForDistrict_ShouldReturnCsvFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        when(this.restUtils.getDistrictByDistrictID(district.getDistrictId())).thenReturn(Optional.of(district));

        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTP12.getCode()));
        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setPen("123456789");
        student.setProvincialSpecialCaseCode("E");
        studentRepository.save(student);

        PaginatedResponse<ReportGradStudentData> gradPage = new PaginatedResponse<>(
                List.of(ReportGradStudentData.builder()
                        .graduationStudentRecordId(UUID.randomUUID())
                        .pen("123456789")
                        .localID("L123")
                        .lastName("DOE")
                        .firstName("JANE")
                        .middleName("Q")
                        .dob("2008/01/02")
                        .studentGrade("12")
                        .programCode("2023-EN")
                        .schoolName("Marco's school")
                        .studentStatus("CUR")
                        .build()),
                PageRequest.of(0, 1000),
                1L
        );
        when(this.restUtils.getGradStudentReportPage(anyString(), anyInt(), anyInt())).thenReturn(gradPage);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/district/" + district.getDistrictId() + "/assessment-completions/current-students/download")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("School of Record,PEN,Local ID,Last Name,First Name,Middle Name,Birthdate,Grade,Program,LTE10,NME10,NMF10,LTE12,LTF12,LTP10,LTP12")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Marco's school,123456789,L123,DOE,JANE,Q,2008/01/02,12,2023-EN,No,No,No,No,No,No,Yes")));
    }

    // ---- checkSessionReportAvailability ----

    @Test
    void testCheckSessionReportAvailability_WhenStudentsExist_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));

        var student = createMockStudentEntity(savedAssessment);
        student.setStudentStatusCode("ACTIVE");
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + ALL_SESSION_REGISTRATIONS.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSessionReportAvailability_WhenNoStudentsExist_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + ALL_SESSION_REGISTRATIONS.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckSessionReportAvailability_ForPenMerges_AlwaysReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + PEN_MERGES.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSessionReportAvailability_WithoutPermission_ShouldReturn403() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRONG_PERMISSION";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + UUID.randomUUID() + "/" + ALL_SESSION_REGISTRATIONS.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void testCheckSessionReportAvailability_DOARProvincialSummary_ApprovedSession_WhenScoredStudentExists_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        session.setCompletionDate(LocalDateTime.now().minusDays(1));
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));

        var student = createMockStudentEntity(savedAssessment);
        student.setProficiencyScore(2);
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + DOAR_PROVINCIAL_SUMMARY.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSessionReportAvailability_DOARProvincialSummary_ApprovedSession_WhenNoScoredStudent_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        session.setCompletionDate(LocalDateTime.now().minusDays(1));
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));

        // student is registered (ACTIVE) but not yet scored -- the generic "any active student" check
        // would say true, but the DOAR provincial report itself would render blank
        var student = createMockStudentEntity(savedAssessment);
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + DOAR_PROVINCIAL_SUMMARY.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckSessionReportAvailability_DOARProvincialSummary_OngoingSession_WhenScoredStagedStudentExists_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));

        var stagedStudent = createMockStagedStudentEntity(savedAssessment);
        stagedStudent.setProficiencyScore(3);
        stagedAssessmentStudentRepository.save(stagedStudent);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + DOAR_PROVINCIAL_SUMMARY.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSessionReportAvailability_DOARProvincialSummary_OngoingSession_WhenNoScoredStagedStudent_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));

        // an ACTIVE, scored student exists in the FINAL table (the generic check would say true), but the
        // session is still ongoing so the DOAR provincial report reads from staging, which has no data
        var student = createMockStudentEntity(savedAssessment);
        student.setProficiencyScore(2);
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + DOAR_PROVINCIAL_SUMMARY.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckSessionReportAvailability_YukonSummary_WhenScoredYukonStudentExists_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        when(this.restUtils.getDistrictByDistrictID(district.getDistrictId())).thenReturn(Optional.of(district));

        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getYukonDistrict()).thenReturn(Optional.of(district));
        when(this.restUtils.getSchools()).thenReturn(List.of(school));

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.LTE10.getCode()));

        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setProficiencyScore(3);
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + YUKON_SUMMARY_CSV.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSessionReportAvailability_YukonSummary_WhenNoYukonStudents_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getYukonDistrict()).thenReturn(Optional.of(district));
        when(this.restUtils.getSchools()).thenReturn(List.of(school));

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.LTE10.getCode()));

        // student exists and is scored, but is NOT in a Yukon-district school -- the generic check
        // (any active student in the session) would say true, but Yukon summary is scoped to Yukon schools
        var student = createMockStudentEntity(savedAssessment);
        student.setProficiencyScore(3);
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + YUKON_SUMMARY_CSV.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckSessionReportAvailability_YukonStudentDetail_WhenYukonStudentExists_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getYukonDistrict()).thenReturn(Optional.of(district));
        when(this.restUtils.getSchools()).thenReturn(List.of(school));

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.LTP10.getCode()));

        // no score needed for this report -- registration alone is sufficient
        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + YUKON_STUDENT_DETAIL_CSV.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSessionReportAvailability_YukonStudentDetail_WhenNoYukonStudents_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getYukonDistrict()).thenReturn(Optional.of(district));
        when(this.restUtils.getSchools()).thenReturn(List.of(school));

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + YUKON_STUDENT_DETAIL_CSV.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckSessionReportAvailability_ItemAnalysis_WhenSessionHasScoresForDifferentAssessmentType_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        session.setCompletionDate(LocalDateTime.now().minusDays(1));
        var savedSession = assessmentSessionRepository.save(session);

        // the session is approved and HAS scored results, but only for LTE10 -- not NME10
        var ltAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.LTE10.getCode()));
        var scoredStudent = createMockStudentEntity(ltAssessment);
        scoredStudent.setProficiencyScore(3);
        studentRepository.save(scoredStudent);

        var nmeAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));
        var unscoredStudent = createMockStudentEntity(nmeAssessment);
        studentRepository.save(unscoredStudent);

        // the generic isSessionReportAvailable(sessionID) check would say true here (active students exist
        // in the session), but NME_ITEM_ANALYSIS specifically has no scored NME10 student -- this is exactly
        // the "approved session, headers-only CSV" scenario being fixed
        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + NME_ITEM_ANALYSIS.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        // sanity check: LTE10 item analysis for the same session correctly reports true
        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + LTE10_ITEM_ANALYSIS.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @ParameterizedTest
    @CsvSource({
            "NME10",
            "NMF10",
            "LTE10",
            "LTE12",
            "LTP10",
            "LTP12",
            "LTF12"
    })
    void testCheckSessionReportAvailability_ItemAnalysis_WhenScoredStudentExists_ReturnsTrue(String assessmentTypeCode) throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        session.setCompletionDate(LocalDateTime.now().minusDays(1));
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, assessmentTypeCode));

        var student = createMockStudentEntity(savedAssessment);
        student.setProficiencyScore(2);
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/" + assessmentTypeCode + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    // ---- checkSchoolReportTypeAvailability ----

    @Test
    void testCheckSchoolReportTypeAvailability_XamFile_WhenResultsExist_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));

        var schoolID = UUID.randomUUID();
        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(schoolID);
        student.setProficiencyScore(3);
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/school/" + schoolID + "/" + XAM_FILE.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSchoolReportTypeAvailability_XamFile_WhenNoResults_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/school/" + UUID.randomUUID() + "/" + XAM_FILE.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckSchoolReportTypeAvailability_DoarSummary_WhenResultsExist_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.LTE10.getCode()));

        var schoolID = UUID.randomUUID();
        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(schoolID);
        student.setProficiencyScore(2);
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/school/" + schoolID + "/" + DOAR_SUMMARY.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSchoolReportTypeAvailability_DetailedDoar_WhenNoResults_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.LTE10.getCode()));

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/school/" + UUID.randomUUID() + "/" + LTE10_DETAILED_DOAR.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @ParameterizedTest
    @CsvSource({
            "NME10,nme-detailed-doar",
            "NMF10,nmf-detailed-doar",
            "LTE10,lte10-detailed-doar",
            "LTE12,lte12-detailed-doar",
            "LTP10,ltp10-detailed-doar",
            "LTP12,ltp12-detailed-doar",
            "LTF12,ltf12-detailed-doar"
    })
    void testCheckSchoolReportTypeAvailability_DetailedDoar_WhenResultsExist_ReturnsTrue(String assessmentTypeCode, String urlTypeCode) throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, assessmentTypeCode));

        var schoolID = UUID.randomUUID();
        var student = createMockStudentEntity(savedAssessment);
        student.setSchoolAtWriteSchoolID(schoolID);
        student.setProvincialSpecialCaseCode("X");
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + savedSession.getSessionID() + "/school/" + schoolID + "/" + urlTypeCode + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckSchoolReportTypeAvailability_WithoutPermission_ShouldReturn403() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRONG_PERMISSION";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + UUID.randomUUID() + "/school/" + UUID.randomUUID() + "/" + XAM_FILE.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // ---- checkStudentReportAvailability ----

    @Test
    void testCheckStudentReportAvailability_WhenWrittenAssessmentsExist_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.LTE10.getCode()));

        var student = createMockStudentEntity(savedAssessment);
        student.setProficiencyScore(3);
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/student/" + student.getStudentID() + "/" + AssessmentStudentReportTypeCode.ISR.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckStudentReportAvailability_WhenNoWrittenAssessments_ReturnsFalse() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.LTE10.getCode()));

        var student = createMockStudentEntity(savedAssessment);
        // no proficiencyScore, no provincialSpecialCaseCode set
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/student/" + student.getStudentID() + "/" + AssessmentStudentReportTypeCode.ISR.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void testCheckStudentReportAvailability_WithSpecialCaseCode_ReturnsTrue() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var session = createMockSessionEntity();
        var savedSession = assessmentSessionRepository.save(session);
        var savedAssessment = assessmentRepository.save(createMockAssessmentEntity(savedSession, AssessmentTypeCodes.NME10.getCode()));

        var student = createMockStudentEntity(savedAssessment);
        student.setProvincialSpecialCaseCode("X");
        studentRepository.save(student);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/student/" + student.getStudentID() + "/" + AssessmentStudentReportTypeCode.ISR.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckStudentReportAvailability_WithoutPermission_ShouldReturn403() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_WRONG_PERMISSION";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/student/" + UUID.randomUUID() + "/" + AssessmentStudentReportTypeCode.ISR.getCode() + "/available")
                                .with(mockAuthority))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetDistrictSchoolsWithResults_ShouldReturnSchoolsThatHaveResults() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var schoolWithResults = this.createMockSchool();
        schoolWithResults.setDistrictId(district.getDistrictId());
        var schoolWithoutResults = this.createMockSchool();
        schoolWithoutResults.setDistrictId(district.getDistrictId());
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(schoolWithResults, schoolWithoutResults));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        var student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(schoolWithResults.getSchoolId()));
        student.setProficiencyScore(2);
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/district/" + district.getDistrictId() + "/schools-with-results")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val schoolIDs = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<UUID>>() {
        });

        assertThat(schoolIDs).containsExactly(UUID.fromString(schoolWithResults.getSchoolId()));
    }

    @Test
    void testGetDistrictSchoolsWithResults_WhenNoSchoolsInDistrict_ShouldReturnEmptyList() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var otherDistrictSchool = this.createMockSchool();
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(otherDistrictSchool));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        var student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(otherDistrictSchool.getSchoolId()));
        student.setProficiencyScore(2);
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/district/" + district.getDistrictId() + "/schools-with-results")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val schoolIDs = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<UUID>>() {
        });

        assertThat(schoolIDs).isEmpty();
    }

    @Test
    void testGetDistrictSchoolsWithResults_WhenNoStudentsHaveResults_ShouldReturnEmptyList() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var school = this.createMockSchool();
        school.setDistrictId(district.getDistrictId());
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        var student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setProficiencyScore(null);
        student.setProvincialSpecialCaseCode(null);
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/district/" + district.getDistrictId() + "/schools-with-results")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val schoolIDs = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<UUID>>() {
        });

        assertThat(schoolIDs).isEmpty();
    }

    @Test
    void testGetDistrictSchoolsWithResults_ShouldExcludeIndependentAndOffshoreSchools() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var district = this.createMockDistrict();
        var publicSchool = this.createMockSchool();
        publicSchool.setDistrictId(district.getDistrictId());
        publicSchool.setSchoolCategoryCode("PUBLIC");
        var independentSchool = this.createMockSchool();
        independentSchool.setDistrictId(district.getDistrictId());
        independentSchool.setSchoolCategoryCode("INDEPEND");
        var offshoreSchool = this.createMockSchool();
        offshoreSchool.setDistrictId(district.getDistrictId());
        offshoreSchool.setSchoolCategoryCode("OFFSHORE");
        when(this.restUtils.getAllSchoolTombstones()).thenReturn(List.of(publicSchool, independentSchool, offshoreSchool));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP10.getCode()));

        for (var school : List.of(publicSchool, independentSchool, offshoreSchool)) {
            var student = createMockStudentEntity(assessment);
            student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
            student.setProficiencyScore(2);
            studentRepository.save(student);
        }

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/district/" + district.getDistrictId() + "/schools-with-results")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val schoolIDs = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), new TypeReference<List<UUID>>() {
        });

        assertThat(schoolIDs).containsExactly(UUID.fromString(publicSchool.getSchoolId()));
    }

}
