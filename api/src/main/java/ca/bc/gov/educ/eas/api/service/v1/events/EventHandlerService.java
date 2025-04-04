package ca.bc.gov.educ.eas.api.service.v1.events;

import ca.bc.gov.educ.eas.api.constants.EventOutcome;
import ca.bc.gov.educ.eas.api.constants.EventType;
import ca.bc.gov.educ.eas.api.constants.SagaEnum;
import ca.bc.gov.educ.eas.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.eas.api.constants.v1.AssessmentStudentStatusCodes;
import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.eas.api.mappers.v1.SessionMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.model.v1.EasEventEntity;
import ca.bc.gov.educ.eas.api.orchestrator.StudentRegistrationOrchestrator;
import ca.bc.gov.educ.eas.api.properties.ApplicationProperties;
import ca.bc.gov.educ.eas.api.repository.v1.SessionRepository;
import ca.bc.gov.educ.eas.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.eas.api.service.v1.SagaService;
import ca.bc.gov.educ.eas.api.struct.Event;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudentDetailResponse;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudentGet;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import ca.bc.gov.educ.eas.api.util.RequestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.eas.api.constants.EventStatus.MESSAGE_PUBLISHED;

/**
 * The type Event handler service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("java:S3864")
public class EventHandlerService {

    /**
     * The constant NO_RECORD_SAGA_ID_EVENT_TYPE.
     */
    public static final String NO_RECORD_SAGA_ID_EVENT_TYPE = "no record found for the saga id and event type combination, processing.";
    /**
     * The constant RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE.
     */
    public static final String RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE = "record found for the saga id and event type combination, might be a duplicate or replay," +
            " just updating the db status so that it will be polled and sent back again.";
    /**
     * The constant PAYLOAD_LOG.
     */
    public static final String PAYLOAD_LOG = "payload is :: {}";
    /**
     * The constant EVENT_PAYLOAD.
     */
    public static final String EVENT_PAYLOAD = "event is :: {}";
    private static final AssessmentStudentMapper assessmentStudentMapper = AssessmentStudentMapper.mapper;
    private static final SessionMapper sessionMapper = SessionMapper.mapper;
    private final SessionRepository sessionRepository;
    private final AssessmentStudentService assessmentStudentService;
    private final StudentRegistrationOrchestrator studentRegistrationOrchestrator;
    private final SagaService sagaService;

    public byte[] handleCreateStudentRegistrationEvent(Event event) throws JsonProcessingException {
        final AssessmentStudent assessmentStudent = JsonUtil.getJsonObjectFromString(AssessmentStudent.class, event.getEventPayload());
        Optional<AssessmentStudentEntity> student = assessmentStudentService.getStudentByAssessmentIDAndStudentID(UUID.fromString(assessmentStudent.getAssessmentID()), UUID.fromString(assessmentStudent.getStudentID()));
        if (student.isEmpty()) {
            RequestUtil.setAuditColumnsForCreate(assessmentStudent);
            AssessmentStudentEntity createStudentEntity = assessmentStudentMapper.toModel(assessmentStudent);
            createStudentEntity.setAssessmentStudentStatusCode(AssessmentStudentStatusCodes.LOADED.getCode());
            var attempts = assessmentStudentService.getNumberOfAttempts(createStudentEntity.getAssessmentEntity().getAssessmentID().toString(), createStudentEntity.getStudentID());
            createStudentEntity.setNumberOfAttempts(Integer.parseInt(attempts));
            log.info("Writing student entity: " + createStudentEntity);
            assessmentStudentService.createStudentWithoutValidation(createStudentEntity);
            event.setEventOutcome(EventOutcome.STUDENT_REGISTRATION_CREATED);
        } else {
            log.info("Student already exists in assessment {} ", assessmentStudent.getAssessmentStudentID());
            event.setEventOutcome(EventOutcome.STUDENT_ALREADY_EXIST);
        }
        val studentEvent = createEventRecord(event);
        return createResponseEvent(studentEvent);
    }

    public byte[] handleGetOpenAssessmentSessionsEvent(Event event, boolean isSynchronous) throws JsonProcessingException {
        var currentDate = LocalDateTime.now();
        val sessions = sessionRepository.findAllByActiveFromDateLessThanEqualAndActiveUntilDateGreaterThanEqual(currentDate, currentDate);
        var sessionStructs = new ArrayList<>();
        sessions.forEach(sessionStruct -> sessionStructs.add(sessionMapper.toStructure(sessionStruct)));
        if (isSynchronous) {
            if (!sessions.isEmpty()) {
                return JsonUtil.getJsonBytesFromObject(sessionStructs);
            } else {
                return new byte[0];
            }
        }

        log.trace(EVENT_PAYLOAD, event);
        event.setEventPayload(JsonUtil.getJsonStringFromObject(sessionStructs));
        event.setEventOutcome(EventOutcome.SESSIONS_FOUND);
        val studentEvent = createEventRecord(event);
        return createResponseEvent(studentEvent);
    }

    public byte[] handleGetStudentAssessmentDetailEvent(Event event) throws JsonProcessingException {
        AssessmentStudentGet student = JsonUtil.getJsonObjectFromString(AssessmentStudentGet.class, event.getEventPayload());
        val optionalStudentEntity = assessmentStudentService.getStudentByAssessmentIDAndStudentID(UUID.fromString(student.getAssessmentID()), UUID.fromString(student.getStudentID()));
        var response = new AssessmentStudentDetailResponse();

        if(optionalStudentEntity.isPresent()){
            response.setHasPriorRegistration(true);

            var stud = optionalStudentEntity.get();
            if(stud.getProficiencyScore() != null || StringUtils.isNotBlank(stud.getProvincialSpecialCaseCode())){
                response.setAlreadyWrittenAssessment(true);
            }
        }

        val numberOfAttempts = assessmentStudentService.getNumberOfAttempts(student.getAssessmentID(), UUID.fromString(student.getStudentID()));
        response.setNumberOfAttempts(numberOfAttempts);
        return JsonUtil.getJsonBytesFromObject(response);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePublishStudentRegistrationEvent(final Event event) throws JsonProcessingException {
        if (event.getEventOutcome() == EventOutcome.STUDENT_REGISTRATION_EVENT_READ) {
            final AssessmentStudent sagaData = JsonUtil.getJsonObjectFromString(AssessmentStudent.class, event.getEventPayload());
            final var sagaOptional = sagaService.findByAssessmentStudentIDAndSagaNameAndStatusNot(UUID.fromString(sagaData.getAssessmentStudentID()), SagaEnum.PUBLISH_STUDENT_REGISTRATION.toString(), SagaStatusEnum.COMPLETED.toString());
            if (sagaOptional.isPresent()) { // possible duplicate message.
                log.trace("Execution is not required for this message returning EVENT is :: {}", event);
                return;
            }
            val saga = this.studentRegistrationOrchestrator.createSaga(event.getEventPayload(), sagaData.getUpdateUser(), UUID.fromString(sagaData.getAssessmentStudentID()));
            log.debug("Starting student processing orchestrator :: {}", saga);
            this.studentRegistrationOrchestrator.startSaga(saga);
        }
    }

    private EasEventEntity createEventRecord(Event event) {
        return EasEventEntity.builder()
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .createUser(ApplicationProperties.EAS_API)
                .updateUser(ApplicationProperties.EAS_API)
                .eventPayloadBytes(event.getEventPayload().getBytes(StandardCharsets.UTF_8))
                .eventType(event.getEventType().toString())
                .sagaId(event.getSagaId())
                .eventStatus(MESSAGE_PUBLISHED.toString())
                .eventOutcome(event.getEventOutcome().toString())
                .replyChannel(event.getReplyTo())
                .build();
    }

    private byte[] createResponseEvent(EasEventEntity event) throws JsonProcessingException {
        val responseEvent = Event.builder()
                .sagaId(event.getSagaId())
                .eventType(EventType.valueOf(event.getEventType()))
                .eventOutcome(EventOutcome.valueOf(event.getEventOutcome()))
                .eventPayload(event.getEventPayload()).build();
        return JsonUtil.getJsonBytesFromObject(responseEvent);
    }

}
