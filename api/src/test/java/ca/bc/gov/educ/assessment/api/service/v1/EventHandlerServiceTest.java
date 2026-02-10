package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentHistoryRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.service.v1.events.EventHandlerService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentDetailResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentGet;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentListItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@Slf4j
class EventHandlerServiceTest extends BaseAssessmentAPITest {

  public static final String ASSESSMENT_API_TOPIC = TopicsEnum.STUDENT_ASSESSMENT_API_TOPIC.toString();
  @Autowired
  AssessmentSessionRepository assessmentSessionRepository;

  @Autowired
  AssessmentRepository assessmentRepository;

  @Autowired
  AssessmentStudentRepository assessmentStudentRepository;

  @Autowired
  AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;

  @Autowired
  EventHandlerService eventHandlerServiceUnderTest;
  private final boolean isSynchronous = false;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    assessmentSessionRepository.save(createMockSessionEntity());
  }

  @AfterEach
  public void tearDown() {
    assessmentStudentRepository.deleteAll();
    assessmentStudentHistoryRepository.deleteAll();
    assessmentRepository.deleteAll();
    assessmentSessionRepository.deleteAll();
  }

  @Test
  void testHandleEvent_givenEventTypeGET_OPEN_ASSESSMENT_SESSIONS_shouldHaveEventOutcomeSESSIONS_FOUND() throws IOException {
    var sagaId = UUID.randomUUID();
    final Event event = Event.builder().eventType(EventType.GET_OPEN_ASSESSMENT_SESSIONS).sagaId(sagaId).replyTo(ASSESSMENT_API_TOPIC).eventPayload(UUID.randomUUID().toString()).build();
    byte[] response = eventHandlerServiceUnderTest.handleGetOpenAssessmentSessionsEvent(event, isSynchronous);
    assertThat(response).isNotEmpty();
    Event responseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class, response);
    assertThat(responseEvent).isNotNull();
    assertThat(responseEvent.getEventOutcome()).isEqualTo(EventOutcome.SESSIONS_FOUND);
  }

  @Test
  void testHandleEvent_givenEventTypePROCESS_STUDENT_REGISTRATION__whenNoStudentExist_shouldHaveEventOutcome_CREATED() throws IOException {
    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudent student1 = createMockStudent();
    student1.setAssessmentID(assessment.getAssessmentID().toString());

    var sagaId = UUID.randomUUID();
    final Event event = Event.builder().eventType(EventType.PROCESS_STUDENT_REGISTRATION).sagaId(sagaId).replyTo(ASSESSMENT_API_TOPIC).eventPayload(JsonUtil.getJsonStringFromObject(student1)).build();
    var response = eventHandlerServiceUnderTest.handleProcessStudentRegistrationEvent(event);
    assertThat(response.getLeft()).isNotEmpty();
    Event responseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class, response.getLeft());
    assertThat(responseEvent).isNotNull();
    assertThat(responseEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_REGISTRATION_PROCESSED_IN_ASSESSMENT_API);
  }

  @Test
  void testHandleEvent_givenEventTypePROCESS_STUDENT_REGISTRATION__whenNoStudentExist_shouldHaveEventOutcome_EXISTS() throws IOException {
    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudent student1 = createMockStudent();
    student1.setAssessmentID(assessment.getAssessmentID().toString());

    var sagaId = UUID.randomUUID();
    final Event event = Event.builder().eventType(EventType.PROCESS_STUDENT_REGISTRATION).sagaId(sagaId).replyTo(ASSESSMENT_API_TOPIC).eventPayload(JsonUtil.getJsonStringFromObject(student1)).build();
    var response = eventHandlerServiceUnderTest.handleProcessStudentRegistrationEvent(event);
    assertThat(response.getLeft()).isNotEmpty();
    Event responseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class, response.getLeft());
    assertThat(responseEvent).isNotNull();
    assertThat(responseEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_REGISTRATION_PROCESSED_IN_ASSESSMENT_API);

    var responseDuplicate = eventHandlerServiceUnderTest.handleProcessStudentRegistrationEvent(event);
    assertThat(responseDuplicate.getLeft()).isNotEmpty();
    Event responseEventDuplicate = JsonUtil.getJsonObjectFromByteArray(Event.class, responseDuplicate.getLeft());
    assertThat(responseEventDuplicate).isNotNull();
    assertThat(responseEventDuplicate.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_REGISTRATION_PROCESSED_IN_ASSESSMENT_API);
  }

  @Test
  void testHandleEvent_givenEventTypeGET_STUDENT_ASSESSMENT_DETAILS_shouldReturnResponse() throws IOException {
    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
    AssessmentStudent student1 = createMockStudent();
    student1.setAssessmentID(assessment.getAssessmentID().toString());
    student1.setProficiencyScore("1");

    var sagaId = UUID.randomUUID();
    final Event event = Event.builder().eventType(EventType.PROCESS_STUDENT_REGISTRATION).sagaId(sagaId).replyTo(ASSESSMENT_API_TOPIC).eventPayload(JsonUtil.getJsonStringFromObject(student1)).build();
    var response = eventHandlerServiceUnderTest.handleProcessStudentRegistrationEvent(event);
    assertThat(response.getLeft()).isNotEmpty();
    Event responseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class, response.getLeft());
    assertThat(responseEvent).isNotNull();
    assertThat(responseEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_REGISTRATION_PROCESSED_IN_ASSESSMENT_API);

    AssessmentStudentGet assessmentStudentGet = createMockAssessmentStudentGet(assessment.getAssessmentID().toString(), student1.getStudentID());
    final Event getStudentEvent = Event.builder().eventType(EventType.GET_STUDENT_ASSESSMENT_DETAILS).sagaId(sagaId).replyTo(ASSESSMENT_API_TOPIC).eventPayload(JsonUtil.getJsonStringFromObject(assessmentStudentGet)).build();
    byte[] studentResponse = eventHandlerServiceUnderTest.handleGetStudentAssessmentDetailEvent(getStudentEvent);
    assertThat(studentResponse).isNotEmpty();
    AssessmentStudentDetailResponse studentResponseEvent = JsonUtil.getJsonObjectFromByteArray(AssessmentStudentDetailResponse.class, studentResponse);
    assertThat(studentResponseEvent).isNotNull();
    assertThat(studentResponseEvent.getNumberOfAttempts()).isEqualTo("1");
    assertThat(studentResponseEvent.isHasPriorRegistration()).isTrue();
  }

  @Test
  void testHandleEvent_givenEventTypeGET_STUDENT_ASSESSMENT_DETAILS_WithNME_shouldReturnResponse() throws IOException {
    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.NME10.getCode()));
    AssessmentStudent student1 = createMockStudent();
    student1.setAssessmentID(assessment.getAssessmentID().toString());
    student1.setProficiencyScore("1");

    var sagaId = UUID.randomUUID();
    final Event event = Event.builder().eventType(EventType.PROCESS_STUDENT_REGISTRATION).sagaId(sagaId).replyTo(ASSESSMENT_API_TOPIC).eventPayload(JsonUtil.getJsonStringFromObject(student1)).build();
    var response = eventHandlerServiceUnderTest.handleProcessStudentRegistrationEvent(event);
    assertThat(response.getLeft()).isNotEmpty();
    Event responseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class, response.getLeft());
    assertThat(responseEvent).isNotNull();
    assertThat(responseEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_REGISTRATION_PROCESSED_IN_ASSESSMENT_API);

    AssessmentStudentGet assessmentStudentGet = createMockAssessmentStudentGet(assessment.getAssessmentID().toString(), student1.getStudentID());
    final Event getStudentEvent = Event.builder().eventType(EventType.GET_STUDENT_ASSESSMENT_DETAILS).sagaId(sagaId).replyTo(ASSESSMENT_API_TOPIC).eventPayload(JsonUtil.getJsonStringFromObject(assessmentStudentGet)).build();
    byte[] studentResponse = eventHandlerServiceUnderTest.handleGetStudentAssessmentDetailEvent(getStudentEvent);
    assertThat(studentResponse).isNotEmpty();
    AssessmentStudentDetailResponse studentResponseEvent = JsonUtil.getJsonObjectFromByteArray(AssessmentStudentDetailResponse.class, studentResponse);
    assertThat(studentResponseEvent).isNotNull();
    assertThat(studentResponseEvent.getNumberOfAttempts()).isEqualTo("1");
    assertThat(studentResponseEvent.isHasPriorRegistration()).isTrue();
  }

  @Test
  void testHandleEvent_givenNumeracyAssessmentsAcrossCodes_shouldTreatAsSameForAttemptsAndRegistration() throws IOException {
    AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
    AssessmentEntity assessmentNME10 = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.NME10.getCode()));
    AssessmentEntity assessmentNMF10 = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.NMF10.getCode()));

    AssessmentStudent student1 = createMockStudent();
    student1.setAssessmentID(assessmentNME10.getAssessmentID().toString());
    student1.setProficiencyScore("1");
    var sagaId = UUID.randomUUID();
    final Event event1 = Event.builder().eventType(EventType.PROCESS_STUDENT_REGISTRATION).sagaId(sagaId).replyTo(ASSESSMENT_API_TOPIC).eventPayload(JsonUtil.getJsonStringFromObject(student1)).build();
    eventHandlerServiceUnderTest.handleProcessStudentRegistrationEvent(event1);

    AssessmentStudent student2 = createMockStudent();
    student2.setAssessmentID(assessmentNMF10.getAssessmentID().toString());
    student2.setStudentID(student1.getStudentID());
    student2.setProficiencyScore("1");
    final Event event2 = Event.builder().eventType(EventType.PROCESS_STUDENT_REGISTRATION).sagaId(sagaId).replyTo(ASSESSMENT_API_TOPIC).eventPayload(JsonUtil.getJsonStringFromObject(student2)).build();
    eventHandlerServiceUnderTest.handleProcessStudentRegistrationEvent(event2);

    AssessmentStudentGet assessmentStudentGet = createMockAssessmentStudentGet(assessmentNME10.getAssessmentID().toString(), student1.getStudentID());
    final Event getStudentEvent = Event.builder().eventType(EventType.GET_STUDENT_ASSESSMENT_DETAILS).sagaId(sagaId).replyTo(ASSESSMENT_API_TOPIC).eventPayload(JsonUtil.getJsonStringFromObject(assessmentStudentGet)).build();
    byte[] studentResponse = eventHandlerServiceUnderTest.handleGetStudentAssessmentDetailEvent(getStudentEvent);
    assertThat(studentResponse).isNotEmpty();
    AssessmentStudentDetailResponse studentResponseEvent = JsonUtil.getJsonObjectFromByteArray(AssessmentStudentDetailResponse.class, studentResponse);
    assertThat(studentResponseEvent).isNotNull();
    assertThat(studentResponseEvent.isHasPriorRegistration()).isTrue();
    assertThat(Integer.parseInt(studentResponseEvent.getNumberOfAttempts())).isEqualTo(2);
  }

  @Nested
  @DisplayName("GET_ASSESSMENT_STUDENTS Event Tests")
  class GetAssessmentStudentsEventTests {

    @Test
    @DisplayName("Should return list when multiple students exist for same student ID")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenStudentsExist_shouldReturnStudentList() throws IOException {
      // Given
      AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
      AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
      
      // Create multiple assessment students for the same student
      UUID studentId = UUID.randomUUID();
      UUID studentId2 = UUID.randomUUID();
      AssessmentStudentEntity student1 = createMockAssessmentStudentEntity(assessment, studentId);
      AssessmentStudentEntity student2 = createMockAssessmentStudentEntity(assessment, studentId2);
      
      assessmentStudentRepository.save(student1);
      assessmentStudentRepository.save(student2);

      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(ASSESSMENT_API_TOPIC)
          .eventPayload(studentId.toString())
          .build();

      // When
      byte[] response = eventHandlerServiceUnderTest.handleGetAssessmentStudentsEvent(event);

      // Then
      assertThat(response).isNotEmpty();
      List<AssessmentStudentListItem> studentList = deserializeStudentList(response);
      assertThat(studentList).hasSize(1);
    }

    @Test
    @DisplayName("Should return empty byte array when no students exist")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenNoStudentsExist_shouldReturnEmptyByteArray() throws IOException {
      // Given
      UUID studentId = UUID.randomUUID();
      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(ASSESSMENT_API_TOPIC)
          .eventPayload(studentId.toString())
          .build();

      // When
      byte[] response = eventHandlerServiceUnderTest.handleGetAssessmentStudentsEvent(event);

      // Then
      assertThat(response).isEmpty();
    }

    @Test
    @DisplayName("Should return single student when one student exists")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenSingleStudentExists_shouldReturnStudentList() throws IOException {
      // Given
      AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
      AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
      
      UUID studentId = UUID.randomUUID();
      AssessmentStudentEntity student = createMockAssessmentStudentEntity(assessment, studentId);
      assessmentStudentRepository.save(student);

      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(ASSESSMENT_API_TOPIC)
          .eventPayload(studentId.toString())
          .build();

      // When
      byte[] response = eventHandlerServiceUnderTest.handleGetAssessmentStudentsEvent(event);

      // Then
      assertThat(response).isNotEmpty();
      List<AssessmentStudentListItem> studentList = deserializeStudentList(response);
      assertThat(studentList).hasSize(1);
      
      AssessmentStudentListItem returnedStudent = studentList.getFirst();
      assertThat(returnedStudent.getStudentID()).isEqualTo(studentId.toString());
      assertThat(returnedStudent.getAssessmentID()).isEqualTo(assessment.getAssessmentID().toString());
      assertThat(returnedStudent.getAssessmentTypeCode()).isEqualTo(assessment.getAssessmentTypeCode());
      assertThat(returnedStudent.getSessionID()).isEqualTo(session.getSessionID().toString());
      assertThat(returnedStudent.getCourseYear()).isEqualTo(session.getCourseYear());
      assertThat(returnedStudent.getCourseMonth()).isEqualTo(session.getCourseMonth());
    }

    @Test
    @DisplayName("Should return all assessments when multiple assessments exist for same student")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenMultipleAssessmentsForSameStudent_shouldReturnAllAssessments() throws IOException {
      // Given
      AssessmentSessionEntity session1 = assessmentSessionRepository.save(createMockSessionEntity());
      AssessmentSessionEntity session2 = assessmentSessionRepository.save(createMockSessionEntity());
      
      AssessmentEntity assessment1 = assessmentRepository.save(createMockAssessmentEntity(session1, AssessmentTypeCodes.LTF12.getCode()));
      AssessmentEntity assessment2 = assessmentRepository.save(createMockAssessmentEntity(session2, AssessmentTypeCodes.LTP10.getCode()));
      
      UUID studentId = UUID.randomUUID();
      AssessmentStudentEntity student1 = createMockAssessmentStudentEntity(assessment1, studentId);
      AssessmentStudentEntity student2 = createMockAssessmentStudentEntity(assessment2, studentId);
      
      assessmentStudentRepository.save(student1);
      assessmentStudentRepository.save(student2);

      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(ASSESSMENT_API_TOPIC)
          .eventPayload(studentId.toString())
          .build();

      // When
      byte[] response = eventHandlerServiceUnderTest.handleGetAssessmentStudentsEvent(event);

      // Then
      assertThat(response).isNotEmpty();
      List<AssessmentStudentListItem> studentList = deserializeStudentList(response);
      assertThat(studentList).hasSize(2);
      
      // Verify both assessments are returned
      List<String> assessmentIds = studentList.stream()
          .map(AssessmentStudentListItem::getAssessmentID)
          .toList();
      assertThat(assessmentIds)
          .contains(assessment1.getAssessmentID().toString())
          .contains(assessment2.getAssessmentID().toString());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when event payload is invalid UUID")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenInvalidUUID_shouldThrowException() {
      // Given
      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(ASSESSMENT_API_TOPIC)
          .eventPayload("invalid-uuid")
          .build();

      // When & Then
      assertThatThrownBy(() -> eventHandlerServiceUnderTest.handleGetAssessmentStudentsEvent(event))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should throw NullPointerException when event payload is null")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenNullEventPayload_shouldThrowException() {
      // Given
      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(ASSESSMENT_API_TOPIC)
          .eventPayload(null)
          .build();

      // When & Then
      assertThatThrownBy(() -> eventHandlerServiceUnderTest.handleGetAssessmentStudentsEvent(event))
          .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when event payload is empty string")
    void testHandleEvent_givenEventTypeGET_ASSESSMENT_STUDENTS_whenEmptyEventPayload_shouldThrowException() {
      // Given
      var sagaId = UUID.randomUUID();
      final Event event = Event.builder()
          .eventType(EventType.GET_ASSESSMENT_STUDENTS)
          .sagaId(sagaId)
          .replyTo(ASSESSMENT_API_TOPIC)
          .eventPayload("")
          .build();

      // When & Then
      assertThatThrownBy(() -> eventHandlerServiceUnderTest.handleGetAssessmentStudentsEvent(event))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

    @Test
    void testHandleEvent_givenExistingStudent_whenUpdateArrives_shouldUpdateAllowedFieldsAndKeepSOROnInvalidUUID() throws IOException {
        AssessmentSessionEntity session = assessmentSessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));

        UUID sorOriginal = UUID.randomUUID();
        AssessmentStudent initial = createMockStudent();
        initial.setAssessmentID(assessment.getAssessmentID().toString());
        initial.setSchoolOfRecordSchoolID(sorOriginal.toString());
        initial.setLocalID("OLD_LOCAL_ID");
        initial.setLocalAssessmentID("OLD_LOCAL_ASSESS_ID");

        var sagaId = UUID.randomUUID();
        final Event createEvent = Event.builder()
                .eventType(EventType.PROCESS_STUDENT_REGISTRATION)
                .sagaId(sagaId)
                .replyTo(ASSESSMENT_API_TOPIC)
                .eventPayload(JsonUtil.getJsonStringFromObject(initial))
                .build();

        var createResponse = eventHandlerServiceUnderTest.handleProcessStudentRegistrationEvent(createEvent);
        assertThat(createResponse.getLeft()).isNotEmpty();
        Event createResponseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class, createResponse.getLeft());
        assertThat(createResponseEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_REGISTRATION_PROCESSED_IN_ASSESSMENT_API);

        UUID studentUUID = UUID.fromString(initial.getStudentID());
        UUID assessmentUUID = UUID.fromString(initial.getAssessmentID());
        var createdAfterSave = assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(assessmentUUID, studentUUID).orElseThrow();
        UUID sorSavedInDB = createdAfterSave.getSchoolOfRecordSchoolID();

        UUID newAssessmentCenter = UUID.randomUUID();
        AssessmentStudent update = createMockStudent();
        update.setAssessmentID(assessment.getAssessmentID().toString());
        update.setStudentID(initial.getStudentID());
        update.setSchoolOfRecordSchoolID("not-a-uuid");
        update.setLocalID("NEW_LOCAL_ID");
        update.setLocalAssessmentID("NEW_LOCAL_ASSESS_ID");
        update.setAssessmentCenterSchoolID(newAssessmentCenter.toString());

        final Event updateEvent = Event.builder()
                .eventType(EventType.PROCESS_STUDENT_REGISTRATION)
                .sagaId(sagaId)
                .replyTo(ASSESSMENT_API_TOPIC)
                .eventPayload(JsonUtil.getJsonStringFromObject(update))
                .build();

        var updateResponse = eventHandlerServiceUnderTest.handleProcessStudentRegistrationEvent(updateEvent);
        assertThat(updateResponse.getLeft()).isNotEmpty();
        Event updateResponseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class, updateResponse.getLeft());
        assertThat(updateResponseEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_REGISTRATION_PROCESSED_IN_ASSESSMENT_API);

        UUID studentUUID2 = UUID.fromString(initial.getStudentID());
        UUID assessmentUUID2 = UUID.fromString(initial.getAssessmentID());
        AssessmentStudentEntity persisted = assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(assessmentUUID2, studentUUID2).orElseThrow();
        assertThat(persisted.getSchoolOfRecordSchoolID()).isEqualTo(sorSavedInDB);
        assertThat(persisted.getLocalID()).isEqualTo("NEW_LOCAL_ID");
        assertThat(persisted.getLocalAssessmentID()).isEqualTo("NEW_LOCAL_ASSESS_ID");
        assertThat(persisted.getAssessmentCenterSchoolID()).isEqualTo(newAssessmentCenter);
    }

  // Helper method to properly deserialize generic lists
  private List<AssessmentStudentListItem> deserializeStudentList(byte[] response) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(response, new TypeReference<>() {
    });
  }
}
