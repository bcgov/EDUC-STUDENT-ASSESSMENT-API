package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.*;
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

    AssessmentStudentEntity assessmentStudentEntity= createMockStudentEntity(assessmentEntity);
    assessmentStudentEntity.setAssessmentStudentID(null);
    assessmentStudentEntity.setSchoolOfRecordSchoolID(schoolID);
    assessmentStudentEntity.getAssessmentEntity().setAssessmentTypeCode(AssessmentTypeCodes.LTF12.getCode());

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(assessmentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName(assessmentStudentEntity.getGivenName());
    studentAPIStudent.setLegalLastName(assessmentStudentEntity.getSurname());
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(Optional.of(studentAPIStudent));

    var gradStudentRecord = new GradStudentRecord();
    gradStudentRecord.setStudentID(UUID.randomUUID().toString());
    gradStudentRecord.setSchoolOfRecordId(String.valueOf(schoolID));
    gradStudentRecord.setGraduated("Y");
    when(this.restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(gradStudentRecord));

    //when creating an assessment student
    var pair = assessmentStudentService.createStudent(assessmentStudentEntity);
    AssessmentStudent student = pair.getLeft();
    List<AssessmentStudentHistoryEntity> studentHistory = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), UUID.fromString(student.getAssessmentStudentID()));
    //then assessment student is created
    assertNotNull(student);
    assertNotNull(assessmentStudentRepository.findById(UUID.fromString(student.getStudentID())));
    assertThat(studentHistory).hasSize(1);
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
    
    AssessmentStudentEntity studentEntity= createMockStudentEntity(assessmentEntity);
    studentEntity.setSchoolOfRecordSchoolID(schoolID);
    studentEntity.setAssessmentStudentID(null);

    AssessmentStudentEntity assessmentStudentEntity= createMockStudentEntity(assessmentEntity);
    assessmentStudentEntity.setAssessmentStudentID(null);
    assessmentStudentEntity.setSchoolOfRecordSchoolID(schoolID);
    assessmentStudentEntity.getAssessmentEntity().setAssessmentTypeCode(AssessmentTypeCodes.LTF12.getCode());

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(assessmentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName(assessmentStudentEntity.getGivenName());
    studentAPIStudent.setLegalLastName(assessmentStudentEntity.getSurname());
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(Optional.of(studentAPIStudent));

    var gradStudentRecord = new GradStudentRecord();
    gradStudentRecord.setStudentID(UUID.randomUUID().toString());
    gradStudentRecord.setSchoolOfRecordId(String.valueOf(schoolID));
    gradStudentRecord.setGraduated("Y");
    when(this.restUtils.getGradStudentRecordByStudentID(any(), any())).thenReturn(Optional.of(gradStudentRecord));

    var pair = assessmentStudentService.createStudent(studentEntity);
    AssessmentStudent assessmentStudent = pair.getLeft();
    //when updating the student
    var pair2 = assessmentStudentService.updateStudent(mapper.toModel(assessmentStudent));
    var student = pair2.getLeft();
    assertNotNull(student);
    List<AssessmentStudentHistoryEntity> studentHistory = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), UUID.fromString(student.getAssessmentStudentID()));

    //then student is updated and saved
    var updatedStudent = assessmentStudentRepository.findById(UUID.fromString(student.getAssessmentStudentID()));
    assertThat(updatedStudent).isPresent();
    assertThat(updatedStudent.get().getAssessmentEntity().getAssessmentTypeCode()).isEqualTo(AssessmentTypeCodes.LTP10.getCode());
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
    assertThrows(EntityNotFoundException.class, () -> assessmentStudentService.updateStudent(student));
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
    assertThrows(EntityNotFoundException.class, () -> assessmentStudentService.updateStudent(student));
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

    var pair = assessmentStudentService.createStudent(assessmentStudentEntity);
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

    assertThrows(EntityNotFoundException.class, () -> assessmentStudentService.deleteStudent(studentEntity.getAssessmentStudentID()));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudent(studentEntity.getAssessmentStudentID()));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudent(finalStudentEntity.getAssessmentStudentID()));
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
    List<AssessmentEventEntity> events = assessmentStudentService.deleteStudents(Collections.singletonList(studentEntity.getAssessmentStudentID()));
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
    List<AssessmentEventEntity> events = assessmentStudentService.deleteStudents(Arrays.asList(studentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID()));
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
    List<AssessmentEventEntity> events = assessmentStudentService.deleteStudents(Collections.singletonList(studentEntity1.getAssessmentStudentID()));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(Collections.singletonList(studentEntity.getAssessmentStudentID())));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(Arrays.asList(studentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID())));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(Arrays.asList(studentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID())));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(Collections.singletonList(studentEntity.getAssessmentStudentID())));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(Arrays.asList(studentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID())));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(Arrays.asList(studentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID())));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(Collections.singletonList(finalStudentEntity.getAssessmentStudentID())));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(Arrays.asList(finalStudentEntity1.getAssessmentStudentID(), finalStudentEntity2.getAssessmentStudentID())));
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

    assertThrows(InvalidPayloadException.class, () -> assessmentStudentService.deleteStudents(Arrays.asList(finalStudentEntity1.getAssessmentStudentID(), studentEntity2.getAssessmentStudentID())));
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
    List<AssessmentEventEntity> events = assessmentStudentService.deleteStudents(Collections.singletonList(studentEntity2.getAssessmentStudentID()));
    assertThat(events).hasSize(1);
    assertThat(events).noneMatch(e -> e.getEventPayload().equals(expectedEventPayloadBody1));
    assertThat(events).anyMatch(e -> e.getEventPayload().equals(expectedEventPayloadBody2));

    AssessmentStudentEntity fromDatabase1 = assessmentStudentRepository.findById(finalStudentEntity1.getAssessmentStudentID()).orElse(null);
    assertThat(fromDatabase1).isNotNull();

    AssessmentStudentEntity fromDatabase2 = assessmentStudentRepository.findById(studentEntity2.getAssessmentStudentID()).orElse(null);
    assertThat(fromDatabase2).isNull();
  }
}
