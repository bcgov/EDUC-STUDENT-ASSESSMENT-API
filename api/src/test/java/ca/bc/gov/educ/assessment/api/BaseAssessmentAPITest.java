package ca.bc.gov.educ.assessment.api;

import ca.bc.gov.educ.assessment.api.config.TestDataSourceConfig;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.*;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.Assessment;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentGet;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResultSagaData;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

@SpringBootTest(classes = {StudentAssessmentApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestDataSourceConfig.class)
public abstract class BaseAssessmentAPITest {

  @Autowired
  private ClaimCodeRepository claimCodeRepository;
  @Autowired
  private CognitiveLevelCodeRepository cognitiveLevelCodeRepository;
  @Autowired
  private ConceptsCodeRepository conceptsCodeRepository;
  @Autowired
  private ContextCodeRepository contextCodeRepository;
  @Autowired
  private TaskCodeRepository taskCodeRepository;

  @BeforeEach
  public void before() {
    // Setup test data - Flyway migrations will have already run during app startup
    claimCodeRepository.saveAll(List.of(createClaimCode("C"), createClaimCode("W"), createClaimCode("O")));
    cognitiveLevelCodeRepository.saveAll(List.of(createCognitiveCode("7"), createCognitiveCode("8"), createCognitiveCode("9")));
    var conceptsList = List.of(createConceptsCode("WRB"),
            createConceptsCode("WRA"),
            createConceptsCode("GO"),
            createConceptsCode("WRS"),
            createConceptsCode("WRD"),
            createConceptsCode("WRF"),
            createConceptsCode("O1D"),
            createConceptsCode("O1F")
            );
    conceptsCodeRepository.saveAll(conceptsList);
    contextCodeRepository.save(createContextCode("1"));
    taskCodeRepository.save(createTaskCode("A"));
  }

  @AfterEach
  public void resetState() {
  }

  public ClaimCodeEntity createClaimCode(String code) {
    return ClaimCodeEntity.builder().claimCode(code).description(code)
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.now().plusYears(10)).displayOrder(1).label(code).createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public ConceptCodeEntity createConceptsCode(String code) {
    return ConceptCodeEntity.builder().conceptCode(code).description(code)
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.now().plusYears(10)).displayOrder(1).label(code).createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public ContextCodeEntity createContextCode(String code) {
    return ContextCodeEntity.builder().contextCode(code).description(code)
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.now().plusYears(10)).displayOrder(1).label(code).createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public TaskCodeEntity createTaskCode(String code) {
    return TaskCodeEntity.builder().taskCode(code).description(code)
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.now().plusYears(10)).displayOrder(1).label(code).createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public CognitiveLevelCodeEntity createCognitiveCode(String code) {
    return CognitiveLevelCodeEntity.builder().cognitiveLevelCode(code).description(code)
            .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.now().plusYears(10)).displayOrder(1).label(code).createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build();
  }

  public AssessmentSessionEntity createMockSessionEntity() {
    LocalDateTime currentDate = LocalDateTime.now();
    return AssessmentSessionEntity.builder()
            .sessionID(UUID.randomUUID())
            .schoolYear(String.valueOf(currentDate.getYear()))
            .courseYear(Integer.toString(currentDate.getYear()))
            .courseMonth(Integer.toString(currentDate.getMonthValue()))
            .activeFromDate(currentDate.minusMonths(2))
            .activeUntilDate(currentDate.plusMonths(2))
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .assessments(null)
            .build();
  }

  public AssessmentEntity createMockAssessmentEntity(AssessmentSessionEntity assessmentSessionEntity, String assessmentTypeCode) {
    return AssessmentEntity.builder()
            .assessmentSessionEntity(assessmentSessionEntity)
            .assessmentTypeCode(assessmentTypeCode)
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

  public AssessmentFormEntity createMockAssessmentFormEntity(AssessmentEntity assessmentEntity, String formCode) {
    return AssessmentFormEntity.builder()
            .assessmentEntity(assessmentEntity)
            .formCode(formCode)
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

  public AssessmentComponentEntity createMockAssessmentComponentEntity(AssessmentFormEntity assessmentFormEntity, String componentTypeCode, String componentSubTypeCode) {
    return AssessmentComponentEntity.builder()
            .assessmentFormEntity(assessmentFormEntity)
            .componentSubTypeCode(componentSubTypeCode)
            .componentTypeCode(componentTypeCode)
            .questionCount(1)
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

  public AssessmentQuestionEntity createMockAssessmentQuestionEntity(AssessmentComponentEntity assessmentComponentEntity, Integer questionNumber, Integer itemNumber) {
    return AssessmentQuestionEntity.builder()
            .assessmentComponentEntity(assessmentComponentEntity)
            .itemNumber(itemNumber)
            .questionNumber(questionNumber)
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

  public List<AssessmentSessionCriteriaEntity> createMockAssessmentSessionCriteriaEntities() {
    List<AssessmentSessionCriteriaEntity> sessionCriteriaEntities = new ArrayList<>();

    AssessmentSessionCriteriaEntity novSession = AssessmentSessionCriteriaEntity.builder()
            .assessmentSessionCriteriaId(UUID.randomUUID())
            .sessionStart(createOctoberFirstDate())
            .sessionEnd(createOctoberFirstDate().plusDays(59))
            .effectiveDate(LocalDateTime.now().minusYears(5))
            .expiryDate(LocalDateTime.of(2099, 12, 31, 0, 0))
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .build();
    sessionCriteriaEntities.add(novSession);

    AssessmentSessionCriteriaEntity juneSession = AssessmentSessionCriteriaEntity.builder()
            .assessmentSessionCriteriaId(UUID.randomUUID())
            .sessionStart(createMayFirstDate())
            .sessionEnd(createMayFirstDate().plusDays(59))
            .effectiveDate(LocalDateTime.now().minusYears(5))
            .expiryDate(LocalDateTime.of(2099, 12, 31, 0, 0))
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
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
            .effectiveDate(LocalDateTime.now().minusYears(10))
            .expiryDate(LocalDateTime.now().plusYears(10))
            .language("E")
            .createUser("TEST-USER")
            .createDate(LocalDateTime.now())
            .updateUser("TEST-USER")
            .updateDate(LocalDateTime.now())
            .build();
  }

  public Set<AssessmentCriteriaEntity> createMockAssessmentSessionTypeCodeCriteriaEntities(List<AssessmentSessionCriteriaEntity> sessionCriteriaEntities, String assessmentTypeCode) {
    AssessmentSessionCriteriaEntity novSession = sessionCriteriaEntities.stream()
            .filter(entity -> entity.getSessionStart().getMonthValue() == 10)
            .findFirst()
            .orElseThrow();

    Set<AssessmentCriteriaEntity> typeCodeCriteriaEntities = new HashSet<>();

      AssessmentCriteriaEntity assessmentCriteriaEntity = AssessmentCriteriaEntity.builder()
            .assessmentCriteriaId(UUID.randomUUID())
            .assessmentSessionCriteriaEntity(novSession)
            .assessmentTypeCode(assessmentTypeCode)
            .effectiveDate(LocalDateTime.now().minusYears(5))
            .expiryDate(LocalDateTime.of(2099, 12, 31, 0, 0))
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .build();

    typeCodeCriteriaEntities.add(assessmentCriteriaEntity);
    return typeCodeCriteriaEntities;
  }

  public Assessment createMockAssessment() {
    return Assessment.builder()
            .assessmentID(UUID.randomUUID().toString())
            .sessionID(UUID.randomUUID().toString())
            .assessmentTypeCode(AssessmentTypeCodes.LTF12.getCode())
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .build();
  }

  public AssessmentStudent createMockStudent() {
    return AssessmentStudent.builder()
            .assessmentStudentID(UUID.randomUUID().toString())
            .assessmentID(UUID.randomUUID().toString())
            .schoolOfRecordSchoolID(UUID.randomUUID().toString())
            .assessmentCenterSchoolID(UUID.randomUUID().toString())
            .studentID(UUID.randomUUID().toString())
            .givenName("TestFirst")
            .surname("TestLast")
            .pen("120164447")
            .localID("123")
            .updateDate(LocalDateTime.now().toString())
            .createDate(LocalDateTime.now().toString())
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .status("ACTIVE")
            .build();
  }

  public GradStudentRecord createMockGradStudentAPIRecord() {
    return GradStudentRecord.builder()
            .schoolOfRecordId(UUID.randomUUID().toString())
            .schoolAtGradId(UUID.randomUUID().toString())
            .studentStatusCode("CUR")
            .programCompletionDate("2023-06-30T00:00:00+01:00")
            .graduated("true")
            .studentGrade("10")
            .build();
  }

  public AssessmentStudentEntity createMockStudentEntity(AssessmentEntity assessmentEntity) {
    return AssessmentStudentEntity.builder()
            .assessmentStudentID(UUID.randomUUID())
            .assessmentEntity(assessmentEntity)
            .schoolOfRecordSchoolID(UUID.randomUUID())
            .assessmentCenterSchoolID(UUID.randomUUID())
            .studentID(UUID.randomUUID())
            .givenName("TestFirst")
            .surname("TestLast")
            .pen("120164447")
            .localID("123")
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

  public AssessmentStudentHistoryEntity createMockStudentHistoryEntity(AssessmentStudentEntity assessmentStudentEntity) {
    return AssessmentStudentHistoryEntity.builder()
            .assessmentStudentHistoryID(UUID.randomUUID())
            .assessmentStudentID(assessmentStudentEntity.getAssessmentStudentID())
            .assessmentID(assessmentStudentEntity.getAssessmentEntity().getAssessmentID())
            .assessmentFormID(assessmentStudentEntity.getAssessmentFormID())
            .schoolAtWriteSchoolID(assessmentStudentEntity.getSchoolAtWriteSchoolID())
            .assessmentCenterSchoolID(assessmentStudentEntity.getAssessmentCenterSchoolID())
            .schoolOfRecordSchoolID(assessmentStudentEntity.getSchoolOfRecordSchoolID())
            .studentID(assessmentStudentEntity.getStudentID())
            .givenName(assessmentStudentEntity.getGivenName())
            .surname(assessmentStudentEntity.getSurname())
            .pen(assessmentStudentEntity.getPen())
            .localID(assessmentStudentEntity.getLocalID())
            .gradeAtRegistration(assessmentStudentEntity.getGradeAtRegistration())
            .localAssessmentID(assessmentStudentEntity.getLocalAssessmentID())
            .proficiencyScore(assessmentStudentEntity.getProficiencyScore())
            .provincialSpecialCaseCode(assessmentStudentEntity.getProvincialSpecialCaseCode())
            .numberOfAttempts(assessmentStudentEntity.getNumberOfAttempts())
            .adaptedAssessmentCode(assessmentStudentEntity.getAdaptedAssessmentCode())
            .irtScore(assessmentStudentEntity.getIrtScore())
            .markingSession(assessmentStudentEntity.getMarkingSession())
            .rawScore(assessmentStudentEntity.getRawScore())
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

  public Student createMockStudentAPIStudent(){
    return Student.builder()
            .studentID(String.valueOf(UUID.randomUUID()))
            .pen("120164447")
            .legalFirstName("TestFirst")
            .legalLastName("TestLast")
            .dob(String.valueOf(LocalDateTime.now().minusYears(16)))
            .sexCode("F")
            .genderCode("F")
            .emailVerified("true")
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

  public AssessmentStudentComponentEntity createMockAssessmentStudentComponentEntity(AssessmentStudentEntity assessmentStudentEntity, UUID assessmentComponentID) {
    return AssessmentStudentComponentEntity.builder()
            .assessmentStudentComponentID(UUID.randomUUID())
            .assessmentStudentEntity(assessmentStudentEntity)
            .assessmentComponentID(assessmentComponentID)
            .choicePath("A")
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now())
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateDate(LocalDateTime.now())
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

  public static LocalDateTime createOctoberFirstDate() {
    int currentMonth = LocalDate.now().getMonthValue();

    if (currentMonth >= Month.SEPTEMBER.getValue()) {
      return LocalDate.of(LocalDate.now().getYear() + 1, Month.OCTOBER, 1).atStartOfDay();
    } else {
      return LocalDate.of(LocalDate.now().getYear(), Month.OCTOBER, 1).atStartOfDay();
    }
  }

  public static LocalDateTime createMayFirstDate() {
    LocalDate now = LocalDate.now();
    int currentMonth = now.getMonthValue();

    if (currentMonth >= Month.SEPTEMBER.getValue()) {
      return LocalDate.of(now.getYear() + 1, Month.MAY, 1).atStartOfDay();
    } else {
      return LocalDate.of(now.getYear(), Month.MAY, 1).atStartOfDay();
    }
  }

  @SneakyThrows
  protected AssessmentSagaEntity createStudentResultMockSaga(final StudentResultSagaData studentResultSagaData) {
    return AssessmentSagaEntity.builder()
            .updateDate(LocalDateTime.now().minusMinutes(15))
            .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
            .createDate(LocalDateTime.now().minusMinutes(15))
            .sagaName(SagaEnum.PROCESS_STUDENT_RESULT.toString())
            .status(SagaStatusEnum.IN_PROGRESS.toString())
            .sagaState(EventType.INITIATED.toString())
            .payload(JsonUtil.getJsonStringFromObject(studentResultSagaData))
            .build();
  }
}
