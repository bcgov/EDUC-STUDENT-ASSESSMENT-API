package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentStudentStatusCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.SessionEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.service.v1.events.RegistrationEventHandlerService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationEventHandlerServiceTest extends BaseAssessmentAPITest {

    private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;

    @Autowired
    AssessmentStudentService assessmentStudentService;
    @Autowired
    RegistrationEventHandlerService eventHandlerServiceUnderTest;
    @Autowired
    AssessmentStudentRepository assessmentStudentRepository;
    @Autowired
    AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    @Autowired
    SessionRepository sessionRepository;
    @Autowired
    AssessmentRepository assessmentRepository;
    @Autowired
    SagaRepository sagaRepository;
    @Autowired
    SagaEventRepository sagaEventRepository;

    @AfterEach
    public void after() {
        assessmentStudentRepository.deleteAll();
        assessmentStudentHistoryRepository.deleteAll();
        assessmentRepository.deleteAll();
        sessionRepository.deleteAll();
        sagaEventRepository.deleteAll();
        sagaRepository.deleteAll();
    }

    @Test
    void testHandleEvent_givenEventType_GET_OPEN_ASSESSMENT_Loaded_Publish() throws IOException {
        SessionEntity session = sessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));

        AssessmentStudent student1 = createMockStudent();
        student1.setAssessmentID(assessment.getAssessmentID().toString());
        AssessmentStudentEntity studentEntity1 = mapper.toModel(student1);
        studentEntity1.setAssessmentStudentStatusCode(AssessmentStudentStatusCodes.LOADED.getCode());
        AssessmentStudent assessmentStudent = assessmentStudentService.createStudent(studentEntity1);

        final Event event = Event.builder().eventType(EventType.PUBLISH_STUDENT_REGISTRATION_EVENT).eventOutcome(EventOutcome.STUDENT_REGISTRATION_EVENT_READ).eventPayload(JsonUtil.getJsonStringFromObject(assessmentStudent)).build();
        eventHandlerServiceUnderTest.handleEvent(event);
        var sagas = sagaRepository.findByAssessmentStudentIDAndSagaName(UUID.fromString(assessmentStudent.getAssessmentStudentID()), SagaEnum.PUBLISH_STUDENT_REGISTRATION.name());
        assertThat(sagas).isPresent();
    }

    @Test
    void testHandleEvent_givenEventType_INVALID_Loaded_Publish() throws IOException {
        SessionEntity session = sessionRepository.save(createMockSessionEntity());
        AssessmentEntity assessment = assessmentRepository.save(createMockAssessmentEntity(session, AssessmentTypeCodes.LTF12.getCode()));

        AssessmentStudent student1 = createMockStudent();
        student1.setAssessmentID(assessment.getAssessmentID().toString());
        AssessmentStudentEntity studentEntity1 = mapper.toModel(student1);
        studentEntity1.setAssessmentStudentStatusCode(AssessmentStudentStatusCodes.LOADED.getCode());
        AssessmentStudent assessmentStudent = assessmentStudentService.createStudent(studentEntity1);
        final Event event = Event.builder().eventType(EventType.GET_OPEN_ASSESSMENT_SESSIONS).eventOutcome(EventOutcome.STUDENT_REGISTRATION_EVENT_READ).eventPayload(JsonUtil.getJsonStringFromObject(assessmentStudent)).build();
        eventHandlerServiceUnderTest.handleEvent(event);
        var sagas = sagaRepository.findByAssessmentStudentIDAndSagaName(UUID.fromString(assessmentStudent.getAssessmentStudentID()), SagaEnum.PUBLISH_STUDENT_REGISTRATION.name());
        assertThat(sagas).isEmpty();
    }

    @Test
    void testHandleEvent_TopicName_verification() {
        assertThat(eventHandlerServiceUnderTest.getTopicToSubscribe()).isEqualTo(TopicsEnum.PUBLISH_STUDENT_REGISTRATION_TOPIC.name());
    }

}
