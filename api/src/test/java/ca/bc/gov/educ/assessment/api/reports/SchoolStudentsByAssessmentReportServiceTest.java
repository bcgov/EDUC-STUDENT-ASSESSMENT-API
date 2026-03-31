package ca.bc.gov.educ.assessment.api.reports;

import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.StagedAssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.byAssessment.SchoolStudentNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.byAssessment.SchoolStudentReportNode;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.student.byAssessment.SchoolStudentRootNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchoolStudentsByAssessmentReportServiceTest {

  @Mock
  private AssessmentSessionRepository assessmentSessionRepository;
  @Mock
  private AssessmentStudentRepository assessmentStudentRepository;
  @Mock
  private RestUtils restUtils;
  @Mock
  private CodeTableService codeTableService;
  @Mock
  private StagedAssessmentStudentRepository stagedAssessmentStudentRepository;

  private SchoolStudentsByAssessmentReportService service;

  @BeforeEach
  void setUp() {
    service = new SchoolStudentsByAssessmentReportService(
        assessmentSessionRepository,
        assessmentStudentRepository,
        restUtils,
        codeTableService,
        stagedAssessmentStudentRepository);
  }

  @Test
  void populateStudentForApproval_ordersReportsByAssessmentTypeOrdinal() {
    UUID schoolId = UUID.randomUUID();
    SchoolTombstone school = schoolTombstone(schoolId);
    District district = district(school.getDistrictId());
    when(restUtils.getSchoolBySchoolID(schoolId.toString())).thenReturn(Optional.of(school));
    when(restUtils.getDistrictByDistrictID(school.getDistrictId())).thenReturn(Optional.of(district));
    when(codeTableService.getAllAssessmentTypeCodesAsMap()).thenReturn(Map.of(
        AssessmentTypeCodes.LTE10.getCode(), "LTE10",
        AssessmentTypeCodes.LTP12.getCode(), "LTP12"));

    AssessmentSessionEntity session = session();
    AssessmentEntity lte10 = assessment(session, AssessmentTypeCodes.LTE10.getCode());
    AssessmentEntity ltp12 = assessment(session, AssessmentTypeCodes.LTP12.getCode());

    AssessmentStudentEntity studentLte = student(lte10, "A", "Zebra");
    AssessmentStudentEntity studentLtp = student(ltp12, "B", "Alpha");
    List<AssessmentStudentEntity> students = List.of(studentLtp, studentLte);

    SchoolStudentRootNode root = ReflectionTestUtils.invokeMethod(
        service,
        "populateStudentForApproval",
        session,
        schoolId,
        students);

    Assertions.assertNotNull(root);
    assertThat(root.getReports()).hasSize(2);
    List<String> typeLabels = root.getReports().stream()
        .map(SchoolStudentReportNode::getAssessmentType)
        .toList();
    assertThat(typeLabels).containsExactly("LTE10", "LTP12");
  }

  @Test
  void populateStudentForApproval_sortsStudentsByNameWithinEachAssessment() {
    UUID schoolId = UUID.randomUUID();
    SchoolTombstone school = schoolTombstone(schoolId);
    District district = district(school.getDistrictId());
    when(restUtils.getSchoolBySchoolID(schoolId.toString())).thenReturn(Optional.of(school));
    when(restUtils.getDistrictByDistrictID(school.getDistrictId())).thenReturn(Optional.of(district));
    when(codeTableService.getAllAssessmentTypeCodesAsMap()).thenReturn(Map.of(
        AssessmentTypeCodes.LTP10.getCode(), "LTP10"));

    AssessmentSessionEntity session = session();
    AssessmentEntity ltp10 = assessment(session, AssessmentTypeCodes.LTP10.getCode());
    List<AssessmentStudentEntity> students = List.of(
        student(ltp10, "Bob", "Zebra"),
        student(ltp10, "Ann", "Alpha"));

    SchoolStudentRootNode root = ReflectionTestUtils.invokeMethod(
        service,
        "populateStudentForApproval",
        session,
        schoolId,
        students);

    Assertions.assertNotNull(root);
    assertThat(root.getReports()).hasSize(1);
    assertThat(root.getReports().getFirst().getStudents().stream().map(SchoolStudentNode::getName).toList())
        .containsExactly("Alpha, Ann", "Zebra, Bob");
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
        .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
        .createDate(now)
        .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
        .updateDate(now)
        .build();
  }

  private static AssessmentEntity assessment(AssessmentSessionEntity session, String typeCode) {
    LocalDateTime now = LocalDateTime.now();
    return AssessmentEntity.builder()
        .assessmentSessionEntity(session)
        .assessmentTypeCode(typeCode)
        .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
        .createDate(now)
        .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
        .updateDate(now)
        .build();
  }

  private static AssessmentStudentEntity student(AssessmentEntity assessment, String givenName, String surname) {
    LocalDateTime now = LocalDateTime.now();
    return AssessmentStudentEntity.builder()
        .assessmentStudentID(UUID.randomUUID())
        .assessmentEntity(assessment)
        .schoolOfRecordSchoolID(UUID.randomUUID())
        .assessmentCenterSchoolID(UUID.randomUUID())
        .studentID(UUID.randomUUID())
        .givenName(givenName)
        .surname(surname)
        .pen("120164447")
        .localID("123")
        .studentStatusCode(StudentStatusCodes.ACTIVE.toString())
        .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
        .createDate(now)
        .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
        .updateDate(now)
        .build();
  }

  private static SchoolTombstone schoolTombstone(UUID schoolId) {
    SchoolTombstone school = new SchoolTombstone();
    school.setSchoolId(schoolId.toString());
    school.setDistrictId(UUID.randomUUID().toString());
    school.setDisplayName("Test school");
    school.setMincode("03636018");
    school.setSchoolCategoryCode("PUBLIC");
    school.setSchoolReportingRequirementCode("REGULAR");
    school.setFacilityTypeCode("STANDARD");
    school.setSchoolNumber("360");
    school.setSchoolOrganizationCode("STANDARD");
    return school;
  }

  private static District district(String districtId) {
    District d = District.builder().build();
    d.setDistrictId(districtId);
    d.setDisplayName("Test district");
    d.setDistrictNumber("036");
    d.setDistrictStatusCode("ACTIVE");
    d.setPhoneNumber("123456789");
    return d;
  }
}
