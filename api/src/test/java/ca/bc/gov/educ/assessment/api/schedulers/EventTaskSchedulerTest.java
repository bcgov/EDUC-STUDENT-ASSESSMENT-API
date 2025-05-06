package ca.bc.gov.educ.assessment.api.schedulers;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentStudentStatusCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentService;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.service.v1.events.EventHandlerService;
import ca.bc.gov.educ.assessment.api.service.v1.events.schedulers.EventTaskSchedulerAsyncService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import io.nats.client.Connection;
import lombok.SneakyThrows;
import org.springframework.transaction.annotation.Transactional;
import org.assertj.core.api.AssertionsForClassTypes;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;
import java.util.List;

class EventTaskSchedulerTest extends BaseAssessmentAPITest {

    private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;

    @Autowired
    EventTaskSchedulerAsyncService eventTaskSchedulerAsyncService;
    @Autowired
    SagaService sagaService;
    @Autowired
    AssessmentStudentService assessmentStudentService;
    @Autowired
    AssessmentService assessmentService;
    @Autowired
    EventHandlerService eventHandlerServiceUnderTest;
    @Autowired
    SagaRepository sagaRepository;
    @Autowired
    SagaEventRepository sagaEventRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    AssessmentCriteriaRepository assessmentCriteriaRepository;
    @Autowired
    AssessmentSessionCriteriaRepository assessmentSessionCriteriaRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    AssessmentStudentRepository assessmentStudentRepository;
    @Autowired
    AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    @Autowired
    AssessmentTypeCodeRepository assessmentTypeCodeRepository;
    @Autowired
    MessagePublisher messagePublisher;
    @Autowired
    Connection connection;

    private AutoCloseable closeable;

    @BeforeEach
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void after() {
        assessmentStudentRepository.deleteAll();
        assessmentStudentHistoryRepository.deleteAll();
        assessmentCriteriaRepository.deleteAll();
        assessmentSessionCriteriaRepository.deleteAll();
        assessmentRepository.deleteAll();
        sessionRepository.deleteAll();
        sagaEventRepository.deleteAll();
        sagaRepository.deleteAll();
    }

    @AfterEach
    void close() throws Exception {
        closeable.close();
    }

