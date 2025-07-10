package ca.bc.gov.educ.assessment.api.service.v1.events;

import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.v1.NumeracyAssessmentCodes;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.mappers.v1.SessionMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentService;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentDetailResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentGet;
import ca.bc.gov.educ.assessment.api.util.EventUtil;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import ca.bc.gov.educ.assessment.api.util.RequestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventStatus.MESSAGE_PUBLISHED;
import static ca.bc.gov.educ.assessment.api.constants.EventType.ASSESSMENT_STUDENT_UPDATE;

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
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentEventRepository assessmentEventRepository;
    private final AssessmentStudentService assessmentStudentService;
    private final AssessmentService assessmentService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<byte[], AssessmentEventEntity> handleProcessStudentRegistrationEvent(Event event) throws JsonProcessingException {
        final AssessmentStudent assessmentStudent = JsonUtil.getJsonObjectFromString(AssessmentStudent.class, event.getEventPayload());
        Optional<AssessmentStudentEntity> student = assessmentStudentService.getStudentByAssessmentIDAndStudentID(UUID.fromString(assessmentStudent.getAssessmentID()), UUID.fromString(assessmentStudent.getStudentID()));
        log.debug("handleCreateStudentRegistrationEvent found student :: {}", student);

        var isWithdrawal = StringUtils.isNotBlank(assessmentStudent.getCourseStatusCode()) && assessmentStudent.getCourseStatusCode().equalsIgnoreCase("W");

        boolean dataChangedForStudent = false;
        if(isWithdrawal && student.isEmpty()){
            log.error("Student withdrawal record submitted but no registration record is present; ignoring message to remove record");
        } else if (isWithdrawal) {
            log.debug("Removing student registration due to incoming withdrawal record :: student ID: {}", student.get().getStudentID());
            assessmentStudentService.deleteStudent(student.get().getAssessmentStudentID());
            dataChangedForStudent = true;
        } else if (student.isEmpty()) {
            log.debug("handleCreateStudentRegistrationEvent setting audit columns :: {}", student);
            RequestUtil.setAuditColumnsForCreate(assessmentStudent);
            AssessmentStudentEntity createStudentEntity = assessmentStudentMapper.toModel(assessmentStudent);
            var attempts = assessmentStudentService.getNumberOfAttempts(createStudentEntity.getAssessmentEntity().getAssessmentID().toString(), createStudentEntity.getStudentID());
            createStudentEntity.setNumberOfAttempts(Integer.parseInt(attempts));
            log.debug("Writing student entity: " + createStudentEntity);
            assessmentStudentService.saveAssessmentStudentWithHistory(createStudentEntity);
            dataChangedForStudent = true;
        } else {
            log.info("Student already exists in assessment {} ", assessmentStudent);
        }

        AssessmentEventEntity assessmentEventEntity = null;
        if(dataChangedForStudent) {
            assessmentEventEntity = EventUtil.createEvent(
                    assessmentStudent.getUpdateUser(), assessmentStudent.getUpdateUser(),
                    JsonUtil.getJsonStringFromObject(assessmentStudent.getStudentID()),
                    ASSESSMENT_STUDENT_UPDATE, EventOutcome.ASSESSMENT_STUDENT_UPDATED);

            log.debug("Assessment event is: {}", assessmentEventEntity);
            log.debug("Assessment student is: {}", assessmentStudent);
            assessmentEventRepository.save(assessmentEventEntity);
        }

        event.setEventOutcome(EventOutcome.STUDENT_REGISTRATION_PROCESSED_IN_ASSESSMENT_API);
        val studentEvent = createEventRecord(event);
        return Pair.of(createResponseEvent(studentEvent), assessmentEventEntity);
    }

    public byte[] handleGetOpenAssessmentSessionsEvent(Event event, boolean isSynchronous) throws JsonProcessingException {
        var currentDate = LocalDateTime.now();
        val sessions = assessmentSessionRepository.findAllByActiveFromDateLessThanEqualAndActiveUntilDateGreaterThanEqual(currentDate, currentDate);
        var sessionStructs = new ArrayList<>();
        sessions.forEach(sessionStruct -> sessionStructs.add(sessionMapper.toStructure(sessionStruct)));
        if (isSynchronous) {
            return JsonUtil.getJsonBytesFromObject(sessionStructs);
        }

        log.trace(EVENT_PAYLOAD, event);
        event.setEventPayload(JsonUtil.getJsonStringFromObject(sessionStructs));
        event.setEventOutcome(EventOutcome.SESSIONS_FOUND);
        val studentEvent = createEventRecord(event);
        return createResponseEvent(studentEvent);
    }

    public byte[] handleGetStudentAssessmentDetailEvent(Event event) throws JsonProcessingException {
        AssessmentStudentGet student = JsonUtil.getJsonObjectFromString(AssessmentStudentGet.class, event.getEventPayload());
        var response = new AssessmentStudentDetailResponse();

        try {
            val assessment = assessmentService.getAssessment(UUID.fromString(student.getAssessmentID()));
            if (NumeracyAssessmentCodes.getAllCodes().stream().anyMatch(code -> code.equalsIgnoreCase(assessment.getAssessmentTypeCode()))) {
                List<UUID> assessmentIDs = new ArrayList<>();
                assessment.getAssessmentSessionEntity().getAssessments().stream()
                        .filter(a -> NumeracyAssessmentCodes.getAllCodes().stream().anyMatch(code -> code.equalsIgnoreCase(a.getAssessmentTypeCode())))
                        .forEach(a -> assessmentIDs.add(a.getAssessmentID()));
                val studentEntityList = assessmentStudentService.getStudentsByAssessmentIDsInAndStudentID(assessmentIDs, UUID.fromString(student.getStudentID()));
                if (!studentEntityList.isEmpty()) {
                    response.setHasPriorRegistration(true);

                    for (var stud : studentEntityList) {
                        if (stud.getProficiencyScore() != null || StringUtils.isNotBlank(stud.getProvincialSpecialCaseCode())) {
                            response.setAlreadyWrittenAssessment(true);
                            break;
                        }
                    }
                }

                val numberOfAttempts = assessmentStudentService.getNumberOfAttempts(student.getAssessmentID(), UUID.fromString(student.getStudentID()));
                response.setNumberOfAttempts(numberOfAttempts);
                return JsonUtil.getJsonBytesFromObject(response);
            }
        } catch (EntityNotFoundException e) {
            log.debug("No assessment found for assessment id :: {} in handleGetStudentAssessmentDetailEvent", student.getAssessmentID());
        }

        val optionalStudentEntity = assessmentStudentService.getStudentByAssessmentIDAndStudentID(UUID.fromString(student.getAssessmentID()), UUID.fromString(student.getStudentID()));

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

    private AssessmentEventEntity createEventRecord(Event event) {
        return AssessmentEventEntity.builder()
                .createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now())
                .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
                .eventPayload(event.getEventPayload())
                .eventType(event.getEventType().toString())
                .sagaId(event.getSagaId())
                .eventStatus(MESSAGE_PUBLISHED.toString())
                .eventOutcome(event.getEventOutcome().toString())
                .replyChannel(event.getReplyTo())
                .build();
    }

    private byte[] createResponseEvent(AssessmentEventEntity event) throws JsonProcessingException {
        val responseEvent = Event.builder()
                .sagaId(event.getSagaId())
                .eventType(EventType.valueOf(event.getEventType()))
                .eventOutcome(EventOutcome.valueOf(event.getEventOutcome()))
                .eventPayload(event.getEventPayload()).build();
        return JsonUtil.getJsonBytesFromObject(responseEvent);
    }

}
