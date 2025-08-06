package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentRegistrationTotalsBySchoolHeader;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentHistoryRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.SimpleHeadcountResultsTable;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@Slf4j
class SummaryReportServiceTest extends BaseAssessmentAPITest {

    @Autowired
    AssessmentSessionRepository assessmentSessionRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    AssessmentStudentRepository assessmentStudentRepository;
    @Autowired
    AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    @Autowired
    RestUtils restUtils;
    @Autowired
    SummaryReportService summaryReportService;

    @AfterEach
    void after() {
        assessmentStudentHistoryRepository.deleteAll();
        assessmentStudentRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();
    }

    @Test
    void testGetAssessmentRegistrationTotalsBySchool_GivenInvalidSessionID_ShouldThrowEntityNotFoundException() {
        UUID sessionID = UUID.randomUUID();
        boolean sessionIdExists = assessmentSessionRepository.existsById(sessionID);
        assertThat(sessionIdExists).isFalse();
        assertThrows(EntityNotFoundException.class, () -> summaryReportService.getAssessmentRegistrationTotalsBySchool(sessionID));
    }

    @Test
    void testGetAssessmentRegistrationTotalsBySchool_GivenValidSessionIdAndNoData_ShouldGenerateHeadersOnly() {
        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
        SimpleHeadcountResultsTable results = summaryReportService.getAssessmentRegistrationTotalsBySchool(assessmentSessionEntity.getSessionID());
        assertThat(results.getHeaders()).isNotEmpty();
        assertThat(results.getRows()).isEmpty();
        assertThat(results.getHeaders().get(0)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode());
        assertThat(results.getHeaders().get(1)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode());
        assertThat(results.getHeaders().get(2)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode());
    }

