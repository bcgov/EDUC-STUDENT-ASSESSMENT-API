package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentHistoryRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CSVReportServiceTest extends BaseAssessmentAPITest {

    /**
     * Fixed reference dates used across tests so behaviour is independent of the real current date.
     *
     * BEFORE_APRIL  (March 15, 2026)  → month < 4  → cutoff = April 1, 2025
     * ON_APRIL      (April  1, 2026)  → month == 4 → cutoff = April 1, 2026  (rollover day itself)
     * AFTER_APRIL   (July  15, 2026)  → month > 4  → cutoff = April 1, 2026
     */
    private static final LocalDate BEFORE_APRIL = LocalDate.of(2026, 3, 15);
    private static final LocalDate ON_APRIL     = LocalDate.of(2026, 4, 1);
    private static final LocalDate AFTER_APRIL  = LocalDate.of(2026, 7, 15);

    @Autowired
    private CSVReportService csvReportService;

    @Autowired
    private AssessmentSessionRepository assessmentSessionRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private AssessmentStudentRepository studentRepository;

    @Autowired
    private AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;

    @MockBean
    private RestUtils restUtils;

    private District district;
    private SchoolTombstone school;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        assessmentStudentHistoryRepository.deleteAll();
        studentRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();

        district = createMockDistrict();
        school = createMockSchool();
        school.setDistrictId(district.getDistrictId());

        when(restUtils.getYukonDistrict()).thenReturn(Optional.of(district));
        when(restUtils.getSchools()).thenReturn(List.of(school));
        when(restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a session with a fixed activeFromDate of 2020-01-01 so it always satisfies
     * the repository's activeFromDate <= referenceDate filter regardless of the mocked today.
     */
    private AssessmentSessionEntity createYukonSession(String courseYear, String courseMonth) {
        AssessmentSessionEntity session = createMockSessionEntity();
        session.setCourseYear(courseYear);
        session.setCourseMonth(courseMonth);
        return session;
    }

    private AssessmentStudentEntity createYukonSummaryStudent(AssessmentEntity assessment) {
        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setProficiencyScore(3);
        return student;
    }

    private AssessmentStudentEntity createYukonDetailStudent(AssessmentEntity assessment, String pen) {
        AssessmentStudentEntity student = createMockStudentEntity(assessment);
        student.setSchoolAtWriteSchoolID(UUID.fromString(school.getSchoolId()));
        student.setPen(pen);
        return student;
    }

    private String decodeCsv(DownloadableReportResponse response) {
        return new String(Base64.getDecoder().decode(response.getDocumentData()), StandardCharsets.UTF_8);
    }

    // =========================================================================
    // Summary report — BEFORE_APRIL reference date (March 15, 2026)
    //   cutoff = April 1, 2025
    //   in range:  202504 through 202603
    //   excluded:  < 202504  OR  > 202603
    // =========================================================================

    @Test
    void yukonSummary_beforeApril_sessionOnCutoffDate_isIncluded() {
        AssessmentSessionEntity saved = assessmentSessionRepository.save(createYukonSession("2025", "04"));
        studentRepository.save(createYukonSummaryStudent(
                assessmentRepository.save(createMockAssessmentEntity(saved, "LTE10"))));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(BEFORE_APRIL);
            assertThat(decodeCsv(csvReportService.generateYukonReport())).contains("202504");
        }
    }

    @Test
    void yukonSummary_beforeApril_sessionOneMonthBeforeCutoff_isExcluded() {
        AssessmentSessionEntity saved = assessmentSessionRepository.save(createYukonSession("2025", "03"));
        studentRepository.save(createYukonSummaryStudent(
                assessmentRepository.save(createMockAssessmentEntity(saved, "LTE10"))));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(BEFORE_APRIL);
            assertThat(decodeCsv(csvReportService.generateYukonReport())).doesNotContain("202503");
        }
    }

    @Test
    void yukonSummary_beforeApril_sessionAfterReferenceDate_isExcluded() {
        // April 2026 is after the March 15, 2026 reference date
        AssessmentSessionEntity saved = assessmentSessionRepository.save(createYukonSession("2026", "04"));
        studentRepository.save(createYukonSummaryStudent(
                assessmentRepository.save(createMockAssessmentEntity(saved, "LTE10"))));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(BEFORE_APRIL);
            assertThat(decodeCsv(csvReportService.generateYukonReport())).doesNotContain("202604");
        }
    }

    @Test
    void yukonSummary_beforeApril_multipleSessionsInRange_allProduceRows() {
        // April 2025, November 2025, January 2026 are all in [Apr 2025, Mar 2026]
        AssessmentSessionEntity s1 = assessmentSessionRepository.save(createYukonSession("2025", "04"));
        AssessmentSessionEntity s2 = assessmentSessionRepository.save(createYukonSession("2025", "11"));
        AssessmentSessionEntity s3 = assessmentSessionRepository.save(createYukonSession("2026", "01"));
        studentRepository.saveAll(List.of(
                createYukonSummaryStudent(assessmentRepository.save(createMockAssessmentEntity(s1, "LTE10"))),
                createYukonSummaryStudent(assessmentRepository.save(createMockAssessmentEntity(s2, "LTE10"))),
                createYukonSummaryStudent(assessmentRepository.save(createMockAssessmentEntity(s3, "LTE10")))));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(BEFORE_APRIL);
            String csv = decodeCsv(csvReportService.generateYukonReport());
            assertThat(csv).contains("202504");
            assertThat(csv).contains("202511");
            assertThat(csv).contains("202601");
        }
    }

    @Test
    void yukonSummary_beforeApril_mixedSessions_onlyInRangeIncluded() {
        AssessmentSessionEntity inRange = assessmentSessionRepository.save(createYukonSession("2025", "11"));
        AssessmentSessionEntity outOfRange = assessmentSessionRepository.save(createYukonSession("2025", "03"));
        studentRepository.saveAll(List.of(
                createYukonSummaryStudent(assessmentRepository.save(createMockAssessmentEntity(inRange, "LTE10"))),
                createYukonSummaryStudent(assessmentRepository.save(createMockAssessmentEntity(outOfRange, "LTE10")))));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(BEFORE_APRIL);
            String csv = decodeCsv(csvReportService.generateYukonReport());
            assertThat(csv).contains("202511");
            assertThat(csv).doesNotContain("202503");
        }
    }

    // =========================================================================
    // Summary report — ON_APRIL reference date (April 1, 2026) — rollover
    //   cutoff rolls forward to April 1, 2026
    //   sessions that were in range on March 31 are now excluded
    // =========================================================================

    @Test
    void yukonSummary_onAprilFirst_previousWindowSessions_areNowExcluded() {
        // January 2026 was in range on March 15; after rollover on April 1 it is excluded
        AssessmentSessionEntity saved = assessmentSessionRepository.save(createYukonSession("2026", "01"));
        studentRepository.save(createYukonSummaryStudent(
                assessmentRepository.save(createMockAssessmentEntity(saved, "LTE10"))));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(ON_APRIL);
            assertThat(decodeCsv(csvReportService.generateYukonReport())).doesNotContain("202601");
        }
    }

    @Test
    void yukonSummary_onAprilFirst_newCutoffSession_isIncluded() {
        // April 2026 is both the new cutoff and today — must be included
        AssessmentSessionEntity saved = assessmentSessionRepository.save(createYukonSession("2026", "04"));
        studentRepository.save(createYukonSummaryStudent(
                assessmentRepository.save(createMockAssessmentEntity(saved, "LTE10"))));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(ON_APRIL);
            assertThat(decodeCsv(csvReportService.generateYukonReport())).contains("202604");
        }
    }

    // =========================================================================
    // Summary report — AFTER_APRIL reference date (July 15, 2026)
    //   cutoff = April 1, 2026
    //   in range: 202604 through 202607
    // =========================================================================

    @Test
    void yukonSummary_afterApril_sessionBeforeCurrentYearCutoff_isExcluded() {
        // January 2026 is before April 1, 2026 cutoff
        AssessmentSessionEntity saved = assessmentSessionRepository.save(createYukonSession("2026", "01"));
        studentRepository.save(createYukonSummaryStudent(
                assessmentRepository.save(createMockAssessmentEntity(saved, "LTE10"))));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(AFTER_APRIL);
            assertThat(decodeCsv(csvReportService.generateYukonReport())).doesNotContain("202601");
        }
    }

    @Test
    void yukonSummary_afterApril_sessionsFromAprilToJune_allIncluded() {
        AssessmentSessionEntity s1 = assessmentSessionRepository.save(createYukonSession("2026", "04"));
        AssessmentSessionEntity s2 = assessmentSessionRepository.save(createYukonSession("2026", "06"));
        studentRepository.saveAll(List.of(
                createYukonSummaryStudent(assessmentRepository.save(createMockAssessmentEntity(s1, "LTE10"))),
                createYukonSummaryStudent(assessmentRepository.save(createMockAssessmentEntity(s2, "LTE10")))));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(AFTER_APRIL);
            String csv = decodeCsv(csvReportService.generateYukonReport());
            assertThat(csv).contains("202604");
            assertThat(csv).contains("202606");
        }
    }

    @Test
    void yukonSummary_afterApril_sessionAfterReferenceDate_isExcluded() {
        // November 2026 is after July 15, 2026
        AssessmentSessionEntity saved = assessmentSessionRepository.save(createYukonSession("2026", "11"));
        studentRepository.save(createYukonSummaryStudent(
                assessmentRepository.save(createMockAssessmentEntity(saved, "LTE10"))));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(AFTER_APRIL);
            assertThat(decodeCsv(csvReportService.generateYukonReport())).doesNotContain("202611");
        }
    }

    // =========================================================================
    // Student detail report — mirrors the rollover logic
    // =========================================================================

    @Test
    void yukonDetail_beforeApril_sessionOnCutoffDate_studentsIncluded() {
        AssessmentSessionEntity saved = assessmentSessionRepository.save(createYukonSession("2025", "04"));
        studentRepository.save(createYukonDetailStudent(
                assessmentRepository.save(createMockAssessmentEntity(saved, "LTE10")), "111111110"));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(BEFORE_APRIL);
            assertThat(decodeCsv(csvReportService.generateYukonStudentDetailsReport())).contains("111111110");
        }
    }

    @Test
    void yukonDetail_beforeApril_sessionBeforeCutoff_studentsExcluded() {
        AssessmentSessionEntity saved = assessmentSessionRepository.save(createYukonSession("2025", "03"));
        studentRepository.save(createYukonDetailStudent(
                assessmentRepository.save(createMockAssessmentEntity(saved, "LTE10")), "999999998"));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(BEFORE_APRIL);
            assertThat(decodeCsv(csvReportService.generateYukonStudentDetailsReport())).doesNotContain("999999998");
        }
    }

    @Test
    void yukonDetail_onAprilFirst_previousWindowStudents_areNowExcluded() {
        AssessmentSessionEntity saved = assessmentSessionRepository.save(createYukonSession("2026", "01"));
        studentRepository.save(createYukonDetailStudent(
                assessmentRepository.save(createMockAssessmentEntity(saved, "LTE10")), "999999998"));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(ON_APRIL);
            assertThat(decodeCsv(csvReportService.generateYukonStudentDetailsReport())).doesNotContain("999999998");
        }
    }

    @Test
    void yukonDetail_beforeApril_multipleSessionsInRange_allStudentsPresent() {
        AssessmentSessionEntity s1 = assessmentSessionRepository.save(createYukonSession("2025", "04"));
        AssessmentSessionEntity s2 = assessmentSessionRepository.save(createYukonSession("2025", "11"));
        studentRepository.saveAll(List.of(
                createYukonDetailStudent(assessmentRepository.save(createMockAssessmentEntity(s1, "LTE10")), "111111110"),
                createYukonDetailStudent(assessmentRepository.save(createMockAssessmentEntity(s2, "NME10")), "222222220")));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(BEFORE_APRIL);
            String csv = decodeCsv(csvReportService.generateYukonStudentDetailsReport());
            assertThat(csv).contains("111111110");
            assertThat(csv).contains("222222220");
        }
    }

    @Test
    void yukonDetail_beforeApril_studentInRangeAndBeforeCutoff_onlyInRangePresent() {
        AssessmentSessionEntity inRange = assessmentSessionRepository.save(createYukonSession("2025", "11"));
        AssessmentSessionEntity outOfRange = assessmentSessionRepository.save(createYukonSession("2025", "03"));
        studentRepository.saveAll(List.of(
                createYukonDetailStudent(assessmentRepository.save(createMockAssessmentEntity(inRange, "LTE10")), "111111110"),
                createYukonDetailStudent(assessmentRepository.save(createMockAssessmentEntity(outOfRange, "LTE10")), "999999998")));

        try (MockedStatic<LocalDate> mock = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            mock.when(LocalDate::now).thenReturn(BEFORE_APRIL);
            String csv = decodeCsv(csvReportService.generateYukonStudentDetailsReport());
            assertThat(csv).contains("111111110");
            assertThat(csv).doesNotContain("999999998");
        }
    }
}
