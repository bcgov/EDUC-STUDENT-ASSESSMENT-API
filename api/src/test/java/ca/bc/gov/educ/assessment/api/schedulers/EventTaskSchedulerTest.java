package ca.bc.gov.educ.assessment.api.schedulers;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentFormEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedStudentResultEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EventTaskSchedulerTest extends BaseAssessmentAPITest {

  @Autowired
  EventTaskScheduler eventTaskScheduler;
  @Autowired
  MessagePublisher messagePublisher;
  @MockBean
  protected RestUtils restUtils;
  @Captor
  ArgumentCaptor<byte[]> eventCaptor;
  @Autowired
  StagedStudentResultRepository stagedStudentResultRepository;
  @Autowired
  private AssessmentFormRepository assessmentFormRepository;
  private AssessmentEntity savedAssessmentEntity;
  private AssessmentFormEntity savedAssessmentFormEntity;
  @Autowired
  AssessmentRepository assessmentRepository;
  @Autowired
  AssessmentSessionRepository assessmentSessionRepository;
  @Autowired
  StagedAssessmentStudentRepository stagedAssessmentStudentRepository;

  @BeforeEach
  void setUp() {
    stagedAssessmentStudentRepository.deleteAll();
    stagedStudentResultRepository.deleteAll();
    assessmentFormRepository.deleteAll();
    assessmentRepository.deleteAll();
    assessmentSessionRepository.deleteAll();
    stagedAssessmentStudentRepository.deleteAll();

    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    savedAssessmentEntity = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTP10.getCode()));
    savedAssessmentFormEntity = assessmentFormRepository.save(createMockAssessmentFormEntity(savedAssessmentEntity, "A"));
  }

  @Test
  void purgeCompletedResultsFromStaging() {
    var studentResult = StagedStudentResultEntity
            .builder()
            .assessmentEntity(savedAssessmentEntity)
            .assessmentFormID(savedAssessmentFormEntity.getAssessmentFormID())
            .pen("123456789")
            .mincode("07965039")
            .stagedStudentResultStatus("COMPLETED")
            .componentType("1")
            .oeMarks("03.003.004.003.003.003.0")
            .mcMarks("00.000.000.001.001.000.001.001.001.001.000.001.001.000.000.000.001.001.000.001.001.000.001.000.001.001.000.001")
            .choicePath(null)
            .provincialSpecialCaseCode(null)
            .proficiencyScore(3)
            .adaptedAssessmentCode(null)
            .irtScore("0.4733")
            .markingSession(null)
            .createUser("TEST")
            .createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now())
            .updateUser("TEST")
            .build();

    var savedStudentResult = stagedStudentResultRepository.save(studentResult);
    eventTaskScheduler.purgeCompletedResultsFromStaging();

    var entity = stagedStudentResultRepository.findById(savedStudentResult.getStagedStudentResultID());
    assertThat(entity).isEmpty();
  }

  @Test
  void processLoadedStudents() throws JsonProcessingException {
    var school = this.createMockSchoolTombstone();
    school.setMincode("07965039");
    when(this.restUtils.getSchoolByMincode(anyString())).thenReturn(Optional.of(school));
    var studentResult = StagedStudentResultEntity
            .builder()
            .assessmentEntity(savedAssessmentEntity)
            .assessmentFormID(savedAssessmentFormEntity.getAssessmentFormID())
            .pen("123456789")
            .mincode("07965039")
            .stagedStudentResultStatus("LOADED")
            .componentType("1")
            .oeMarks("03.003.004.003.003.003.0")
            .mcMarks("00.000.000.001.001.000.001.001.001.001.000.001.001.000.000.000.001.001.000.001.001.000.001.000.001.001.000.001")
            .choicePath(null)
            .provincialSpecialCaseCode(null)
            .proficiencyScore(3)
            .adaptedAssessmentCode(null)
            .irtScore("0.4733")
            .markingSession(null)
            .createUser("TEST")
            .createDate(LocalDateTime.now())
            .updateDate(LocalDateTime.now())
            .updateUser("TEST")
            .build();

    var savedStudentResult = stagedStudentResultRepository.save(studentResult);

    eventTaskScheduler.processLoadedStudents();
    verify(this.messagePublisher, atMost(1)).dispatchMessage(eq(TopicsEnum.READ_STUDENT_RESULT_RECORD.toString()), this.eventCaptor.capture());
    final var newEvent = JsonUtil.getJsonObjectFromString(Event.class, new String(this.eventCaptor.getValue()));
    assertThat(newEvent.getEventType()).isEqualTo(EventType.READ_STUDENT_RESULT_FOR_PROCESSING);
  }

  @Test
  void processTransferStudents_WhenTransferStudentsExist_ShouldCallAsyncService() {
    var stagedStudent1 = createMockStagedStudentEntity(savedAssessmentEntity);
    stagedStudent1.setStagedAssessmentStudentStatus("TRANSFER");
    var stagedStudent2 = createMockStagedStudentEntity(savedAssessmentEntity);
    stagedStudent2.setStagedAssessmentStudentStatus("TRANSFER");

    var savedStudent1 = stagedAssessmentStudentRepository.save(stagedStudent1);
    var savedStudent2 = stagedAssessmentStudentRepository.save(stagedStudent2);

    eventTaskScheduler.processTransferStudents();

    var updatedStudent1 = stagedAssessmentStudentRepository.findById(savedStudent1.getAssessmentStudentID()).orElse(null);
    var updatedStudent2 = stagedAssessmentStudentRepository.findById(savedStudent2.getAssessmentStudentID()).orElse(null);

    assertThat(updatedStudent1).isNotNull();
    assertThat(updatedStudent1.getStagedAssessmentStudentStatus()).isEqualTo("TRANSFERIN");
    assertThat(updatedStudent2).isNotNull();
    assertThat(updatedStudent2.getStagedAssessmentStudentStatus()).isEqualTo("TRANSFERIN");
  }
}
