package ca.bc.gov.educ.assessment.api.reports;

import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentLightEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentLightEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentDOARCalculationRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentLightRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.StagedAssessmentStudentDOARCalculationRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.StagedAssessmentStudentLightRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.doar.DOARSummaryNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.doar.DOARSummaryPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DOARProvincialReportServiceTest {
  @Mock
  private AssessmentSessionRepository assessmentSessionRepository;
  @Mock
  private AssessmentStudentLightRepository assessmentStudentLightRepository;
  @Mock
  private StagedAssessmentStudentLightRepository stagedAssessmentStudentLightRepository;
  @Mock
  private AssessmentStudentDOARCalculationRepository assessmentStudentDOARCalculationRepository;
  @Mock
  private StagedAssessmentStudentDOARCalculationRepository stagedAssessmentStudentDOARCalculationRepository;
  @Mock
  private RestUtils restUtils;

  private DOARProvincialReportService service;

  @BeforeEach
  void setUp() {
    service = new DOARProvincialReportService(
      assessmentSessionRepository,
      assessmentStudentLightRepository,
      stagedAssessmentStudentLightRepository,
      assessmentStudentDOARCalculationRepository,
      stagedAssessmentStudentDOARCalculationRepository,
      restUtils
    );
  }

  @Test
  void setStudentLevelsForStaging_ordersReportsByAssessmentTypeCode() {
    UUID schoolId = UUID.randomUUID();
    when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(publicSchool(schoolId)));
    when(stagedAssessmentStudentDOARCalculationRepository.findAllByAssessmentIDAndAssessmentStudentIDIn(any(), anyList()))
      .thenReturn(List.of());

    AssessmentSessionEntity session = session();
    List<StagedAssessmentStudentLightEntity> students = List.of(
      stagedStudent(assessment(session, AssessmentTypeCodes.LTP12.getCode()), schoolId, "A"),
      stagedStudent(assessment(session, AssessmentTypeCodes.LTE10.getCode()), schoolId, "B"),
      stagedStudent(assessment(session, AssessmentTypeCodes.NMF10.getCode()), schoolId, "C")
    );
    DOARSummaryNode root = new DOARSummaryNode();
    root.setReports(new ArrayList<>());

    ReflectionTestUtils.invokeMethod(service, "setStudentLevelsForStaging", students, root, session);

    assertThat(root.getReports()).hasSize(3);
    assertThat(root.getReports().stream().map(DOARSummaryPage::getAssessmentType).toList())
      .containsExactly(
        AssessmentTypeCodes.LTE10.getCode(),
        AssessmentTypeCodes.NMF10.getCode(),
        AssessmentTypeCodes.LTP12.getCode()
      );
  }

  @Test
  void setStudentLevels_ordersReportsByAssessmentTypeCode() {
    UUID schoolId = UUID.randomUUID();
    when(restUtils.getAllSchoolTombstones()).thenReturn(List.of(publicSchool(schoolId)));
    when(assessmentStudentDOARCalculationRepository.findAllByAssessmentIDAndAssessmentStudentIDIn(any(), anyList()))
      .thenReturn(List.of());

    AssessmentSessionEntity session = session();
    List<AssessmentStudentLightEntity> students = List.of(
      student(assessment(session, AssessmentTypeCodes.LTP12.getCode()), schoolId, "A"),
      student(assessment(session, AssessmentTypeCodes.LTE10.getCode()), schoolId, "B"),
      student(assessment(session, AssessmentTypeCodes.NMF10.getCode()), schoolId, "C")
    );
    DOARSummaryNode root = new DOARSummaryNode();
    root.setReports(new ArrayList<>());

    ReflectionTestUtils.invokeMethod(service, "setStudentLevels", students, root, session);

    assertThat(root.getReports()).hasSize(3);
    assertThat(root.getReports().stream().map(DOARSummaryPage::getAssessmentType).toList())
      .containsExactly(
        AssessmentTypeCodes.LTE10.getCode(),
        AssessmentTypeCodes.NMF10.getCode(),
        AssessmentTypeCodes.LTP12.getCode()
      );
  }

  private static AssessmentSessionEntity session() {
    LocalDateTime now = LocalDateTime.now();
    return AssessmentSessionEntity.builder()
      .sessionID(UUID.randomUUID())
      .schoolYear(String.valueOf(now.getYear()))
      .courseYear(Integer.toString(now.getYear()))
      .courseMonth(Integer.toString(now.getMonthValue()))
      .activeFromDate(now.minusMonths(2))
      .activeUntilDate(now.plusMonths(2))
      .createDate(now)
      .updateDate(now)
      .build();
  }

  private static AssessmentEntity assessment(AssessmentSessionEntity session, String typeCode) {
    LocalDateTime now = LocalDateTime.now();
    return AssessmentEntity.builder()
      .assessmentID(UUID.randomUUID())
      .assessmentSessionEntity(session)
      .assessmentTypeCode(typeCode)
      .createDate(now)
      .updateDate(now)
      .build();
  }

  private static StagedAssessmentStudentLightEntity stagedStudent(AssessmentEntity assessment, UUID schoolId, String suffix) {
    LocalDateTime now = LocalDateTime.now();
    return StagedAssessmentStudentLightEntity.builder()
      .assessmentStudentID(UUID.randomUUID())
      .assessmentEntity(assessment)
      .schoolAtWriteSchoolID(schoolId)
      .schoolOfRecordSchoolID(schoolId)
      .assessmentCenterSchoolID(schoolId)
      .studentID(UUID.randomUUID())
      .givenName("Given" + suffix)
      .surname("Surname" + suffix)
      .pen("12016444" + suffix)
      .localID("L" + suffix)
      .proficiencyScore(3)
      .stagedAssessmentStudentStatus(StudentStatusCodes.ACTIVE.getCode())
      .createDate(now)
      .updateDate(now)
      .build();
  }

  private static AssessmentStudentLightEntity student(AssessmentEntity assessment, UUID schoolId, String suffix) {
    return AssessmentStudentLightEntity.builder()
      .assessmentStudentID(UUID.randomUUID())
      .assessmentEntity(assessment)
      .schoolAtWriteSchoolID(schoolId)
      .schoolOfRecordSchoolID(schoolId)
      .givenName("Given" + suffix)
      .surname("Surname" + suffix)
      .pen("22016444" + suffix)
      .localID("L" + suffix)
      .proficiencyScore(3)
      .studentStatusCode(StudentStatusCodes.ACTIVE.getCode())
      .build();
  }

  private static SchoolTombstone publicSchool(UUID schoolId) {
    SchoolTombstone school = new SchoolTombstone();
    school.setSchoolId(schoolId.toString());
    school.setSchoolCategoryCode("PUBLIC");
    return school;
  }
}
