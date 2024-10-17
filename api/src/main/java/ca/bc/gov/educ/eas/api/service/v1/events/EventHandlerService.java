package ca.bc.gov.educ.eas.api.service.v1.events;

import ca.bc.gov.educ.eas.api.constants.EventOutcome;
import ca.bc.gov.educ.eas.api.constants.EventType;
import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.model.v1.EasEventEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.EasEventRepository;
import ca.bc.gov.educ.eas.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.eas.api.struct.Event;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudentGet;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import ca.bc.gov.educ.eas.api.util.RequestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
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

  private final AssessmentStudentRepository assessmentStudentRepository;

  private final AssessmentStudentService assessmentStudentService;

  private static final AssessmentStudentMapper assessmentStudentMapper = AssessmentStudentMapper.mapper;

  private final EasEventRepository easEventRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Pair<byte[], EasEventEntity> handleCreateStudentRegistrationEvent(Event event) throws JsonProcessingException {
    val studentEventOptional = easEventRepository.findBySagaIdAndEventType(event.getSagaId(), event.getEventType().toString());
    EasEventEntity easEvent;
    EasEventEntity choreographyEvent = null;
    if (studentEventOptional.isEmpty()) {
      log.info(NO_RECORD_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      AssessmentStudent student = JsonUtil.getJsonObjectFromString(AssessmentStudent.class, event.getEventPayload());
      val optionalStudent = assessmentStudentRepository.findByAssessmentIDAndStudentID(UUID.fromString(student.getAssessmentID()), student.getStudentID());
      if (optionalStudent.isPresent()) {
        event.setEventOutcome(EventOutcome.STUDENT_ALREADY_EXIST);
        event.setEventPayload(optionalStudent.get().getStudentID().toString()); // return the student ID in response.
      } else {
        RequestUtil.setAuditColumnsForCreate(student);
        var studentPair = assessmentStudentService.createStudent(assessmentStudentMapper.toModel(student));
        AssessmentStudentEntity entity = studentPair.getLeft();
        choreographyEvent = studentPair.getRight();
        event.setEventOutcome(EventOutcome.STUDENT_CREATED);
        event.setEventPayload(JsonUtil.getJsonStringFromObject(studentMapper.toStructure(entity)));// need to convert to structure MANDATORY otherwise jackson will break.
      }
      easEvent = createAssessmentStudentEventRecord(event);
    } else {
      log.info(RECORD_FOUND_FOR_SAGA_ID_EVENT_TYPE);
      log.trace(EVENT_PAYLOAD, event);
      easEvent = studentEventOptional.get();
      easEvent.setUpdateDate(LocalDateTime.now());
    }

    easEventRepository.save(easEvent);
    return Pair.of(createResponseEvent(easEvent), choreographyEvent);
  }


  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public byte[] handleGetStudentRegistrationEvent(Event event, boolean isSynchronous) throws JsonProcessingException {
    AssessmentStudentGet student = JsonUtil.getJsonObjectFromString(AssessmentStudentGet.class, event.getEventPayload());
    if (isSynchronous) {
      val optionalStudentEntity = assessmentStudentRepository.findByAssessmentIDAndStudentID(UUID.fromString(student.getAssessmentID()), student.getStudentID());
      if (optionalStudentEntity.isPresent()) {
        return JsonUtil.getJsonBytesFromObject(assessmentStudentMapper.toStructure(optionalStudentEntity.get()));
      } else {
        return new byte[0];
      }
    }

    log.trace(EVENT_PAYLOAD, event);
    val optionalStudentEntity = assessmentStudentRepository.findByAssessmentIDAndStudentID(UUID.fromString(student.getAssessmentID()), student.getStudentID());
    if (optionalStudentEntity.isPresent()) {
      AssessmentStudent structStud = assessmentStudentMapper.toStructure(optionalStudentEntity.get()); // need to convert to structure MANDATORY otherwise jackson will break.
      event.setEventPayload(JsonUtil.getJsonStringFromObject(structStud));
      event.setEventOutcome(EventOutcome.STUDENT_REGISTRATION_FOUND);
    } else {
      event.setEventOutcome(EventOutcome.STUDENT_REGISTRATION_NOT_FOUND);
    }
    val studentEvent = createAssessmentStudentEventRecord(event);
    return createResponseEvent(studentEvent);
  }

  private EasEventEntity createAssessmentStudentEventRecord(Event event) {
    return EasEventEntity.builder()
        .createDate(LocalDateTime.now())
        .updateDate(LocalDateTime.now())
        .createUser(event.getEventType().toString()) //need to discuss what to put here.
        .updateUser(event.getEventType().toString())
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
