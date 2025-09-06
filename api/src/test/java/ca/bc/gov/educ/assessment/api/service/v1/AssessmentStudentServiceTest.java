package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@Slf4j
class AssessmentStudentServiceTest extends BaseAssessmentAPITest {

  private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;

  @Autowired
  AssessmentStudentRepository assessmentStudentRepository;

  @Autowired
  AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;

  @Autowired
  AssessmentSessionRepository assessmentSessionRepository;

  @Autowired
  AssessmentRepository assessmentRepository;

  @Autowired
  RestUtils restUtils;
  
  @Autowired
  StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
  
  @Autowired
  AssessmentStudentService assessmentStudentService;

  @Autowired
  private AssessmentFormRepository assessmentFormRepository;

  @Autowired
  private AssessmentComponentRepository assessmentComponentRepository;

  @Autowired
  private AssessmentQuestionRepository assessmentQuestionRepository;

  @AfterEach
  public void after() {
    stagedAssessmentStudentRepository.deleteAll();
    this.assessmentStudentRepository.deleteAll();
    this.assessmentStudentHistoryRepository.deleteAll();
    this.assessmentRepository.deleteAll();
    this.assessmentSessionRepository.deleteAll();
  }

  @Test
  void testGetStudentByID_WhenStudentExistInDB_ShouldReturnStudent()  {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentStudentEntity assessmentStudentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    //when retrieving the student
    AssessmentStudentEntity student = assessmentStudentService.getStudentByID(assessmentStudentEntity.getAssessmentStudentID());

    //then student is returned
    assertNotNull(student);
  }

  @Test
  void testGetStudentBy_AssessmentIDAndStudentID_WhenStudentExistInDB_ShouldReturnStudent()  {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentStudentEntity assessmentStudentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    //when retrieving the student
    Optional<AssessmentStudentEntity> student = assessmentStudentService.getStudentByAssessmentIDAndStudentID(assessmentEntity.getAssessmentID(), assessmentStudentEntity.getStudentID());

    //then student is returned
    assertThat(student).isPresent();
  }

  @Test
  void testGetStudentByID_WhenStudentDoesNotExistInDB_Return404()  {
    //given student does not exist in database
    //when attempting to retrieve student
    UUID id = UUID.randomUUID();
    //then throw exception
    assertThrows(EntityNotFoundException.class, () -> assessmentStudentService.getStudentByID(id));
  }

