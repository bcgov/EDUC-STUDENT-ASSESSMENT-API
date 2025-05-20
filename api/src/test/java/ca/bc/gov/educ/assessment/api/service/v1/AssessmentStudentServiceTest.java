package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistoryEntity;
import ca.bc.gov.educ.assessment.api.model.v1.SessionEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentHistoryRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.SessionRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
  AssessmentStudentService service;

  @Autowired
  AssessmentStudentRepository assessmentStudentRepository;

  @Autowired
  AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;

  @Autowired
  SessionRepository sessionRepository;

  @Autowired
  AssessmentRepository assessmentRepository;

  @Autowired
  RestUtils restUtils;

  @AfterEach
  public void after() {
    this.assessmentStudentRepository.deleteAll();
    this.assessmentStudentHistoryRepository.deleteAll();
    this.assessmentRepository.deleteAll();
    this.sessionRepository.deleteAll();
  }

  @Test
  void testGetStudentByID_WhenStudentExistInDB_ShouldReturnStudent()  {
    //given student exists in db
    SessionEntity sessionEntity = sessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentStudentEntity assessmentStudentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    //when retrieving the student
    AssessmentStudentEntity student = service.getStudentByID(assessmentStudentEntity.getAssessmentStudentID());

    //then student is returned
    assertNotNull(student);
  }

  @Test
  void testGetStudentBy_AssessmentIDAndStudentID_WhenStudentExistInDB_ShouldReturnStudent()  {
    //given student exists in db
    SessionEntity sessionEntity = sessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentStudentEntity assessmentStudentEntity = assessmentStudentRepository.save(createMockStudentEntity(assessmentEntity));

    //when retrieving the student
    Optional<AssessmentStudentEntity> student = service.getStudentByAssessmentIDAndStudentID(assessmentEntity.getAssessmentID(), assessmentStudentEntity.getStudentID());

    //then student is returned
    assertThat(student).isPresent();
  }

  @Test
  void testGetStudentByID_WhenStudentDoesNotExistInDB_Return404()  {
    //given student does not exist in database
    //when attempting to retrieve student
    UUID id = UUID.randomUUID();
    //then throw exception
    assertThrows(EntityNotFoundException.class, () -> service.getStudentByID(id));
  }

  @Test
  void testCreateStudent_WhenStudentDoesNotExistInDB_ShouldReturnStudent()  {
    //given session exists
    SessionEntity sessionEntity = sessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP12.getCode()));

    var school = this.createMockSchool();
    UUID schoolID = UUID.randomUUID();
    school.setSchoolId(String.valueOf(schoolID));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    AssessmentStudentEntity assessmentStudentEntity= createMockStudentEntity(assessmentEntity);
    assessmentStudentEntity.setAssessmentStudentID(null);

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(assessmentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName(assessmentStudentEntity.getGivenName());
    studentAPIStudent.setLegalLastName(assessmentStudentEntity.getSurname());
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(studentAPIStudent);

    //when creating an assessment student
    AssessmentStudent student = service.createStudent(assessmentStudentEntity);
    List<AssessmentStudentHistoryEntity> studentHistory = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(assessmentEntity.getAssessmentID(), UUID.fromString(student.getAssessmentStudentID()));
    //then assessment student is created
    assertNotNull(student);
    assertNotNull(assessmentStudentRepository.findById(UUID.fromString(student.getStudentID())));
    assertThat(studentHistory).hasSize(1);
  }

  @Test
  void testUpdateStudent_WhenStudentExistInDB_ShouldReturnUpdatedStudent()  {
    //given student exists in db
    SessionEntity sessionEntity = sessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    AssessmentStudentEntity studentEntity= createMockStudentEntity(assessmentEntity);
    studentEntity.setAssessmentStudentID(null);

    var school = this.createMockSchool();
    UUID schoolID = UUID.randomUUID();
    school.setSchoolId(String.valueOf(schoolID));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    AssessmentStudentEntity assessmentStudentEntity= createMockStudentEntity(assessmentEntity);
    assessmentStudentEntity.setAssessmentStudentID(null);

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(assessmentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName(assessmentStudentEntity.getGivenName());
    studentAPIStudent.setLegalLastName(assessmentStudentEntity.getSurname());
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(studentAPIStudent);

    AssessmentStudent assessmentStudent = service.createStudent(studentEntity);
    //when updating the student
    AssessmentStudent student = service.updateStudent(mapper.toModel(assessmentStudent));
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
    SessionEntity sessionEntity = sessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP10.getCode()));

    //given student does not exist in database
    //when attempting to update student
    AssessmentStudentEntity student = AssessmentStudentEntity.builder().assessmentStudentID(UUID.randomUUID()).pen("120164447").schoolID(UUID.randomUUID()).studentID(UUID.randomUUID()).assessmentEntity(assessmentEntity).build();

    //then throw exception
    assertThrows(EntityNotFoundException.class, () -> service.updateStudent(student));
  }

  @Test
  void testUpdateStudent_WhenSessionIDDoesNotExistInDB_ShouldThrowError()  {
    //given student existing in db
    SessionEntity sessionEntity = createMockSessionEntity();
    //assessment does not exist in db
    AssessmentEntity assessmentEntity =createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTF12.getCode());

    //when attempting to update to session id that does not exist
    AssessmentStudentEntity student = createMockStudentEntity(assessmentEntity);

    //then throw exception
    assertThrows(EntityNotFoundException.class, () -> service.updateStudent(student));
  }

  @Test
  void testCreateStudent_WhenNamesDoNotMatchStudentAPI_ShouldReturnValidationErrors(){
    SessionEntity sessionEntity = sessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentEntity = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.LTP12.getCode()));

    var school = this.createMockSchool();
    UUID schoolID = UUID.randomUUID();
    school.setSchoolId(String.valueOf(schoolID));
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));

    AssessmentStudentEntity assessmentStudentEntity= createMockStudentEntity(assessmentEntity);
    assessmentStudentEntity.setAssessmentStudentID(null);

    var studentAPIStudent = this.createMockStudentAPIStudent();
    studentAPIStudent.setPen(assessmentStudentEntity.getPen());
    studentAPIStudent.setLegalFirstName("Bugs");
    studentAPIStudent.setLegalLastName("Bunny");
    when(this.restUtils.getStudentByPEN(any(UUID.class), anyString())).thenReturn(studentAPIStudent);

    AssessmentStudent student = service.createStudent(assessmentStudentEntity);
    assertThat(student.getAssessmentStudentValidationIssues()).hasSize(2);
  }

  @Test
  void testGetStudentsByAssessmentIDsInAndStudentID_WithNumeracyCodes_ReturnsAllNumeracyRegistrations() {
    SessionEntity sessionEntity = sessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentNME10 = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.NME10.getCode()));
    AssessmentEntity assessmentNMF10 = assessmentRepository.save(createMockAssessmentEntity(sessionEntity, AssessmentTypeCodes.NMF10.getCode()));

    AssessmentStudentEntity studentEntity1 = createMockStudentEntity(assessmentNME10);
    AssessmentStudentEntity studentEntity2 = createMockStudentEntity(assessmentNMF10);
    studentEntity2.setStudentID(studentEntity1.getStudentID());

    assessmentStudentRepository.save(studentEntity1);
    assessmentStudentRepository.save(studentEntity2);

    List<UUID> numeracyAssessmentIDs = List.of(assessmentNME10.getAssessmentID(), assessmentNMF10.getAssessmentID());
    List<AssessmentStudentEntity> found = service.getStudentsByAssessmentIDsInAndStudentID(numeracyAssessmentIDs, studentEntity1.getStudentID());

    assertThat(found).hasSize(2);
    assertThat(found.stream().map(AssessmentStudentEntity::getAssessmentEntity).map(AssessmentEntity::getAssessmentTypeCode).anyMatch(code -> code.equals(AssessmentTypeCodes.NME10.getCode()))).isTrue();
    assertThat(found.stream().map(AssessmentStudentEntity::getAssessmentEntity).map(AssessmentEntity::getAssessmentTypeCode).anyMatch(code -> code.equals(AssessmentTypeCodes.NMF10.getCode()))).isTrue();
  }
}
