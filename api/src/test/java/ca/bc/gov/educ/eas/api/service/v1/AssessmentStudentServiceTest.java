package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.eas.api.constants.v1.StatusCodes;
import ca.bc.gov.educ.eas.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.SessionRepository;
import jakarta.persistence.EntityExistsException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@Slf4j
class AssessmentStudentServiceTest {

  @Autowired
  AssessmentStudentService service;

  @Autowired
  AssessmentStudentRepository repository;

  @Autowired
  AssessmentStudentRepository assessmentStudentRepository;

  @Autowired
  SessionRepository sessionRepository;

  @Test
  void testGetStudentByID_WhenStudentExistInDB_ShouldReturnStudent()  {
    //given student exists in db
    SessionEntity sessionEntity = sessionRepository.save(SessionEntity.builder().sessionID(UUID.randomUUID()).activeFromDate(LocalDateTime.now()).activeUntilDate(LocalDateTime.now()).statusCode(StatusCodes.OPEN.getCode()).updateDate(LocalDateTime.now()).updateUser("USER").build());

    AssessmentStudentEntity assessmentStudentEntity = repository.save(AssessmentStudentEntity.builder().assessmentTypeCode(AssessmentTypeCodes.LTF12.getCode()).pen("120164447").schoolID(UUID.randomUUID()).studentID(UUID.randomUUID()).sessionEntity(SessionEntity.builder().sessionID(sessionEntity.getSessionID()).build()).build());

    //when retrieving the student
    AssessmentStudentEntity student = service.getStudentByID(assessmentStudentEntity.getAssessmentStudentID());

    //then student is returned
    assertNotNull(student);
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
    SessionEntity sessionEntity = sessionRepository.save(SessionEntity.builder().sessionID(UUID.randomUUID()).activeFromDate(LocalDateTime.now()).activeUntilDate(LocalDateTime.now()).statusCode(StatusCodes.OPEN.getCode()).updateDate(LocalDateTime.now()).updateUser("USER").build());

    //when creating an assessment student
    AssessmentStudentEntity student = service.createStudent(AssessmentStudentEntity.builder().assessmentTypeCode(AssessmentTypeCodes.LTF12.getCode()).pen("120164447").schoolID(UUID.randomUUID()).studentID(UUID.randomUUID()).sessionEntity(SessionEntity.builder().sessionID(sessionEntity.getSessionID()).build()).build());

    //then assessment student is created
    assertNotNull(student);
    assertNotNull(repository.findById(student.getStudentID()));
  }

  @Test
  void testCreateStudent_WhenStudentAlreadyInThisSession_ShouldThrowError()  {
    //given studentID exists in db for a session
    SessionEntity sessionEntity = sessionRepository.save(SessionEntity.builder().sessionID(UUID.randomUUID()).activeFromDate(LocalDateTime.now()).activeUntilDate(LocalDateTime.now()).statusCode(StatusCodes.OPEN.getCode()).updateDate(LocalDateTime.now()).updateUser("USER").build());

    AssessmentStudentEntity assessmentStudentEntity = repository.save(AssessmentStudentEntity.builder().assessmentTypeCode(AssessmentTypeCodes.LTF12.getCode()).pen("120164447").schoolID(UUID.randomUUID()).studentID(UUID.randomUUID()).sessionEntity(SessionEntity.builder().sessionID(sessionEntity.getSessionID()).build()).build());

    //when attempting to create a new assessment student with same studentID and session
    assessmentStudentEntity.setAssessmentStudentID(UUID.randomUUID());

    //then throw exception
    assertThrows(EntityExistsException.class, () -> service.createStudent(assessmentStudentEntity));
  }

  @Test
  void testCreateStudent_WhenSessionIDDoesNotExistInDB_ShouldThrowError()  {
    //when attempting to create student with invalid session id
    AssessmentStudentEntity student = AssessmentStudentEntity.builder().assessmentTypeCode(AssessmentTypeCodes.LTF12.getCode()).pen("120164447").schoolID(UUID.randomUUID()).studentID(UUID.randomUUID()).sessionEntity(SessionEntity.builder().sessionID(UUID.randomUUID()).build()).build();

    //then throw exception
    assertThrows(EntityNotFoundException.class, () -> service.createStudent(student));
  }

