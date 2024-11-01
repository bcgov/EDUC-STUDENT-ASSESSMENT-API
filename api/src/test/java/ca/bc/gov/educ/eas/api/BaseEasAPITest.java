package ca.bc.gov.educ.eas.api;

import ca.bc.gov.educ.eas.api.constants.v1.AssessmentStudentStatusCodes;
import ca.bc.gov.educ.eas.api.model.v1.*;
import ca.bc.gov.educ.eas.api.properties.ApplicationProperties;
import ca.bc.gov.educ.eas.api.struct.external.institute.v1.*;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudentGet;
import ca.bc.gov.educ.eas.api.struct.v1.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;

@SpringBootTest(classes = {EasApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class BaseEasAPITest {

  @BeforeEach
  public void before() {

  }

  @AfterEach
  public void resetState() {

  }

  public Session createMockSession() {
    LocalDateTime currentDate = LocalDateTime.now();
    return Session.builder()
            .sessionID(UUID.randomUUID().toString())
            .schoolYear(String.valueOf(currentDate.getYear()))
            .courseYear(currentDate.getYear())
            .courseMonth(currentDate.getMonthValue())
            .activeFromDate(currentDate.minusMonths(2).toString())
            .activeUntilDate(currentDate.plusMonths(2).toString())
            .build();
  }

  public SessionEntity createMockSessionEntity() {
    LocalDateTime currentDate = LocalDateTime.now();
    return SessionEntity.builder()
            .sessionID(UUID.randomUUID())
            .schoolYear(String.valueOf(currentDate.getYear()))
            .courseYear(Integer.toString(currentDate.getYear()))
            .courseMonth(Integer.toString(currentDate.getMonthValue()))
            .activeFromDate(currentDate.minusMonths(2))
            .activeUntilDate(currentDate.plusMonths(2))
            .createUser(ApplicationProperties.EAS_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.EAS_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

  public AssessmentEntity createMockAssessmentEntity(SessionEntity sessionEntity, String assessmentTypeCode) {
    return AssessmentEntity.builder()
            .sessionEntity(sessionEntity)
            .assessmentTypeCode(assessmentTypeCode)
            .createUser(ApplicationProperties.EAS_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.EAS_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

  public List<AssessmentSessionCriteriaEntity> createMockAssessmentSessionCriteriaEntities() {
    List<AssessmentSessionCriteriaEntity> sessionCriteriaEntities = new ArrayList<>();

    AssessmentSessionCriteriaEntity novSession = AssessmentSessionCriteriaEntity.builder()
            .assessmentSessionCriteriaId(UUID.randomUUID())
            .sessionStart(LocalDateTime.of(2024, 10, 1, 0, 0))
            .sessionEnd(LocalDateTime.of(2024, 11, 30, 0, 0))
            .effectiveDate(LocalDateTime.of(2023, 1, 1, 0, 0))
            .expiryDate(LocalDateTime.of(2024, 12, 31, 0, 0))
            .createUser(ApplicationProperties.EAS_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.EAS_API)
            .updateDate(LocalDateTime.now())
            .build();
    sessionCriteriaEntities.add(novSession);

    AssessmentSessionCriteriaEntity juneSession = AssessmentSessionCriteriaEntity.builder()
            .assessmentSessionCriteriaId(UUID.randomUUID())
            .sessionStart(LocalDateTime.of(2025, 5, 1, 0, 0))
            .sessionEnd(LocalDateTime.of(2025, 6, 30, 0, 0))
            .effectiveDate(LocalDateTime.of(2023, 1, 1, 0, 0))
            .expiryDate(LocalDateTime.of(2024, 12, 31, 0, 0))
            .createUser(ApplicationProperties.EAS_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.EAS_API)
            .updateDate(LocalDateTime.now())
            .build();
    sessionCriteriaEntities.add(juneSession);

    return sessionCriteriaEntities;
  }

  public AssessmentTypeCodeEntity createMockAssessmentTypeCodeEntity(String assessmentTypeCode){
    return AssessmentTypeCodeEntity.builder()
            .assessmentTypeCode(assessmentTypeCode)
            .label(assessmentTypeCode)
            .description("This is a test code for assessment type.")
            .displayOrder(1)
            .effectiveDate(LocalDateTime.now())
            .expiryDate(LocalDateTime.now().plusYears(1))
            .language("EN")
            .createUser("TEST-USER")
            .createDate(LocalDateTime.now())
            .updateUser("TEST-USER")
            .updateDate(LocalDateTime.now())
            .build();
  }

  public Set<AssessmentCriteriaEntity> createMockAssessmentSessionTypeCodeCriteriaEntities(List<AssessmentSessionCriteriaEntity> sessionCriteriaEntities, AssessmentTypeCodeEntity assessmentTypeCodeEntity) {
    AssessmentSessionCriteriaEntity novSession = sessionCriteriaEntities.stream()
            .filter(entity -> entity.getSessionStart().getMonthValue() == 10)
            .findFirst()
            .orElseThrow();

    Set<AssessmentCriteriaEntity> typeCodeCriteriaEntities = new HashSet<>();

    AssessmentCriteriaEntity assessmentCriteriaEntity = AssessmentCriteriaEntity.builder()
            .assessmentCriteriaId(UUID.randomUUID())
            .assessmentSessionCriteriaEntity(novSession)
            .assessmentTypeCodeEntity(assessmentTypeCodeEntity)
            .effectiveDate(LocalDateTime.of(2023, 1, 1, 0, 0))
            .expiryDate(LocalDateTime.of(2025, 12, 31, 0, 0))
            .createUser(ApplicationProperties.EAS_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.EAS_API)
            .updateDate(LocalDateTime.now())
            .build();

    typeCodeCriteriaEntities.add(assessmentCriteriaEntity);
    return typeCodeCriteriaEntities;
  }

  public AssessmentStudent createMockStudent() {
    return AssessmentStudent.builder()
            .assessmentStudentID(UUID.randomUUID().toString())
            .assessmentID(UUID.randomUUID().toString())
            .schoolID(UUID.randomUUID().toString())
            .studentID(UUID.randomUUID().toString())
            .pen("120164447")
            .localID("123")
            .createUser(ApplicationProperties.EAS_API)
            .updateUser(ApplicationProperties.EAS_API)
            .build();
  }

  public AssessmentStudentEntity createMockStudentEntity(AssessmentEntity assessmentEntity) {
    return AssessmentStudentEntity.builder()
            .assessmentStudentID(UUID.randomUUID())
            .assessmentEntity(assessmentEntity)
            .assessmentStudentStatusCode(AssessmentStudentStatusCodes.LOADED.getCode())
            .schoolID(UUID.randomUUID())
            .studentID(UUID.randomUUID())
            .pen("120164447")
            .localID("123")
            .createUser(ApplicationProperties.EAS_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.EAS_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

  public SchoolTombstone createMockSchoolTombstone() {
    return SchoolTombstone.builder()
            .schoolId(UUID.randomUUID().toString())
            .mincode("123456")
            .schoolNumber("01001")
            .displayName("Mock School Tombstone 01001")
            .schoolOrganizationCode("QUARTER")
            .schoolCategoryCode("PUBLIC")
            .facilityTypeCode("STANDARD")
            .schoolReportingRequirementCode("REGULAR")
            .openedDate("2018-07-01 00:00:00.000")
            .closedDate(null)
            .build();
  }

  public AssessmentStudentGet createMockAssessmentStudentGet(String assessmentID, String studentID) {
    return AssessmentStudentGet.builder()
            .assessmentID(assessmentID)
            .studentID(studentID)
            .build();
  }


  public SchoolTombstone createMockSchool() {
    final SchoolTombstone schoolTombstone = new SchoolTombstone();
    schoolTombstone.setSchoolId(UUID.randomUUID().toString());
    schoolTombstone.setDistrictId(UUID.randomUUID().toString());
    schoolTombstone.setDisplayName("Marco's school");
    schoolTombstone.setMincode("03636018");
    schoolTombstone.setOpenedDate("1964-09-01T00:00:00");
    schoolTombstone.setSchoolCategoryCode("PUBLIC");
    schoolTombstone.setSchoolReportingRequirementCode("REGULAR");
    schoolTombstone.setFacilityTypeCode("STANDARD");
    return schoolTombstone;
  }

  public School createMockSchoolDetail() {
    final School school = new School();
    school.setSchoolId(UUID.randomUUID().toString());
    school.setDistrictId(UUID.randomUUID().toString());
    school.setDisplayName("Marco's school");
    school.setMincode("03636018");
    school.setOpenedDate("1964-09-01T00:00:00");
    school.setSchoolCategoryCode("PUBLIC");
    school.setSchoolReportingRequirementCode("REGULAR");
    school.setFacilityTypeCode("STANDARD");

    var contactList = new ArrayList<SchoolContact>();
    SchoolContact contact1 = new SchoolContact();
    contact1.setEmail("abc@acb.com");
    contact1.setSchoolContactTypeCode("PRINCIPAL");
    contactList.add(contact1);
    school.setContacts(contactList);

    var gradesList = new ArrayList<SchoolGrade>();
    SchoolGrade grade1 = new SchoolGrade();
    grade1.setSchoolGradeCode("GRADE01");
    gradesList.add(grade1);
    school.setGrades(gradesList);

    return school;
  }

  public District createMockDistrict() {
    final District district = District.builder().build();
    district.setDistrictId(UUID.randomUUID().toString());
    district.setDisplayName("Marco's district");
    district.setDistrictNumber("036");
    district.setDistrictStatusCode("ACTIVE");
    district.setPhoneNumber("123456789");
    return district;
  }

  public IndependentAuthority createMockAuthority() {
    final IndependentAuthority independentAuthority = IndependentAuthority.builder().build();
    independentAuthority.setIndependentAuthorityId(UUID.randomUUID().toString());
    independentAuthority.setDisplayName("Marco's authority");
    independentAuthority.setAuthorityNumber("777");
    independentAuthority.setAuthorityTypeCode("INDEPENDNT");
    independentAuthority.setPhoneNumber("123456789");
    return independentAuthority;
  }

  public static String asJsonString(final Object obj) {
    try {
      ObjectMapper om = new ObjectMapper();
      om.registerModule(new JavaTimeModule()).configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
      return om.writeValueAsString(obj);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