    @SneakyThrows
    @Test
    void test_findAndPublishLoadedStudentRecordsForPublishing_WithStatusCode_LOADED_shouldReturnOk() {

        SessionEntity session = sessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));

        AssessmentStudent student1 = createMockStudent();
        student1.setAssessmentID(assessment.getAssessmentID().toString());
        AssessmentStudentEntity studentEntity1 = mapper.toModel(student1);
        studentEntity1.setAssessmentStudentStatusCode(AssessmentStudentStatusCodes.LOADED.getCode());
        AssessmentStudent assessmentStudent = assessmentStudentService.createStudent(studentEntity1);

        final Event event = Event.builder().eventType(EventType.PUBLISH_STUDENT_REGISTRATION_EVENT).eventOutcome(EventOutcome.STUDENT_REGISTRATION_EVENT_READ).eventPayload(JsonUtil.getJsonStringFromObject(assessmentStudent)).build();
        eventHandlerServiceUnderTest.handlePublishStudentRegistrationEvent(event);
        var sagas = sagaRepository.findByAssessmentStudentIDAndSagaName(UUID.fromString(assessmentStudent.getAssessmentStudentID()), SagaEnum.PUBLISH_STUDENT_REGISTRATION.name());
        assertThat(sagas).isPresent();
    }

    @SneakyThrows
    @Test
    void test_findAndPublishLoadedStudentRecordsForPublishing_WithStatusCode_LOADED_ExistingSaga_shouldReturnOk() {

        SessionEntity session = sessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));

        AssessmentStudent student1 = createMockStudent();
        student1.setAssessmentID(assessment.getAssessmentID().toString());
        AssessmentStudentEntity studentEntity1 = mapper.toModel(student1);
        studentEntity1.setAssessmentStudentStatusCode(AssessmentStudentStatusCodes.LOADED.getCode());
        AssessmentStudent assessmentStudent = assessmentStudentService.createStudent(studentEntity1);

        var saga = AssessmentSagaEntity.builder()
                .updateDate(LocalDateTime.now().minusMinutes(15))
                .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .createDate(LocalDateTime.now().minusMinutes(15))
                .sagaName(SagaEnum.PUBLISH_STUDENT_REGISTRATION.name())
                .assessmentStudentID(UUID.fromString(assessmentStudent.getAssessmentStudentID()))
                .status(SagaStatusEnum.COMPLETED.name())
                .sagaState(EventType.MARK_SAGA_COMPLETE.name())
                .payload(JsonUtil.getJsonStringFromObject(assessmentStudent))
                .build();
        AssessmentSagaEntity sagaEntity = sagaRepository.save(saga);

        final Event event = Event.builder().eventType(EventType.PUBLISH_STUDENT_REGISTRATION_EVENT).eventOutcome(EventOutcome.STUDENT_REGISTRATION_EVENT_READ).eventPayload(JsonUtil.getJsonStringFromObject(assessmentStudent)).build();
        eventHandlerServiceUnderTest.handlePublishStudentRegistrationEvent(event);

        var sagas = sagaRepository.findById(sagaEntity.getSagaId());
        assertThat(sagas).isPresent();
        var sagaEvents = sagaEventRepository.findBySaga(sagaEntity);
        assertThat(sagaEvents).isEmpty();
    }

    @SneakyThrows
    @Test
    void test_findAndPublishLoadedStudentRecordsForPublishing_LOADED_shouldReturnOk() {
        SessionEntity session = sessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));

        AssessmentStudent student1 = createMockStudent();
        student1.setAssessmentID(assessment.getAssessmentID().toString());
        AssessmentStudentEntity studentEntity1 = mapper.toModel(student1);
        studentEntity1.setAssessmentStudentStatusCode(AssessmentStudentStatusCodes.LOADED.getCode());
        AssessmentStudent assessmentStudent = assessmentStudentService.createStudent(studentEntity1);

        eventTaskSchedulerAsyncService.findAndPublishLoadedStudentRegistrationsForProcessing();
        final Event event = Event.builder().eventType(EventType.PUBLISH_STUDENT_REGISTRATION_EVENT).eventOutcome(EventOutcome.STUDENT_REGISTRATION_EVENT_READ).eventPayload(JsonUtil.getJsonStringFromObject(assessmentStudent)).build();
        eventHandlerServiceUnderTest.handlePublishStudentRegistrationEvent(event);
        var sagas = sagaRepository.findByAssessmentStudentIDAndSagaName(UUID.fromString(assessmentStudent.getAssessmentStudentID()), SagaEnum.PUBLISH_STUDENT_REGISTRATION.name());
        assertThat(sagas).isPresent();
    }

    @Test
    void testHandleEvent_givenEventTypePROCESS_STUDENT_REGISTRATION__whenNoStudentExist_shouldHaveEventOutcome_CREATED() throws IOException {
        SessionEntity session = sessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
        AssessmentStudent student1 = createMockStudent();
        student1.setAssessmentID(assessment.getAssessmentID().toString());

        var sagaId = UUID.randomUUID();
        final Event event = Event.builder().eventType(EventType.PROCESS_STUDENT_REGISTRATION).sagaId(sagaId).eventPayload(JsonUtil.getJsonStringFromObject(student1)).build();
        var response = eventHandlerServiceUnderTest.handleProcessStudentRegistrationEvent(event);
        AssertionsForClassTypes.assertThat(response.getLeft()).isNotEmpty();
        Event responseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class, response.getLeft());
        AssertionsForClassTypes.assertThat(responseEvent).isNotNull();
        AssertionsForClassTypes.assertThat(responseEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_REGISTRATION_PROCESSED_IN_ASSESSMENT_API);
    }

    @Test
    @Transactional
    void test_setupSessionsForUpcomingSchoolYear_ShouldCreateSessionsAndAssessments() {
        List<AssessmentSessionCriteriaEntity> savedAssessmentSessionCriteriaEntities = assessmentSessionCriteriaRepository.saveAll(createMockAssessmentSessionCriteriaEntities());

        for (AssessmentSessionCriteriaEntity entity : savedAssessmentSessionCriteriaEntities) {
            AssessmentTypeCodeEntity savedAssessmentTypeCode = assessmentTypeCodeRepository.save(createMockAssessmentTypeCodeEntity("LTF12"));
            entity.setAssessmentCriteriaEntities(createMockAssessmentSessionTypeCodeCriteriaEntities(savedAssessmentSessionCriteriaEntities, savedAssessmentTypeCode));
            assessmentSessionCriteriaRepository.save(entity);
        }

        eventTaskSchedulerAsyncService.createSessionsForSchoolYear();

        int currentMonth = LocalDate.now().getMonthValue();
        int targetYear = currentMonth >= Month.SEPTEMBER.getValue() ? LocalDate.now().getYear() + 1 : LocalDate.now().getYear();

        List<SessionEntity> savedSessions = sessionRepository.findAll();
        assertThat(savedSessions)
            .hasSize(2)
            .anySatisfy(session -> {
                assertThat(session.getCourseMonth()).isEqualTo("11");
                assertThat(session.getCourseYear()).isEqualTo(String.valueOf(targetYear));
                assertThat(session.getAssessments())
                        .hasSize(1)
                        .extracting(AssessmentEntity::getAssessmentTypeCode)
                        .containsExactly("LTF12");
            })
            .anySatisfy(session -> {
                assertThat(session.getCourseMonth()).isEqualTo("06");
                assertThat(session.getCourseYear()).isEqualTo(String.valueOf(targetYear + 1));
            });
    }

}