    @Test
    void testGetAssessmentRegistrationTotalsBySchool_GivenValidSessionIdAndData_ShouldGenerate() {
        UUID schoolID1 = UUID.randomUUID();
        UUID schoolID2 = UUID.randomUUID();
        SchoolTombstone school = this.createMockSchool();
        school.setSchoolId(String.valueOf(schoolID1));
        when(this.restUtils.getSchoolBySchoolID(school.getSchoolId())).thenReturn(Optional.of(school));
        when(this.restUtils.getSchoolBySchoolID(String.valueOf(schoolID2))).thenReturn(Optional.empty());

        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, "LTE10"));
        AssessmentStudentEntity assessmentStudentEntity1 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity1.setSchoolOfRecordSchoolID(schoolID1);
        assessmentStudentEntity1.setGradeAtRegistration("10");
        AssessmentStudentEntity assessmentStudentEntity2 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity2.setSchoolOfRecordSchoolID(schoolID1);
        assessmentStudentEntity2.setGradeAtRegistration("10");
        AssessmentStudentEntity assessmentStudentEntity3 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity3.setSchoolOfRecordSchoolID(schoolID1);
        assessmentStudentEntity3.setGradeAtRegistration("12");
        AssessmentStudentEntity assessmentStudentEntity4 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity4.setSchoolOfRecordSchoolID(schoolID2);
        assessmentStudentEntity4.setGradeAtRegistration("10");

        assessmentStudentRepository.saveAll(List.of(assessmentStudentEntity1, assessmentStudentEntity2, assessmentStudentEntity3, assessmentStudentEntity4));

        SimpleHeadcountResultsTable results = summaryReportService.getAssessmentRegistrationTotalsBySchool(assessmentSessionEntity.getSessionID());
        assertThat(results.getHeaders()).isNotEmpty();
        assertThat(results.getRows()).isNotEmpty();
        assertThat(results.getHeaders().get(0)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode());
        assertThat(results.getHeaders().get(1)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode());
        assertThat(results.getHeaders().get(2)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode());
        assertThat(results.getHeaders().get(3)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_12_COUNT.getCode());
        assertThat(results.getHeaders().get(4)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode());
        Map<String, String> row1 = results.getRows().stream().filter(r -> r.get(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode()).equals(school.getMincode())).findFirst().orElse(null);
        assertThat(row1).isNotNull()
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode(), "LTE10")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode(), "2")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_12_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode(), "3");
        Map<String, String> row2 = results.getRows().stream().filter(r -> r.get(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode()).equals(String.valueOf(schoolID2))).findFirst().orElse(null);
        assertThat(row2).isNotNull()
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode(), "LTE10")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_12_COUNT.getCode(), "0")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode(), "1");
    }

    @Test
    void testGetAssessmentRegistrationTotalsBySchool_GivenDownloadedStudentsInSession_OnlyIncludesDownloadedStudents() {
        UUID schoolID = UUID.randomUUID();
        SchoolTombstone school = this.createMockSchool();
        school.setSchoolId(String.valueOf(schoolID));
        when(this.restUtils.getSchoolBySchoolID(school.getSchoolId())).thenReturn(Optional.of(school));

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, "LTE10"));
        AssessmentStudentEntity assessmentStudentEntity1 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity1.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity1.setGradeAtRegistration("10");
        assessmentStudentEntity1.setDownloadDate(yesterday);
        AssessmentStudentEntity assessmentStudentEntity2 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity2.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity2.setGradeAtRegistration("11");
        assessmentStudentEntity2.setDownloadDate(yesterday);
        AssessmentStudentEntity assessmentStudentEntity3 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity3.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity3.setGradeAtRegistration("10");
        assessmentStudentEntity3.setDownloadDate(null);
        AssessmentStudentEntity assessmentStudentEntity4 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity4.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity4.setGradeAtRegistration("11");
        assessmentStudentEntity4.setDownloadDate(yesterday);
        AssessmentStudentEntity assessmentStudentEntity5 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity5.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity5.setGradeAtRegistration("12");
        assessmentStudentEntity5.setDownloadDate(null);

        assessmentStudentRepository.saveAll(List.of(assessmentStudentEntity1, assessmentStudentEntity2, assessmentStudentEntity3, assessmentStudentEntity4, assessmentStudentEntity5));

        SimpleHeadcountResultsTable results = summaryReportService.getAssessmentRegistrationTotalsBySchool(assessmentSessionEntity.getSessionID());
        assertThat(results.getHeaders()).isNotEmpty();
        assertThat(results.getRows()).isNotEmpty();
        assertThat(results.getHeaders().get(0)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode());
        assertThat(results.getHeaders().get(1)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode());
        assertThat(results.getHeaders().get(2)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode());
        assertThat(results.getHeaders().get(3)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_11_COUNT.getCode());
        assertThat(results.getHeaders().get(4)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode());
        Map<String, String> row1 = results.getRows().getFirst();
        assertThat(row1)
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode(), "LTE10")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode(), school.getMincode())
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_11_COUNT.getCode(), "2")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode(), "3");
    }

    @Test
    void testGetAssessmentRegistrationTotalsBySchool_AllGradesAdded() {
        UUID schoolID = UUID.randomUUID();
        SchoolTombstone school = this.createMockSchool();
        school.setSchoolId(String.valueOf(schoolID));
        when(this.restUtils.getSchoolBySchoolID(school.getSchoolId())).thenReturn(Optional.of(school));

        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, "LTE10"));
        AssessmentStudentEntity assessmentStudentEntity1 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity1.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity1.setGradeAtRegistration("07");
        AssessmentStudentEntity assessmentStudentEntity2 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity2.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity2.setGradeAtRegistration("08");
        AssessmentStudentEntity assessmentStudentEntity3 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity3.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity3.setGradeAtRegistration("09");
        AssessmentStudentEntity assessmentStudentEntity4 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity4.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity4.setGradeAtRegistration("10");
        AssessmentStudentEntity assessmentStudentEntity5 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity5.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity5.setGradeAtRegistration("11");
        AssessmentStudentEntity assessmentStudentEntity6 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity6.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity6.setGradeAtRegistration("12");
        AssessmentStudentEntity assessmentStudentEntity7 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity7.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity7.setGradeAtRegistration("AD");
        AssessmentStudentEntity assessmentStudentEntity8 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity8.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity8.setGradeAtRegistration("OT");
        AssessmentStudentEntity assessmentStudentEntity9 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity9.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity9.setGradeAtRegistration("HS");
        AssessmentStudentEntity assessmentStudentEntity10 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity10.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity10.setGradeAtRegistration("AN");
        AssessmentStudentEntity assessmentStudentEntity11 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity11.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity11.setGradeAtRegistration(null);

        assessmentStudentRepository.saveAll(List.of(assessmentStudentEntity1, assessmentStudentEntity2, assessmentStudentEntity3, assessmentStudentEntity4, assessmentStudentEntity5, assessmentStudentEntity6, assessmentStudentEntity7, assessmentStudentEntity8, assessmentStudentEntity9, assessmentStudentEntity10, assessmentStudentEntity11));

        SimpleHeadcountResultsTable results = summaryReportService.getAssessmentRegistrationTotalsBySchool(assessmentSessionEntity.getSessionID());
        assertThat(results.getHeaders()).isNotEmpty();
        assertThat(results.getRows()).isNotEmpty();
        assertThat(results.getHeaders().get(0)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode());
        assertThat(results.getHeaders().get(1)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode());
        assertThat(results.getHeaders().get(2)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_08_COUNT.getCode());
        assertThat(results.getHeaders().get(3)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_09_COUNT.getCode());
        assertThat(results.getHeaders().get(4)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode());
        assertThat(results.getHeaders().get(5)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_11_COUNT.getCode());
        assertThat(results.getHeaders().get(6)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_12_COUNT.getCode());
        assertThat(results.getHeaders().get(7)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_AD_COUNT.getCode());
        assertThat(results.getHeaders().get(8)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_OT_COUNT.getCode());
        assertThat(results.getHeaders().get(9)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_HS_COUNT.getCode());
        assertThat(results.getHeaders().get(10)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.GRADE_AN_COUNT.getCode());
        assertThat(results.getHeaders().get(11)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.BLANK_GRADE_COUNT.getCode());
        assertThat(results.getHeaders().get(12)).isEqualTo(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode());
        Map<String, String> row1 = results.getRows().getFirst();
        assertThat(row1)
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode(), "LTE10")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode(), school.getMincode())
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_08_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_09_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_11_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_12_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_AD_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_OT_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_HS_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_AN_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.BLANK_GRADE_COUNT.getCode(), "2")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode(), "11");
    }
    @Test
    void testGetAssessmentRegistrationTotalsBySchool_ExcludesHeadersWithZeroCount() {
        UUID schoolID = UUID.randomUUID();
        SchoolTombstone school = this.createMockSchool();
        school.setSchoolId(String.valueOf(schoolID));
        when(this.restUtils.getSchoolBySchoolID(school.getSchoolId())).thenReturn(Optional.of(school));

        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, "LTE10"));
        AssessmentStudentEntity assessmentStudentEntity1 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity1.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity1.setGradeAtRegistration("10");
        assessmentStudentEntity1.setDownloadDate(yesterday);
        AssessmentStudentEntity assessmentStudentEntity2 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity2.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity2.setGradeAtRegistration("11");
        assessmentStudentEntity2.setDownloadDate(yesterday);
        AssessmentStudentEntity assessmentStudentEntity3 = createMockStudentEntity(assessmentEntity);
        assessmentStudentEntity3.setSchoolOfRecordSchoolID(schoolID);
        assessmentStudentEntity3.setGradeAtRegistration("08");
        assessmentStudentEntity3.setDownloadDate(null);

        assessmentStudentRepository.saveAll(List.of(assessmentStudentEntity1, assessmentStudentEntity2, assessmentStudentEntity3));

        SimpleHeadcountResultsTable results = summaryReportService.getAssessmentRegistrationTotalsBySchool(assessmentSessionEntity.getSessionID());
        assertThat(results.getHeaders()).isNotEmpty();
        assertThat(results.getRows()).isNotEmpty();
        assertThat(results.getHeaders()).contains(
                AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.GRADE_11_COUNT.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode());
        assertThat(results.getHeaders()).doesNotContain(
                AssessmentRegistrationTotalsBySchoolHeader.GRADE_08_COUNT.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.GRADE_09_COUNT.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.GRADE_12_COUNT.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.GRADE_AD_COUNT.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.GRADE_OT_COUNT.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.GRADE_HS_COUNT.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.GRADE_AN_COUNT.getCode(),
                AssessmentRegistrationTotalsBySchoolHeader.BLANK_GRADE_COUNT.getCode());
        Map<String, String> row1 = results.getRows().getFirst();
        assertThat(row1)
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode(), "LTE10")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode(), school.getMincode())
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.GRADE_11_COUNT.getCode(), "1")
                .containsEntry(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode(), "2");
    }
}
