package ca.bc.gov.educ.assessment.api.controller.v1;


import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentFormRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMerge;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    AssessmentSessionRepository assessmentSessionRepository;

    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RestUtils restUtils;
    @Autowired
    private AssessmentFormRepository assessmentFormRepository;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        assessmentFormRepository.deleteAll();
        this.studentRepository.deleteAll();
        this.assessmentRepository.deleteAll();
        this.assessmentSessionRepository.deleteAll();
    }

    @Test
    void testGetMinistryReport_WithWrongType_ShouldReturnBadRequest() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORTS";
        final OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);
        this.mockMvc.perform(get(URL.BASE_URL_REPORT + "/" + UUID.randomUUID() + "/testing").with(mockAuthority))
                .andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    void testGetMinistryReport_ValidType_ShouldReturnReportData() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORTS";
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
                        get(URL.BASE_URL_REPORT + "/" + assessmentSessionEntity.getSessionID() + "/" + AssessmentReportTypeCode.ALL_SESSION_REGISTRATIONS.getCode() + "/download").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
        });

        assertThat(summary1).isNotNull();
        assertThat(summary1.getReportType()).isEqualTo(AssessmentReportTypeCode.ALL_SESSION_REGISTRATIONS.getCode());
    }

    @Test
    void testGetMinistryReport_ValidTypeAttempts_ShouldReturnReportData() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORTS";
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
                        get(URL.BASE_URL_REPORT + "/" + assessmentSessionEntity.getSessionID() + "/" + AssessmentReportTypeCode.ATTEMPTS.getCode() + "/download").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
        });

        assertThat(summary1).isNotNull();
        assertThat(summary1.getReportType()).isEqualTo(AssessmentReportTypeCode.ATTEMPTS.getCode());
    }

    @Test
    void testGetMinistryReport_PenMerges_ShouldReturnReportData() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORTS";
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

        var resultActions1 = this.mockMvc.perform(
                        get(URL.BASE_URL_REPORT + "/" + UUID.randomUUID() + "/" + AssessmentReportTypeCode.PEN_MERGES.getCode() + "/download").with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());
        val summary1 = objectMapper.readValue(resultActions1.andReturn().getResponse().getContentAsByteArray(), new TypeReference<DownloadableReportResponse>() {
        });

        assertThat(summary1).isNotNull();
        assertThat(summary1.getReportType()).isEqualTo(AssessmentReportTypeCode.PEN_MERGES.getCode());
    }

    @Test
    void testGetDownloadableReportForSchool_ShouldReturnXamFile() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORTS";
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
                get(URL.BASE_URL_REPORT + "/" + sessionEntity.getSessionID() + "/school/" + school.getSchoolId() + "/download")
                        .with(mockAuthority))
                .andDo(print()).andExpect(status().isOk());

        val summary = objectMapper.readValue(resultActions.andReturn().getResponse().getContentAsByteArray(), DownloadableReportResponse.class);

        assertThat(summary).isNotNull();
        assertThat(summary.getReportType()).isEqualTo(school.getMincode() + "-" + sessionEntity.getCourseYear() + sessionEntity.getCourseMonth() + "-Results" + ".xam");
        assertThat(summary.getDocumentData()).isNotBlank();
    }

    @Test
    void testGetDownloadableReportForSchool_SchoolNotFound_ShouldReturnBadRequest() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_ASSESSMENT_REPORTS";
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
}