  @Test
  void testUpdateStudent_WhenStudentExistInDB_ShouldReturnUpdatedStudent()  {
    //given student exists in db
    SessionEntity sessionEntity = sessionRepository.save(SessionEntity.builder().sessionID(UUID.randomUUID()).activeFromDate(LocalDateTime.now()).activeUntilDate(LocalDateTime.now()).statusCode(StatusCodes.OPEN.getCode()).updateDate(LocalDateTime.now()).updateUser("USER").build());

    AssessmentStudentEntity assessmentStudentEntity = repository.save(AssessmentStudentEntity.builder().assessmentTypeCode(AssessmentTypeCodes.LTF12.getCode()).pen("120164447").schoolID(UUID.randomUUID()).studentID(UUID.randomUUID()).sessionEntity(SessionEntity.builder().sessionID(sessionEntity.getSessionID()).build()).build());

    //when updating the student
    assessmentStudentEntity.setAssessmentTypeCode(AssessmentTypeCodes.LTP10.getCode());
    AssessmentStudentEntity student = service.updateStudent(assessmentStudentEntity);
    assertNotNull(student);

    //then student is updated and saved
    var updatedStudent = repository.findById(student.getAssessmentStudentID());
    assertThat(updatedStudent).isPresent();
    assertThat(updatedStudent.get().getAssessmentTypeCode()).isEqualTo(AssessmentTypeCodes.LTP10.getCode());
  }

  @Test
  void testUpdateStudent_WhenStudentDoesNotExistInDB_ReturnError()  {
    //given student does not exist in database
    //when attempting to update student
    AssessmentStudentEntity student = AssessmentStudentEntity.builder().assessmentStudentID(UUID.randomUUID()).assessmentTypeCode(AssessmentTypeCodes.LTF12.getCode()).pen("120164447").schoolID(UUID.randomUUID()).studentID(UUID.randomUUID()).sessionEntity(SessionEntity.builder().sessionID(UUID.randomUUID()).build()).build();

    //then throw exception
    assertThrows(EntityNotFoundException.class, () -> service.updateStudent(student));
  }

  @Test
  void testUpdateStudent_WhenStudentAlreadyInThisSession_ShouldThrowError()  {
    //given two existing students in the session
    SessionEntity sessionEntity = sessionRepository.save(SessionEntity.builder().sessionID(UUID.randomUUID()).activeFromDate(LocalDateTime.now()).activeUntilDate(LocalDateTime.now()).statusCode(StatusCodes.OPEN.getCode()).updateDate(LocalDateTime.now()).updateUser("USER").build());

    UUID studentUUID = UUID.randomUUID();
    AssessmentStudentEntity studentEntity = repository.save(AssessmentStudentEntity.builder().assessmentTypeCode(AssessmentTypeCodes.LTF12.getCode()).pen("120164447").schoolID(UUID.randomUUID()).studentID(studentUUID).sessionEntity(SessionEntity.builder().sessionID(sessionEntity.getSessionID()).build()).build());

    //when updating one student to have the same studentID and sessionID as another student
    studentEntity.setAssessmentStudentID(null);
    studentEntity.setStudentID(UUID.randomUUID());
    AssessmentStudentEntity studentEntity2 = repository.save(studentEntity);
    studentEntity2.setStudentID(studentUUID);

    //then exception thrown
    assertThrows(EntityExistsException.class, () -> service.updateStudent(studentEntity2));
  }

  @Test
  void testUpdateStudent_WhenSessionIDDoesNotExistInDB_ShouldThrowError()  {
    //given student existing in db
    SessionEntity sessionEntity = sessionRepository.save(SessionEntity.builder().sessionID(UUID.randomUUID()).activeFromDate(LocalDateTime.now()).activeUntilDate(LocalDateTime.now()).statusCode(StatusCodes.OPEN.getCode()).updateDate(LocalDateTime.now()).updateUser("USER").build());

    //when attempting to update to session id that does not exist
    AssessmentStudentEntity student = repository.save(AssessmentStudentEntity.builder().assessmentTypeCode(AssessmentTypeCodes.LTF12.getCode()).pen("120164447").schoolID(UUID.randomUUID()).studentID(UUID.randomUUID()).sessionEntity(SessionEntity.builder().sessionID(sessionEntity.getSessionID()).build()).build());

    student.setSessionEntity(SessionEntity.builder().sessionID(UUID.randomUUID()).build());

    //then throw exception
    assertThrows(EntityNotFoundException.class, () -> service.updateStudent(student));
  }
}
