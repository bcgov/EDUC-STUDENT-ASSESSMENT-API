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
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMerge;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResultSagaData;
import ca.bc.gov.educ.assessment.api.struct.v1.TransferOnApprovalSagaData;
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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
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

    @Test
    void testGetMinistryReport_WithWrongType_ShouldReturnBadRequest() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
        this.mockMvc.perform(get(URL.BASE_URL_REPORT + "/" + UUID.randomUUID() + "/testing/download/JANE").with(mockAuthority))
                .andDo(print()).andExpect(status().isBadRequest());
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
    void testGetDownloadableReportForSchool_WithWrongType_ShouldReturnBadRequest() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
        this.mockMvc.perform(get(URL.BASE_URL_REPORT + "/" + UUID.randomUUID() + "/school/" + UUID.randomUUID() + "/testing/download").with(mockAuthority))
                .andDo(print()).andExpect(status().isBadRequest());
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
    void testGetDownloadableReportForSchoolByAssessment_ValidTypeSessionResults_ShouldReturnCSVFile() throws Exception {
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

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/student/" + student.getStudentID() + "/ISR/download")
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


    @Test
    void testGetDownloadableReport_NmeDetailedDOARBySchool_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "NME10"));

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
        studentRepository.save(studentEntity1);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/nme-detailed-doar/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("NME10");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_NmfDetailedDOARBySchool_ShouldReturnCSVFile() throws Exception {
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
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/nmf-detailed-doar/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("NMF10");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_LTE10DetailedDOARBySchool_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTE10"));

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
        studentRepository.save(studentEntity1);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/lte10-detailed-doar/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("LTE10");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_LTP10DetailedDOARBySchool_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

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
        studentRepository.save(studentEntity1);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/ltp10-detailed-doar/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("LTP10");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_LTF12DetailedDOARBySchool_ShouldReturnCSVFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORT";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseMonth("08");
        AssessmentSessionEntity sessionEntity = assessmentSessionRepository.save(session);
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTF12"));

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
        studentRepository.save(studentEntity1);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/ltf12-detailed-doar/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("LTF12");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_DOARSummaryBySchool_LTF12_ShouldReturnPDFFile() throws Exception {
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
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTF12"));

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

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/doar-summary/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_DOARSummaryBySchool_NME10_ShouldReturnPDFFile() throws Exception {
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
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "NME10"));

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

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/doar-summary/download")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_DOARSummaryBySchool_NMF10_ShouldReturnPDFFile() throws Exception {
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

    @Test
    void testGetDownloadableReport_DOARSummaryBySchool_LTE10_ShouldReturnPDFFile() throws Exception {
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
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTE10"));

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
            "NME10"
    })
    void testGetDownloadableReport_DOARProvincialSummary_LTF12_ShouldReturnPDFFile() throws Exception {
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
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTF12"));

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
            "NME10"
    })
    void testGetDownloadableReport_DOARProvincialSummary_From_Staging_LTF12_ShouldReturnPDFFile() throws Exception {
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
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTF12"));

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

    @Test
    void testGetDownloadableReport_KeySummary_ShouldReturnCSVFile() throws Exception {
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
        assessmentChoiceRepository.save(createMockAssessmentChoiceEntity(savedOpenEndedComp, 2, 1));
        assessmentChoiceRepository.save(createMockAssessmentChoiceEntity(savedOpenEndedComp, 4, 4));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 2));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 2, 3));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 5));
        assessmentQuestionRepository.save(createMockAssessmentQuestionEntity(savedOpenEndedComp, 4, 6));

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/nmf-key-summary/download/" + "TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("NMF10-key-summary");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_LTE10ItemAnalysis_ShouldReturnCSVFile() throws Exception {
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
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTE10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setPen("123456789");
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/LTE10/download/TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("LTE10-data-item-analysis");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_LTE12ItemAnalysis_ShouldReturnCSVFile() throws Exception {
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
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTE12"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setPen("123456789");
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/LTE12/download/TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("LTE12-data-item-analysis");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_LTP10ItemAnalysis_ShouldReturnCSVFile() throws Exception {
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
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP10"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setPen("123456789");
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/LTP10/download/TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("LTP10-data-item-analysis");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_LTP12ItemAnalysis_ShouldReturnCSVFile() throws Exception {
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
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTP12"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setPen("123456789");
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/LTP12/download/TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("LTP12-data-item-analysis");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReport_LTF12ItemAnalysis_ShouldReturnCSVFile() throws Exception {
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
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, "LTF12"));

        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setPen("123456789");
        studentRepository.save(student);

        var resultActions = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/LTF12/download/TESTUSER")
                                .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo("LTF12-data-item-analysis");
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
                .andExpect(content().contentType("text/csv"))
                .andExpect(header().exists("Content-Disposition"));

        String csvContent = resultActions.andReturn().getResponse().getContentAsString();
        String contentDisposition = resultActions.andReturn().getResponse().getHeader("Content-Disposition");

        assertThat(csvContent).isNotBlank();
        assertThat(csvContent).contains("PEN,Surname,Given Name,Grade,School of Record,School at Write,Assessment Code,Assessment Session,Proficiency Score,Special Case");
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
                .andExpect(content().contentType("text/csv"));

        String csvContent = resultActions.andReturn().getResponse().getContentAsString();

        assertThat(csvContent).isNotBlank();
        assertThat(csvContent).contains("PEN,Surname,Given Name,Grade,School of Record,School at Write,Assessment Code,Assessment Session,Proficiency Score,Special Case");
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
                .andExpect(content().contentType("text/csv"));

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
                .andExpect(content().contentType("text/csv"));

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
                .andExpect(content().contentType("text/csv"));

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
                .andExpect(content().contentType("text/csv"));

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
}
