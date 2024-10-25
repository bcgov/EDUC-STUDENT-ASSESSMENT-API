package ca.bc.gov.educ.eas.api.schedulers;

import ca.bc.gov.educ.eas.api.BaseEasAPITest;
import ca.bc.gov.educ.eas.api.constants.EventOutcome;
import ca.bc.gov.educ.eas.api.constants.EventType;
import ca.bc.gov.educ.eas.api.constants.SagaEnum;
import ca.bc.gov.educ.eas.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.eas.api.constants.v1.AssessmentStudentStatusCodes;
import ca.bc.gov.educ.eas.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.eas.api.messaging.MessagePublisher;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.model.v1.EasSagaEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.*;
import ca.bc.gov.educ.eas.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.eas.api.service.v1.SagaService;
import ca.bc.gov.educ.eas.api.service.v1.events.EventHandlerService;
import ca.bc.gov.educ.eas.api.service.v1.events.schedulers.EventTaskSchedulerAsyncService;
import ca.bc.gov.educ.eas.api.struct.Event;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import io.nats.client.Connection;
import lombok.SneakyThrows;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class EventTaskSchedulerTest extends BaseEasAPITest {

    private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;

    @Autowired
    EventTaskSchedulerAsyncService eventTaskSchedulerAsyncService;
    @Autowired
    SagaService sagaService;
    @Autowired
    AssessmentStudentService assessmentStudentService;
    @Autowired
    EventHandlerService eventHandlerServiceUnderTest;
    @Autowired
    SagaRepository sagaRepository;
    @Autowired
    SagaEventRepository sagaEventRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    AssessmentStudentRepository assessmentStudentRepository;
    @Autowired
    AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    @Autowired
    MessagePublisher messagePublisher;
    @Captor
    ArgumentCaptor<byte[]> eventCaptor;
    @Autowired
    Connection connection;

    @AfterEach
    public void after() {
        assessmentStudentRepository.deleteAll();
        assessmentStudentHistoryRepository.deleteAll();
        assessmentRepository.deleteAll();
        sessionRepository.deleteAll();
        sagaEventRepository.deleteAll();
        sagaRepository.deleteAll();
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
        AssessmentStudentEntity assessmentStudentEntity = assessmentStudentService.createStudent(studentEntity1);

        final Event event = Event.builder().eventType(EventType.PUBLISH_STUDENT_REGISTRATION_EVENT).eventOutcome(EventOutcome.STUDENT_REGISTRATION_EVENT_READ).eventPayload(JsonUtil.getJsonStringFromObject(mapper.toStructure(assessmentStudentEntity))).build();
        eventHandlerServiceUnderTest.handlePublishStudentRegistrationEvent(event);
        var sagas = sagaRepository.findByAssessmentStudentIDAndSagaName(assessmentStudentEntity.getAssessmentStudentID(), SagaEnum.PUBLISH_STUDENT_REGISTRATION.name());
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
        AssessmentStudentEntity assessmentStudentEntity = assessmentStudentService.createStudent(studentEntity1);

        var saga = EasSagaEntity.builder()
                .updateDate(LocalDateTime.now().minusMinutes(15))
                .createUser("EAS_API")
                .updateUser("EAS_API")
                .createDate(LocalDateTime.now().minusMinutes(15))
                .sagaName(SagaEnum.PUBLISH_STUDENT_REGISTRATION.name())
                .assessmentStudentID(assessmentStudentEntity.getAssessmentStudentID())
                .status(SagaStatusEnum.COMPLETED.name())
                .sagaState(EventType.MARK_SAGA_COMPLETE.name())
                .payload(JsonUtil.getJsonStringFromObject(mapper.toStructure(assessmentStudentEntity)))
                .build();
        EasSagaEntity sagaEntity = sagaRepository.save(saga);

        final Event event = Event.builder().eventType(EventType.PUBLISH_STUDENT_REGISTRATION_EVENT).eventOutcome(EventOutcome.STUDENT_REGISTRATION_EVENT_READ).eventPayload(JsonUtil.getJsonStringFromObject(mapper.toStructure(assessmentStudentEntity))).build();
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
        AssessmentStudentEntity assessmentStudentEntity = assessmentStudentService.createStudent(studentEntity1);

        eventTaskSchedulerAsyncService.findAndPublishLoadedStudentRegistrationsForProcessing();
        final Event event = Event.builder().eventType(EventType.PUBLISH_STUDENT_REGISTRATION_EVENT).eventOutcome(EventOutcome.STUDENT_REGISTRATION_EVENT_READ).eventPayload(JsonUtil.getJsonStringFromObject(mapper.toStructure(assessmentStudentEntity))).build();
        eventHandlerServiceUnderTest.handlePublishStudentRegistrationEvent(event);
        var sagas = sagaRepository.findByAssessmentStudentIDAndSagaName(assessmentStudentEntity.getAssessmentStudentID(), SagaEnum.PUBLISH_STUDENT_REGISTRATION.name());
        assertThat(sagas).isPresent();
    }

    @Test
    void testHandleEvent_givenEventTypeCREATE_STUDENT_REGISTRATION__whenNoStudentExist_shouldHaveEventOutcome_CREATED() throws IOException {
        SessionEntity session = sessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));
        AssessmentStudent student1 = createMockStudent();
        student1.setAssessmentID(assessment.getAssessmentID().toString());

        var sagaId = UUID.randomUUID();
        final Event event = Event.builder().eventType(EventType.CREATE_STUDENT_REGISTRATION).sagaId(sagaId).eventPayload(JsonUtil.getJsonStringFromObject(student1)).build();
        byte[] response = eventHandlerServiceUnderTest.handleCreateStudentRegistrationEvent(event);
        AssertionsForClassTypes.assertThat(response).isNotEmpty();
        Event responseEvent = JsonUtil.getJsonObjectFromByteArray(Event.class, response);
        AssertionsForClassTypes.assertThat(responseEvent).isNotNull();
        AssertionsForClassTypes.assertThat(responseEvent.getEventOutcome()).isEqualTo(EventOutcome.STUDENT_REGISTRATION_CREATED);
    }

}