  @Test
  void testCreateStudent_WhenStudentDoesNotExistInDB_ShouldReturnStudent() throws JsonProcessingException {
    //given session exists
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP12.getCode()));

    var school = this.createMockSchool();
    UUID schoolID = UUID.randomUUID();
    school.setSchoolId(String.valueOf(schoolID));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    GradStudentRecord gradStudentRecord = this.createMockGradStudentAPIRecord();
    gradStudentRecord.setStudentID(UUID.randomUUID().toString());
    gradStudentRecord.setSchoolOfRecordId(String.valueOf(schoolID));
    gradStudentRecord.setGraduated("Y");
    gradStudentRecord.setStudentGrade("10");
    when(this.restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(gradStudentRecord));

    AssessmentStudentEntity assessmentStudentEntity= createMockStudentEntity(assessmentEntity);
    assessmentStudentEntity.setAssessmentStudentID(null);
    assessmentStudentEntity.setSchoolOfRecordSchoolID(schoolID);
    assessmentStudentEntity.getAssessmentEntity().setAssessmentTypeCode(AssessmentTypeCodes.LTF12.getCode());

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(assessmentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName(assessmentStudentEntity.getGivenName());
    studentAPIStudent.setLegalLastName(assessmentStudentEntity.getSurname());
    studentAPIStudent.setGradeCode("11");
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(Optional.of(studentAPIStudent));

    //when creating an assessment student
    var pair = assessmentStudentService.createStudent(assessmentStudentEntity, false, "UNKNOWN" );
    AssessmentStudent student = pair.getLeft();
    List<AssessmentStudentHistoryEntity> studentHistory = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), UUID.fromString(student.getAssessmentStudentID()));
    //then assessment student is created
    assertNotNull(student);
    assertThat(student.getGradeAtRegistration()).isEqualTo(gradStudentRecord.getStudentGrade());
    assertThat(student.getGradeAtRegistration()).isNotEqualTo(studentAPIStudent.getGradeCode());
    assertNotNull(assessmentStudentRepository.findById(UUID.fromString(student.getStudentID())));
    assertThat(studentHistory).hasSize(1);

    when(this.restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.empty());
    var pair2 = assessmentStudentService.createStudent(assessmentStudentEntity, false, "UNKNOWN");
    AssessmentStudent student2 = pair2.getLeft();
    assertNotNull(student2);
    assertThat(student2.getGradeAtRegistration()).isNotEqualTo(gradStudentRecord.getStudentGrade());
    assertThat(student2.getGradeAtRegistration()).isEqualTo(studentAPIStudent.getGradeCode());
  }

  @Test
  void testUpdateStudent_WhenStudentExistInDB_ShouldReturnUpdatedStudent() throws JsonProcessingException {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    var school = this.createMockSchool();
    UUID schoolID = UUID.randomUUID();
    school.setSchoolId(String.valueOf(schoolID));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    
    AssessmentStudentEntity studentEntity = createMockStudentEntity(assessmentEntity);
    studentEntity.setSchoolOfRecordSchoolID(schoolID);
    studentEntity.setGradeAtRegistration("12");
    studentEntity.setAssessmentStudentID(null);

    AssessmentStudentEntity assessmentStudentEntity = createMockStudentEntity(assessmentEntity);
    assessmentStudentEntity.setAssessmentStudentID(null);
    assessmentStudentEntity.setSchoolOfRecordSchoolID(schoolID);
    assessmentStudentEntity.getAssessmentEntity().setAssessmentTypeCode(AssessmentTypeCodes.LTF12.getCode());

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(assessmentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName(assessmentStudentEntity.getGivenName());
    studentAPIStudent.setLegalLastName(assessmentStudentEntity.getSurname());
    studentAPIStudent.setGradeCode("11");
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(Optional.of(studentAPIStudent));

    var gradStudentRecord = new GradStudentRecord();
    gradStudentRecord.setStudentID(UUID.randomUUID().toString());
    gradStudentRecord.setSchoolOfRecordId(String.valueOf(schoolID));
    gradStudentRecord.setGraduated("Y");
    gradStudentRecord.setStudentGrade("10");
    when(this.restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(gradStudentRecord));

    var pair = assessmentStudentService.createStudent(studentEntity, false, "UNKNOWN");
    AssessmentStudent assessmentStudent = pair.getLeft();
    //when updating the student
    var pair2 = assessmentStudentService.updateStudent(mapper.toModel(assessmentStudent), false, "UNKNOWN");
    var student = pair2.getLeft();
    assertNotNull(student);
    List<AssessmentStudentHistoryEntity> studentHistory = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), UUID.fromString(student.getAssessmentStudentID()));

    //then student is updated and saved
    var updatedStudent = assessmentStudentRepository.findById(UUID.fromString(student.getAssessmentStudentID()));
    assertThat(updatedStudent).isPresent();
    assertThat(updatedStudent.get().getAssessmentEntity().getAssessmentTypeCode()).isEqualTo(AssessmentTypeCodes.LTP10.getCode());
    assertThat(updatedStudent.get().getGradeAtRegistration()).isEqualTo(studentEntity.getGradeAtRegistration()); //gradeAtRegistration should not be updated.
    assertThat(studentHistory).hasSize(2);
  }

  @Test
  void testUpdateStudent_WhenStudentDoesNotExistInDB_ReturnError()  {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    //given student does not exist in database
    //when attempting to update student
    AssessmentStudentEntity student = AssessmentStudentEntity.builder().assessmentStudentID(UUID.randomUUID()).pen("120164447").schoolOfRecordSchoolID(UUID.randomUUID()).studentID(UUID.randomUUID()).assessmentEntity(assessmentEntity).build();

    //then throw exception
    assertThrows(EntityNotFoundException.class, () -> assessmentStudentService.updateStudent(student, false, "UNKNOWN"));
  }

  @Test
  void testUpdateStudent_WhenSessionIDDoesNotExistInDB_ShouldThrowError()  {
    //given student existing in db
    AssessmentSessionEntity assessmentSessionEntity = createMockSessionEntity();
    //assessment does not exist in db
    AssessmentEntity assessmentEntity =createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode());

    //when attempting to update to session id that does not exist
    AssessmentStudentEntity student = createMockStudentEntity(assessmentEntity);

    //then throw exception
    assertThrows(EntityNotFoundException.class, () -> assessmentStudentService.updateStudent(student, false, "UNKNOWN"));
  }

  @Test
  void testCreateStudent_WhenNamesDoNotMatchStudentAPI_ShouldReturnValidationErrors() throws JsonProcessingException {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP12.getCode()));

    var school = this.createMockSchool();
    UUID schoolID = UUID.randomUUID();
    school.setSchoolId(String.valueOf(schoolID));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    AssessmentStudentEntity assessmentStudentEntity= createMockStudentEntity(assessmentEntity);
    assessmentStudentEntity.setSchoolOfRecordSchoolID(schoolID);
    assessmentStudentEntity.setAssessmentStudentID(null);

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(assessmentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName("Bugs");
    studentAPIStudent.setLegalLastName("Bunny");
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(Optional.of(studentAPIStudent));

    var gradStudentRecord = new GradStudentRecord();
    gradStudentRecord.setStudentID(UUID.randomUUID().toString());
    gradStudentRecord.setSchoolOfRecordId(String.valueOf(schoolID));
    gradStudentRecord.setGraduated("Y");
    when(this.restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(gradStudentRecord));

    var pair = assessmentStudentService.createStudent(assessmentStudentEntity, false, "UNKNOWN");
    AssessmentStudent student = pair.getLeft();
    assertThat(student.getAssessmentStudentValidationIssues()).hasSize(2);
  }

  @Test
  void testGetStudentsByAssessmentIDsInAndStudentID_WithNumeracyCodes_ReturnsAllNumeracyRegistrations() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentNME10 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.NME10.getCode()));
    AssessmentEntity assessmentNMF10 = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.NMF10.getCode()));

    AssessmentStudentEntity studentEntity1 = createMockStudentEntity(assessmentNME10);
    AssessmentStudentEntity studentEntity2 = createMockStudentEntity(assessmentNMF10);
    studentEntity2.setStudentID(studentEntity1.getStudentID());

    assessmentStudentRepository.save(studentEntity1);
    assessmentStudentRepository.save(studentEntity2);

    List<UUID> numeracyAssessmentIDs = List.of(assessmentNME10.getAssessmentID(), assessmentNMF10.getAssessmentID());
    List<AssessmentStudentEntity> found = assessmentStudentService.getStudentsByAssessmentIDsInAndStudentID(numeracyAssessmentIDs, studentEntity1.getStudentID());

    assertThat(found).hasSize(2);
    assertThat(found.stream().map(AssessmentStudentEntity::getAssessmentEntity).map(AssessmentEntity::getAssessmentTypeCode).anyMatch(code -> code.equals(AssessmentTypeCodes.NME10.getCode()))).isTrue();
    assertThat(found.stream().map(AssessmentStudentEntity::getAssessmentEntity).map(AssessmentEntity::getAssessmentTypeCode).anyMatch(code -> code.equals(AssessmentTypeCodes.NMF10.getCode()))).isTrue();
  }

  @Test
  void testDeleteStudent_WhenStudentExistInDB_ShouldReturnAssessmentEventEntity() throws JsonProcessingException {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity));

    List<AssessmentStudentHistoryEntity> studentHistory = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity.getAssessmentStudentID());
    assertThat(studentHistory).hasSize(2);

    AssessmentEventEntity event = assessmentStudentService.deleteStudent(studentEntity.getAssessmentStudentID());
    assertThat(event).isNotNull();
    assertThat(event.getEventPayload()).isEqualTo(JsonUtil.getJsonStringFromObject(studentEntity.getStudentID()));

    AssessmentStudentEntity fromDatabase = assessmentStudentRepository.findById(studentEntity.getAssessmentStudentID()).orElse(null);
    assertThat(fromDatabase).isNull();
    studentHistory = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity.getAssessmentStudentID());
    assertThat(studentHistory).isEmpty();
  }

  @Test
  void testDeleteStudent_WhenStudentDoesNotExistInDB_ShouldThrowEntityNotFoundException() {
    AssessmentSessionEntity assessmentSessionEntity = createMockSessionEntity();
    AssessmentEntity assessmentEntity = createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode());
    AssessmentStudentEntity studentEntity = createMockStudentEntity(assessmentEntity);

    AssessmentStudentEntity existsInDatabase = assessmentStudentRepository.findById(studentEntity.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase).isNull();

    UUID studentId = studentEntity.getAssessmentStudentID();
    assertThrows(EntityNotFoundException.class, () -> assessmentStudentService.deleteStudent(studentId));
  }

  @Test
  void testDeleteStudent_WhenAssessmentSessionClosed_ShouldThrowInvalidPayloadException() {
    //given student exists in db
    LocalDateTime currentDate = LocalDateTime.now();
    AssessmentSessionEntity assessmentSessionEntity = createMockSessionEntity();
    assessmentSessionEntity.setActiveFromDate(currentDate.minusMonths(2));
    assessmentSessionEntity.setActiveUntilDate(currentDate.minusMonths(1));
    assessmentSessionEntity = assessmentSessionRepository.save(assessmentSessionEntity);
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    AssessmentStudentEntity existsInDatabase = assessmentStudentRepository.findById(studentEntity.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase).isNotNull();

    UUID studentId = studentEntity.getAssessmentStudentID();
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudent(studentId));
  }

  @Test
  void testDeleteStudent_WhenProvincialSpecialCaseCodeSpecified_ShouldThrowInvalidPayloadException() {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity = createMockStudentEntity(assessmentEntity);
    studentEntity.setProvincialSpecialCaseCode("A");
    AssessmentStudentEntity finalStudentEntity = assessmentStudentRepository.save(studentEntity);

    AssessmentStudentEntity existsInDatabase = assessmentStudentRepository.findById(finalStudentEntity.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase).isNotNull();

    UUID studentId = finalStudentEntity.getAssessmentStudentID();
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudent(studentId));
  }

  @Test
  void testDeleteStudents_WithSingleStudent_WhenStudentExistsInDB_ShouldReturnListOfAssessmentEventEntity() throws JsonProcessingException {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity));

    List<AssessmentStudentHistoryEntity> studentHistory = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity.getAssessmentStudentID());
    assertThat(studentHistory).hasSize(2);

    String expectedEventPayloadBody = JsonUtil.getJsonStringFromObject(studentEntity.getStudentID());
    List<AssessmentEventEntity> events = assessmentStudentService.deleteStudents(Collections.singletonList(studentEntity.getAssessmentStudentID()), false);
    assertThat(events).hasSize(1);
    assertThat(events).anyMatch(e -> e.getEventPayload().equals(expectedEventPayloadBody));

    AssessmentStudentEntity fromDatabase = assessmentStudentRepository.findById(studentEntity.getAssessmentStudentID()).orElse(null);
    assertThat(fromDatabase).isNull();
    studentHistory = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity.getAssessmentStudentID());
    assertThat(studentHistory).isEmpty();
  }

  @Test
  void testDeleteStudents_WithMultipleStudents_WhenStudentsExistInDB_ShouldReturnListOfAssessmentEventEntity() throws JsonProcessingException {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity1 = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity1));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity1));
    AssessmentStudentEntity studentEntity2 = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity2));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity2));

    List<AssessmentStudentHistoryEntity> studentHistory1 = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity1.getAssessmentStudentID());
    assertThat(studentHistory1).hasSize(2);
    List<AssessmentStudentHistoryEntity> studentHistory2 = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity2.getAssessmentStudentID());
    assertThat(studentHistory2).hasSize(2);

    String expectedEventPayloadBody1 = JsonUtil.getJsonStringFromObject(studentEntity1.getStudentID());
    String expectedEventPayloadBody2 = JsonUtil.getJsonStringFromObject(studentEntity2.getStudentID());
    List<AssessmentEventEntity> events = assessmentStudentService.deleteStudents(Arrays.asList(studentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID()), false);
    assertThat(events).hasSize(2);
    assertThat(events).anyMatch(e -> e.getEventPayload().equals(expectedEventPayloadBody1));
    assertThat(events).anyMatch(e -> e.getEventPayload().equals(expectedEventPayloadBody2));

    AssessmentStudentEntity fromDatabase1 = assessmentStudentRepository.findById(studentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(fromDatabase1).isNull();
    studentHistory1 = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity1.getAssessmentStudentID());
    assertThat(studentHistory1).isEmpty();

    AssessmentStudentEntity fromDatabase2 = assessmentStudentRepository.findById(studentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(fromDatabase2).isNull();
    studentHistory2 = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity2.getAssessmentStudentID());
    assertThat(studentHistory2).isEmpty();
  }

  @Test
  void testDeleteStudents_WithMultipleStudents_WhenStudentExistsInDB_ShouldReturnListOfAssessmentEventEntity() throws JsonProcessingException {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity1 = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity1));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity1));
    AssessmentStudentEntity studentEntity2 = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity2));
    assessmentStudentHistoryRepository.save(createMockStudentHistoryEntity(studentEntity2));

    List<AssessmentStudentHistoryEntity> studentHistory1 = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity1.getAssessmentStudentID());
    assertThat(studentHistory1).hasSize(2);
    List<AssessmentStudentHistoryEntity> studentHistory2 = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity2.getAssessmentStudentID());
    assertThat(studentHistory2).hasSize(2);

    String expectedEventPayloadBody1 = JsonUtil.getJsonStringFromObject(studentEntity1.getStudentID());
    String expectedEventPayloadBody2 = JsonUtil.getJsonStringFromObject(studentEntity2.getStudentID());
    List<AssessmentEventEntity> events = assessmentStudentService.deleteStudents(Collections.singletonList(studentEntity1.getAssessmentStudentID()), false);
    assertThat(events).hasSize(1);
    assertThat(events).anyMatch(e -> e.getEventPayload().equals(expectedEventPayloadBody1));
    assertThat(events).noneMatch(e -> e.getEventPayload().equals(expectedEventPayloadBody2));

    AssessmentStudentEntity fromDatabase1 = assessmentStudentRepository.findById(studentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(fromDatabase1).isNull();
    studentHistory1 = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity1.getAssessmentStudentID());
    assertThat(studentHistory1).isEmpty();

    AssessmentStudentEntity fromDatabase2 = assessmentStudentRepository.findById(studentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(fromDatabase2).isNotNull();
    studentHistory2 = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), studentEntity2.getAssessmentStudentID());
    assertThat(studentHistory2).hasSize(2);
  }

  @Test
  void testDeleteStudents_WithSingleStudent_WhenStudentDoesNotExistInDB_ShouldThrowInvalidPayloadException() {
    AssessmentSessionEntity assessmentSessionEntity = createMockSessionEntity();
    AssessmentEntity assessmentEntity = createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode());
    AssessmentStudentEntity studentEntity = createMockStudentEntity(assessmentEntity);

    AssessmentStudentEntity existsInDatabase = assessmentStudentRepository.findById(studentEntity.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase).isNull();

    List<UUID> studentIds = Collections.singletonList(studentEntity.getAssessmentStudentID());
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(studentIds, false));
  }

  @Test
  void testDeleteStudents_WithMultipleStudents_WhenStudentsDoNotExistInDB_ShouldThrowInvalidPayloadException() {
    AssessmentSessionEntity assessmentSessionEntity = createMockSessionEntity();
    AssessmentEntity assessmentEntity = createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode());
    AssessmentStudentEntity studentEntity1 = createMockStudentEntity(assessmentEntity);
    AssessmentStudentEntity studentEntity2 = createMockStudentEntity(assessmentEntity);

    AssessmentStudentEntity existsInDatabase1 = assessmentStudentRepository.findById(studentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase1).isNull();
    AssessmentStudentEntity existsInDatabase2 = assessmentStudentRepository.findById(studentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase2).isNull();

    List<UUID> studentIds = Arrays.asList(studentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID());
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(studentIds, false));
  }

  @Test
  void testDeleteStudents_WithMultipleStudents_WhenOneStudentDoesNotExistInDB_ShouldThrowInvalidPayloadException() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity1 = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));
    AssessmentStudentEntity studentEntity2 = createMockStudentEntity(assessmentEntity);

    AssessmentStudentEntity existsInDatabase1 = assessmentStudentRepository.findById(studentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase1).isNotNull();
    AssessmentStudentEntity existsInDatabase2 = assessmentStudentRepository.findById(studentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase2).isNull();

    List<UUID> studentIds = Arrays.asList(studentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID());
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(studentIds, false));
  }

  @Test
  void testDeleteStudents_WithSingleStudent_WhenAssessmentSessionClosed_ShouldThrowInvalidPayloadException() {
    //given student exists in db
    LocalDateTime currentDate = LocalDateTime.now();
    AssessmentSessionEntity assessmentSessionEntity = createMockSessionEntity();
    assessmentSessionEntity.setActiveFromDate(currentDate.minusMonths(2));
    assessmentSessionEntity.setActiveUntilDate(currentDate.minusMonths(1));
    assessmentSessionEntity = assessmentSessionRepository.save(assessmentSessionEntity);
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    AssessmentStudentEntity existsInDatabase = assessmentStudentRepository.findById(studentEntity.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase).isNotNull();

    List<UUID> studentIds = Collections.singletonList(studentEntity.getAssessmentStudentID());
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(studentIds, false));
  }

  @Test
  void testDeleteStudents_WithMultipleStudents_WhenAssessmentSessionClosed_ShouldThrowInvalidPayloadException() {
    //given student exists in db
    LocalDateTime currentDate = LocalDateTime.now();
    AssessmentSessionEntity assessmentSessionEntity = createMockSessionEntity();
    assessmentSessionEntity.setActiveFromDate(currentDate.minusMonths(2));
    assessmentSessionEntity.setActiveUntilDate(currentDate.minusMonths(1));
    assessmentSessionEntity = assessmentSessionRepository.save(assessmentSessionEntity);
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity1 = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));
    AssessmentStudentEntity studentEntity2 = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    AssessmentStudentEntity existsInDatabase1 = assessmentStudentRepository.findById(studentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase1).isNotNull();
    AssessmentStudentEntity existsInDatabase2 = assessmentStudentRepository.findById(studentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase2).isNotNull();

    List<UUID> studentIds = Arrays.asList(studentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID());
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(studentIds, false));
  }

  @Test
  void testDeleteStudents_WithMultipleStudents_WhenOneAssessmentSessionClosedAndOtherOpened_ShouldThrowInvalidPayloadException() {
    //given student exists in db
    AssessmentSessionEntity openedAssessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    LocalDateTime currentDate = LocalDateTime.now();
    AssessmentSessionEntity closedAssessmentSessionEntity = createMockSessionEntity();
    closedAssessmentSessionEntity.setActiveFromDate(currentDate.minusMonths(2));
    closedAssessmentSessionEntity.setActiveUntilDate(currentDate.minusMonths(1));
    closedAssessmentSessionEntity = assessmentSessionRepository.save(closedAssessmentSessionEntity);
    AssessmentEntity openedAssessmentEntity = assessmentRepository.save(createMockAssessmentEntity(openedAssessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentEntity closedAssessmentEntity = assessmentRepository.save(createMockAssessmentEntity(closedAssessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity1 = assessmentStudentRepository.save(createMockStudentEntity(openedAssessmentEntity));
    AssessmentStudentEntity studentEntity2 = assessmentStudentRepository.save(createMockStudentEntity(closedAssessmentEntity));

    AssessmentStudentEntity existsInDatabase1 = assessmentStudentRepository.findById(studentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase1).isNotNull();
    AssessmentStudentEntity existsInDatabase2 = assessmentStudentRepository.findById(studentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase2).isNotNull();

    List<UUID> studentIds = Arrays.asList(studentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID());
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(studentIds, false));
  }

  @Test
  void testDeleteStudents_WithSingleStudent_WhenProvincialSpecialCaseCodeSpecified_ShouldThrowInvalidPayloadException() {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity = createMockStudentEntity(assessmentEntity);
    studentEntity.setProvincialSpecialCaseCode("A");
    AssessmentStudentEntity finalStudentEntity = assessmentStudentRepository.save(studentEntity);

    AssessmentStudentEntity existsInDatabase = assessmentStudentRepository.findById(finalStudentEntity.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase).isNotNull();

    List<UUID> studentIds = Collections.singletonList(finalStudentEntity.getAssessmentStudentID());
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(studentIds, false));
  }

  @Test
  void testDeleteStudents_WithMultipleStudent_WhenProvincialSpecialCaseCodeSpecified_ShouldThrowInvalidPayloadException() {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity1 = createMockStudentEntity(assessmentEntity);
    studentEntity1.setProvincialSpecialCaseCode("A");
    AssessmentStudentEntity finalStudentEntity1 = assessmentStudentRepository.save(studentEntity1);
    AssessmentStudentEntity studentEntity2 = createMockStudentEntity(assessmentEntity);
    studentEntity2.setProvincialSpecialCaseCode("A");
    AssessmentStudentEntity finalStudentEntity2 = assessmentStudentRepository.save(studentEntity2);

    AssessmentStudentEntity existsInDatabase1 = assessmentStudentRepository.findById(finalStudentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase1).isNotNull();
    AssessmentStudentEntity existsInDatabase2 = assessmentStudentRepository.findById(finalStudentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase2).isNotNull();

    List<UUID> studentIds = Arrays.asList(finalStudentEntity1.getAssessmentStudentID(), finalStudentEntity2.getAssessmentStudentID());
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(studentIds, false));
  }

  @Test
  void testDeleteStudents_WithMultipleStudent_OneStudentWithProvincialSpecialCaseCodeSpecified_ShouldThrowInvalidPayloadException() {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity1 = createMockStudentEntity(assessmentEntity);
    studentEntity1.setProvincialSpecialCaseCode("A");
    AssessmentStudentEntity finalStudentEntity1 = assessmentStudentRepository.save(studentEntity1);
    AssessmentStudentEntity studentEntity2 =  assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    AssessmentStudentEntity existsInDatabase1 = assessmentStudentRepository.findById(finalStudentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase1).isNotNull();
    AssessmentStudentEntity existsInDatabase2 = assessmentStudentRepository.findById(studentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase2).isNotNull();

    List<UUID> studentIds = Arrays.asList(finalStudentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID());
    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(studentIds, false));
  }

  @Test
  void testDeleteStudents_WithMultipleStudent_OneStudentWithProvincialSpecialCaseCodeSpecified_ShouldReturnListOfAssessmentEventEntity() throws JsonProcessingException {
    //given student exists in db
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity studentEntity1 = createMockStudentEntity(assessmentEntity);
    studentEntity1.setProvincialSpecialCaseCode("A");
    AssessmentStudentEntity finalStudentEntity1 = assessmentStudentRepository.save(studentEntity1);
    AssessmentStudentEntity studentEntity2 =  assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    AssessmentStudentEntity existsInDatabase1 = assessmentStudentRepository.findById(finalStudentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase1).isNotNull();
    AssessmentStudentEntity existsInDatabase2 = assessmentStudentRepository.findById(studentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(existsInDatabase2).isNotNull();

    String expectedEventPayloadBody1 = JsonUtil.getJsonStringFromObject(finalStudentEntity1.getStudentID());
    String expectedEventPayloadBody2 = JsonUtil.getJsonStringFromObject(studentEntity2.getStudentID());
    List<AssessmentEventEntity> events = assessmentStudentService.deleteStudents(Collections.singletonList(studentEntity2.getAssessmentStudentID()), false);
    assertThat(events).hasSize(1);
    assertThat(events).noneMatch(e -> e.getEventPayload().equals(expectedEventPayloadBody1));
    assertThat(events).anyMatch(e -> e.getEventPayload().equals(expectedEventPayloadBody2));

    AssessmentStudentEntity fromDatabase1 = assessmentStudentRepository.findById(finalStudentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(fromDatabase1).isNotNull();

    AssessmentStudentEntity fromDatabase2 = assessmentStudentRepository.findById(studentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(fromDatabase2).isNull();
  }

  @Test
  void testUpdateStudent_WhenAssessmentIDsMatch_ShouldUseCurrentAssessmentEntity() throws JsonProcessingException {
    //given student exists in db with current assessment
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity currentAssessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    var school = this.createMockSchool();
    UUID schoolID = UUID.randomUUID();
    school.setSchoolId(String.valueOf(schoolID));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    AssessmentStudentEntity currentStudentEntity = createMockStudentEntity(currentAssessmentEntity);
    currentStudentEntity.setSchoolOfRecordSchoolID(schoolID);
    currentStudentEntity.setAssessmentStudentID(null);

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(currentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName(currentStudentEntity.getGivenName());
    studentAPIStudent.setLegalLastName(currentStudentEntity.getSurname());
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(Optional.of(studentAPIStudent));

    var gradStudentRecord = new GradStudentRecord();
    gradStudentRecord.setStudentID(UUID.randomUUID().toString());
    gradStudentRecord.setSchoolOfRecordId(String.valueOf(schoolID));
    gradStudentRecord.setGraduated("Y");
    when(this.restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(gradStudentRecord));

    var pair = assessmentStudentService.createStudent(currentStudentEntity, false, "UNKNOWN");
    AssessmentStudent createdStudent = pair.getLeft();

    //when updating student with same assessment ID
    AssessmentStudentEntity updateStudentEntity = mapper.toModel(createdStudent);
    updateStudentEntity.getAssessmentEntity().setAssessmentID(currentAssessmentEntity.getAssessmentID());

    var updatePair = assessmentStudentService.updateStudent(updateStudentEntity, false, "UNKNOWN");
    AssessmentStudent updatedStudent = updatePair.getLeft();

    //then current assessment entity should be used
    assertNotNull(updatedStudent);
    var savedStudent = assessmentStudentRepository.findById(UUID.fromString(updatedStudent.getAssessmentStudentID()));
    assertThat(savedStudent).isPresent();
    assertThat(savedStudent.get().getAssessmentEntity().getAssessmentID()).isEqualTo(currentAssessmentEntity.getAssessmentID());
  }

  @Test
  void testUpdateStudent_WhenAssessmentIDsDifferAndAssessmentDNE_ShouldThrowException() throws JsonProcessingException {
    //given student exists in db with current assessment
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity currentAssessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    var school = this.createMockSchool();
    UUID schoolID = UUID.randomUUID();
    school.setSchoolId(String.valueOf(schoolID));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    AssessmentStudentEntity currentStudentEntity = createMockStudentEntity(currentAssessmentEntity);
    currentStudentEntity.setSchoolOfRecordSchoolID(schoolID);
    currentStudentEntity.setAssessmentStudentID(null);

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(currentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName(currentStudentEntity.getGivenName());
    studentAPIStudent.setLegalLastName(currentStudentEntity.getSurname());
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(Optional.of(studentAPIStudent));

    var gradStudentRecord = new GradStudentRecord();
    gradStudentRecord.setStudentID(UUID.randomUUID().toString());
    gradStudentRecord.setSchoolOfRecordId(String.valueOf(schoolID));
    gradStudentRecord.setGraduated("Y");
    when(this.restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(gradStudentRecord));

    var pair = assessmentStudentService.createStudent(currentStudentEntity, false, "UNKNOWN");
    AssessmentStudent createdStudent = pair.getLeft();

    //when updating student with different assessment ID
    AssessmentStudentEntity updateStudentEntity = mapper.toModel(createdStudent);
    UUID newAssessmentId = UUID.randomUUID();
    updateStudentEntity.getAssessmentEntity().setAssessmentID(newAssessmentId);

    //then should throw exception for non-existent assessment
    assertThrows(EntityNotFoundException.class, () -> assessmentStudentService.updateStudent(updateStudentEntity, false, "UNKNOWN"));
  }

  @Test
  void testUpdateStudent_WhenNewAssessmentExists_ShouldUseNewAssessmentEntity() throws JsonProcessingException {
    //given student exists in db with current assessment
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity currentAssessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    //and new assessment exists
    AssessmentEntity newAssessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTF12.getCode()));

    var school = this.createMockSchool();
    UUID schoolID = UUID.randomUUID();
    school.setSchoolId(String.valueOf(schoolID));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    AssessmentStudentEntity currentStudentEntity = createMockStudentEntity(currentAssessmentEntity);
    currentStudentEntity.setSchoolOfRecordSchoolID(schoolID);
    currentStudentEntity.setAssessmentStudentID(null);

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(currentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName(currentStudentEntity.getGivenName());
    studentAPIStudent.setLegalLastName(currentStudentEntity.getSurname());
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(Optional.of(studentAPIStudent));

    var gradStudentRecord = new GradStudentRecord();
    gradStudentRecord.setStudentID(UUID.randomUUID().toString());
    gradStudentRecord.setSchoolOfRecordId(String.valueOf(schoolID));
    gradStudentRecord.setGraduated("Y");
    when(this.restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(gradStudentRecord));

    var pair = assessmentStudentService.createStudent(currentStudentEntity, false, "UNKNOWN");
    AssessmentStudent createdStudent = pair.getLeft();

    //when updating student with new assessment ID
    AssessmentStudentEntity updateStudentEntity = mapper.toModel(createdStudent);
    updateStudentEntity.getAssessmentEntity().setAssessmentID(newAssessmentEntity.getAssessmentID());

    var updatePair = assessmentStudentService.updateStudent(updateStudentEntity, false, "UNKNOWN");
    AssessmentStudent updatedStudent = updatePair.getLeft();

    //then new assessment entity should be used
    assertNotNull(updatedStudent);
    var savedStudent = assessmentStudentRepository.findById(UUID.fromString(updatedStudent.getAssessmentStudentID()));
    assertThat(savedStudent).isPresent();
    assertThat(savedStudent.get().getAssessmentEntity().getAssessmentID()).isEqualTo(newAssessmentEntity.getAssessmentID());
    assertThat(savedStudent.get().getAssessmentEntity().getAssessmentTypeCode()).isEqualTo(newAssessmentEntity.getAssessmentTypeCode());
  }

  @Test
  void testMarkStagedStudentsReadyForTransfer_WhenStagedStudentsExist_ShouldMarkThemForTransfer() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    StagedAssessmentStudentEntity stagedStudent1 = createMockStagedStudentEntity(assessmentEntity);
    stagedStudent1.setStagedAssessmentStudentStatus("PENMATCHED");
    StagedAssessmentStudentEntity stagedStudent2 = createMockStagedStudentEntity(assessmentEntity);
    stagedStudent2.setStagedAssessmentStudentStatus("PENMATCHED");

    stagedAssessmentStudentRepository.save(stagedStudent1);
    stagedAssessmentStudentRepository.save(stagedStudent2);

    int updatedCount = assessmentStudentService.markStagedStudentsReadyForTransfer();

    assertThat(updatedCount).isEqualTo(2);

    List<StagedAssessmentStudentEntity> transferStudents = stagedAssessmentStudentRepository.findAll();
    assertThat(transferStudents).hasSize(2);
    assertThat(transferStudents).allMatch(student -> "TRANSFER".equals(student.getStagedAssessmentStudentStatus()));
    assertThat(transferStudents).allMatch(student -> ApplicationProperties.STUDENT_ASSESSMENT_API.equals(student.getUpdateUser()));
  }

  @Test
  void testMarkStagedStudentsReadyForTransfer_WhenNoStagedStudentsExist_ShouldReturnZero() {
    int updatedCount = assessmentStudentService.markStagedStudentsReadyForTransfer();
    assertThat(updatedCount).isZero();
  }

  @Test
  void testFindBatchOfTransferStudentIds_WhenTransferStudentsExist_ShouldReturnBatch() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    StagedAssessmentStudentEntity transferStudent1 = createMockStagedStudentEntity(assessmentEntity);
    transferStudent1.setStagedAssessmentStudentStatus("TRANSFER");
    StagedAssessmentStudentEntity transferStudent2 = createMockStagedStudentEntity(assessmentEntity);
    transferStudent2.setStagedAssessmentStudentStatus("TRANSFER");
    StagedAssessmentStudentEntity nonTransferStudent = createMockStagedStudentEntity(assessmentEntity);
    nonTransferStudent.setStagedAssessmentStudentStatus("MERGED");

    StagedAssessmentStudentEntity saved1 = stagedAssessmentStudentRepository.save(transferStudent1);
    StagedAssessmentStudentEntity saved2 = stagedAssessmentStudentRepository.save(transferStudent2);
    stagedAssessmentStudentRepository.save(nonTransferStudent);

    List<StagedAssessmentStudentEntity> transferStudents = assessmentStudentService.findBatchOfTransferStudentIds(10);

    assertThat(transferStudents).hasSize(2);
    var transferStudentIds = transferStudents.stream().map(StagedAssessmentStudentEntity::getAssessmentStudentID).toList();
    assertThat(transferStudentIds).containsExactlyInAnyOrder(saved1.getAssessmentStudentID(), saved2.getAssessmentStudentID());
  }

  @Test
  void testFindBatchOfTransferStudentIds_WhenBatchSizeIsSmaller_ShouldRespectBatchSize() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    for (int i = 0; i < 5; i++) {
      StagedAssessmentStudentEntity transferStudent = createMockStagedStudentEntity(assessmentEntity);
      transferStudent.setStagedAssessmentStudentStatus("TRANSFER");
      stagedAssessmentStudentRepository.save(transferStudent);
    }

    List<StagedAssessmentStudentEntity> transferStudentIds = assessmentStudentService.findBatchOfTransferStudentIds(3);

    assertThat(transferStudentIds).hasSize(3);
  }

  @Test
  void testFindBatchOfTransferStudentIds_WhenNoTransferStudentsExist_ShouldReturnEmptyList() {
    List<StagedAssessmentStudentEntity> transferStudentIds = assessmentStudentService.findBatchOfTransferStudentIds(10);
    assertThat(transferStudentIds).isEmpty();
  }

  @Test
  void testMarkStudentAsTransferInProgress_WhenStudentExistsWithTransferStatus_ShouldUpdateToTransferIn() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    StagedAssessmentStudentEntity transferStudent = createMockStagedStudentEntity(assessmentEntity);
    transferStudent.setStagedAssessmentStudentStatus("TRANSFER");
    StagedAssessmentStudentEntity savedStudent = stagedAssessmentStudentRepository.save(transferStudent);

    int updatedCount = assessmentStudentService.markStudentAsTransferInProgress(savedStudent.getAssessmentStudentID());

    assertThat(updatedCount).isEqualTo(1);

    StagedAssessmentStudentEntity updatedStudent = stagedAssessmentStudentRepository.findById(savedStudent.getAssessmentStudentID()).orElse(null);
    assertThat(updatedStudent).isNotNull();
    assertThat(updatedStudent.getStagedAssessmentStudentStatus()).isEqualTo("TRANSFERED");
    assertThat(updatedStudent.getUpdateUser()).isEqualTo("ASSESSMENT-API");
  }

  @Test
  void testMarkStudentAsTransferInProgress_WhenStudentDoesNotExist_ShouldReturnZero() {
    UUID nonExistentStudentId = UUID.randomUUID();
    int updatedCount = assessmentStudentService.markStudentAsTransferInProgress(nonExistentStudentId);
    assertThat(updatedCount).isZero();
  }

  @Test
  void testMarkStudentAsTransferInProgress_WhenStudentHasDifferentStatus_ShouldReturnZero() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    StagedAssessmentStudentEntity loadedStudent = createMockStagedStudentEntity(assessmentEntity);
    loadedStudent.setStagedAssessmentStudentStatus("LOADED");
    StagedAssessmentStudentEntity savedStudent = stagedAssessmentStudentRepository.save(loadedStudent);

    int updatedCount = assessmentStudentService.markStudentAsTransferInProgress(savedStudent.getAssessmentStudentID());

    assertThat(updatedCount).isZero();

    StagedAssessmentStudentEntity unchangedStudent = stagedAssessmentStudentRepository.findById(savedStudent.getAssessmentStudentID()).orElse(null);
    assertThat(unchangedStudent).isNotNull();
    assertThat(unchangedStudent.getStagedAssessmentStudentStatus()).isEqualTo("LOADED");
  }

  @Test
  void testGetStudentWithAssessmentDetailsByID_WhenStudentAndAssessmentExist_ShouldReturnStudentWithFullAssessmentDetails() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    
    AssessmentFormEntity formEntity = createMockAssessmentFormEntity(assessmentEntity, "A");
    AssessmentFormEntity savedForm = assessmentFormRepository.save(formEntity);
    
    AssessmentComponentEntity componentEntity = createMockAssessmentComponentEntity(savedForm, "MUL_CHOICE", "NONE");
    AssessmentComponentEntity savedComponent = assessmentComponentRepository.save(componentEntity);
    
    AssessmentQuestionEntity question1 = createMockAssessmentQuestionEntity(savedComponent, 1, 1);
    AssessmentQuestionEntity question2 = createMockAssessmentQuestionEntity(savedComponent, 2, 2);
    assessmentQuestionRepository.save(question1);
    assessmentQuestionRepository.save(question2);

    AssessmentStudentEntity assessmentStudentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    AssessmentStudentEntity student = assessmentStudentService.getStudentWithAssessmentDetailsByID(
        assessmentStudentEntity.getAssessmentStudentID(), 
        assessmentEntity.getAssessmentID()
    );

    assertNotNull(student);
    assertThat(student.getAssessmentStudentID()).isEqualTo(assessmentStudentEntity.getAssessmentStudentID());
    
    assertNotNull(student.getAssessmentEntity());
    assertThat(student.getAssessmentEntity().getAssessmentID()).isEqualTo(assessmentEntity.getAssessmentID());
    
    assertNotNull(student.getAssessmentEntity().getAssessmentSessionEntity());
    assertThat(student.getAssessmentEntity().getAssessmentSessionEntity().getSessionID())
        .isEqualTo(assessmentSessionEntity.getSessionID());
    
    assertThat(student.getAssessmentEntity().getAssessmentForms()).hasSize(1);
    AssessmentFormEntity loadedForm = student.getAssessmentEntity().getAssessmentForms().iterator().next();
    assertThat(loadedForm.getAssessmentFormID()).isEqualTo(savedForm.getAssessmentFormID());
    
    assertThat(loadedForm.getAssessmentComponentEntities()).hasSize(1);
    AssessmentComponentEntity loadedComponent = loadedForm.getAssessmentComponentEntities().iterator().next();
    assertThat(loadedComponent.getAssessmentComponentID()).isEqualTo(savedComponent.getAssessmentComponentID());
    
    assertThat(loadedComponent.getAssessmentQuestionEntities()).hasSize(2);
    List<Integer> questionNumbers = loadedComponent.getAssessmentQuestionEntities().stream()
        .map(AssessmentQuestionEntity::getQuestionNumber)
        .sorted()
        .toList();
    assertThat(questionNumbers).containsExactly(1, 2);
  }

  @Test
  void testGetStudentWithAssessmentDetailsByID_WhenStudentExistsButAssessmentIDDoesNotMatch_ShouldThrowException() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));
    AssessmentStudentEntity assessmentStudentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    UUID differentAssessmentID = UUID.randomUUID();

    assertThrows(EntityNotFoundException.class, () ->
        assessmentStudentService.getStudentWithAssessmentDetailsByID(
            assessmentStudentEntity.getAssessmentStudentID(),
            differentAssessmentID
        )
    );
  }

  @Test
  void testGetStudentWithAssessmentDetailsByID_WhenStudentDoesNotExist_ShouldThrowException() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    UUID nonExistentStudentID = UUID.randomUUID();

    assertThrows(EntityNotFoundException.class, () ->
        assessmentStudentService.getStudentWithAssessmentDetailsByID(
            nonExistentStudentID,
            assessmentEntity.getAssessmentID()
        )
    );
  }

  @Test
  void testGetStudentWithAssessmentDetailsByID_WhenAssessmentHasMultipleForms_ShouldLoadAllFormsWithComponentsAndQuestions() {
    AssessmentSessionEntity assessmentSessionEntity = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(assessmentSessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentFormEntity form1 = createMockAssessmentFormEntity(assessmentEntity, "A");
    AssessmentFormEntity savedForm1 = assessmentFormRepository.save(form1);

    AssessmentFormEntity form2 = createMockAssessmentFormEntity(assessmentEntity, "B");
    AssessmentFormEntity savedForm2 = assessmentFormRepository.save(form2);

    AssessmentComponentEntity component1 = createMockAssessmentComponentEntity(savedForm1, "MUL_CHOICE", "NONE");
    AssessmentComponentEntity savedComponent1 = assessmentComponentRepository.save(component1);

    AssessmentComponentEntity component2 = createMockAssessmentComponentEntity(savedForm2, "OPEN_ENDED", "NONE");
    AssessmentComponentEntity savedComponent2 = assessmentComponentRepository.save(component2);

    AssessmentQuestionEntity question1 = createMockAssessmentQuestionEntity(savedComponent1, 1, 1);
    AssessmentQuestionEntity question2 = createMockAssessmentQuestionEntity(savedComponent2, 2, 2);
    assessmentQuestionRepository.save(question1);
    assessmentQuestionRepository.save(question2);

    AssessmentStudentEntity assessmentStudentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    AssessmentStudentEntity student = assessmentStudentService.getStudentWithAssessmentDetailsByID(
        assessmentStudentEntity.getAssessmentStudentID(),
        assessmentEntity.getAssessmentID()
    );

    assertNotNull(student);
    assertThat(student.getAssessmentEntity().getAssessmentForms()).hasSize(2);

    List<String> formCodes = student.getAssessmentEntity().getAssessmentForms().stream()
        .map(AssessmentFormEntity::getFormCode)
        .sorted()
        .toList();
    assertThat(formCodes).containsExactly("A", "B");

    for (AssessmentFormEntity form : student.getAssessmentEntity().getAssessmentForms()) {
      assertThat(form.getAssessmentComponentEntities()).hasSize(1);
      for (AssessmentComponentEntity component : form.getAssessmentComponentEntities()) {
        assertThat(component.getAssessmentQuestionEntities()).hasSize(1);
      }
    }
  }
}
