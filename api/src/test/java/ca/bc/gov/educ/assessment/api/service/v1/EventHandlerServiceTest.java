package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.SessionEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentHistoryRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.SessionRepository;
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
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest
@Slf4j
class EventHandlerServiceTest extends BaseAssessmentAPITest {

  public static final String ASSESSMENT_API_TOPIC = TopicsEnum.STUDENT_ASSESSMENT_API_TOPIC.toString();
  @Autowired
  SessionRepository sessionRepository;

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
    sessionRepository.save(createMockSessionEntity());
  }

  @AfterEach
  public void tearDown() {
    assessmentStudentRepository.deleteAll();
    assessmentStudentHistoryRepository.deleteAll();
    assessmentRepository.deleteAll();
    sessionRepository.deleteAll();
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
    SessionEntity session = sessionRepository.save(createMockSessionEntity());
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
    SessionEntity session = sessionRepository.save(createMockSessionEntity());
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
    SessionEntity session = sessionRepository.save(createMockSessionEntity());
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
    SessionEntity session = sessionRepository.save(createMockSessionEntity());
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
    SessionEntity session = sessionRepository.save(createMockSessionEntity());
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
}
