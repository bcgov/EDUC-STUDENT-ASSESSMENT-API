package ca.bc.gov.educ.assessment.api.controller.v1;


import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMerge;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.SimpleHeadcountResultsTable;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
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

import static ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode.SCHOOL_STUDENTS_BY_ASSESSMENT;
import static ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode.SCHOOL_STUDENTS_IN_SESSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    AssessmentRepository assessmentRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RestUtils restUtils;
    @Autowired
    StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    @Autowired
    private AssessmentFormRepository assessmentFormRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        stagedAssessmentStudentRepository.deleteAll();
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
        componentEntity1.setAssessmentFormID(savedForm.getAssessmentFormID());
        componentEntity2.setAssessmentFormID(savedForm.getAssessmentFormID());

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
        componentEntity1.setAssessmentFormID(savedForm.getAssessmentFormID());
        componentEntity2.setAssessmentFormID(savedForm.getAssessmentFormID());

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
}
