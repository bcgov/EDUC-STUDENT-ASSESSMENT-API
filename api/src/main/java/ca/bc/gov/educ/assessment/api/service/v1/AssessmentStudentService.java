package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventStatus;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentHistoryRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentRulesProcessor;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentValidationIssue;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentRuleData;
import ca.bc.gov.educ.assessment.api.util.EventUtil;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import ca.bc.gov.educ.assessment.api.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.ASSESSMENT_STUDENT_UPDATED;
import static ca.bc.gov.educ.assessment.api.constants.EventType.ASSESSMENT_STUDENT_UPDATE;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentStudentService {

    private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final AssessmentEventRepository assessmentEventRepository;
    private final AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    private final AssessmentStudentHistoryService assessmentStudentHistoryService;
    private final MessagePublisher messagePublisher;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentStudentRulesProcessor assessmentStudentRulesProcessor;
    private final RestUtils restUtils;

    public AssessmentStudentEntity getStudentByID(UUID assessmentStudentID) {
        return assessmentStudentRepository.findById(assessmentStudentID).orElseThrow(() ->
                new EntityNotFoundException(AssessmentStudent.class, "assessmentStudentID", assessmentStudentID.toString())
        );
    }

    public Optional<AssessmentStudentEntity> getStudentByAssessmentIDAndStudentID(UUID assessmentID, UUID studentID) {
        return assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(assessmentID, studentID);
    }

    public List<AssessmentStudentEntity> getStudentsByAssessmentIDsInAndStudentID(List<UUID> assessmentIDs, UUID studentID) {
        return assessmentStudentRepository.findByAssessmentEntity_AssessmentIDInAndStudentID(assessmentIDs, studentID);
    }

    public String getNumberOfAttempts(String assessmentID, UUID studentID) {
        var assessment = assessmentRepository.findById(UUID.fromString(assessmentID)).orElseThrow(() ->
                new EntityNotFoundException(AssessmentEntity.class, "Assessment", assessmentID));

        return Integer.toString(assessmentStudentRepository.findNumberOfAttemptsForStudent(studentID, getAssessmentTypeCodeList(assessment.getAssessmentTypeCode())));
    }

    private List<String> getAssessmentTypeCodeList(String assessmentTypeCode){
        if(assessmentTypeCode.startsWith("NM")){
            return Arrays.asList(AssessmentTypeCodes.NME.getCode(), AssessmentTypeCodes.NME10.getCode(), AssessmentTypeCodes.NMF.getCode(), AssessmentTypeCodes.NMF10.getCode());
        }
        return Arrays.asList(assessmentTypeCode);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<AssessmentStudent, AssessmentEventEntity> updateStudent(AssessmentStudentEntity assessmentStudentEntity) throws JsonProcessingException {
        AssessmentStudentEntity currentAssessmentStudentEntity = assessmentStudentRepository.findById(assessmentStudentEntity.getAssessmentStudentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentStudentEntity.class, "AssessmentStudent", assessmentStudentEntity.getAssessmentStudentID().toString())
        );

        var student = processStudent(assessmentStudentEntity, currentAssessmentStudentEntity);

        final AssessmentEventEntity event = EventUtil.createEvent(
                student.getUpdateUser(), student.getUpdateUser(),
                JsonUtil.getJsonStringFromObject(student),
                ASSESSMENT_STUDENT_UPDATE, ASSESSMENT_STUDENT_UPDATED);
        assessmentEventRepository.save(event);

        return Pair.of(student, event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentStudentEntity createStudentWithoutValidation(AssessmentStudentEntity assessmentStudentEntity) {
        return createAssessmentStudentWithHistory(assessmentStudentEntity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<AssessmentStudent, AssessmentEventEntity> createStudent(AssessmentStudentEntity assessmentStudentEntity) throws JsonProcessingException {
        var student = processStudent(assessmentStudentEntity, null);
        final AssessmentEventEntity event = EventUtil.createEvent(
                student.getUpdateUser(), student.getUpdateUser(),
                JsonUtil.getJsonStringFromObject(student),
                ASSESSMENT_STUDENT_UPDATE, ASSESSMENT_STUDENT_UPDATED);
        assessmentEventRepository.save(event);

        return Pair.of(student, event);
    }

    private AssessmentStudent processStudent(AssessmentStudentEntity assessmentStudentEntity, AssessmentStudentEntity currentAssessmentStudentEntity) {
        SchoolTombstone schoolTombstone = restUtils.getSchoolBySchoolID(assessmentStudentEntity.getSchoolOfRecordSchoolID().toString()).orElse(null);

        UUID studentCorrelationID = UUID.randomUUID();
        log.info("Retrieving student record for PEN ::{} with correlationID :: {}", assessmentStudentEntity.getPen(), studentCorrelationID);
        Student studentApiStudent = restUtils.getStudentByPEN(studentCorrelationID, assessmentStudentEntity.getPen()).orElseThrow(() ->
                new EntityNotFoundException(Student.class, "Student", assessmentStudentEntity.getPen()));

        List<AssessmentStudentValidationIssue> validationIssues = runValidationRules(assessmentStudentEntity, schoolTombstone, studentApiStudent);

        if (validationIssues.isEmpty()) {
            if (currentAssessmentStudentEntity != null) {
                BeanUtils.copyProperties(assessmentStudentEntity, currentAssessmentStudentEntity, "districtID", "schoolID", "studentID", "givenName", "surName", "pen", "localID", "courseStatusCode", "createUser", "createDate");
                TransformUtil.uppercaseFields(currentAssessmentStudentEntity);
                currentAssessmentStudentEntity.setNumberOfAttempts(Integer.parseInt(getNumberOfAttempts(currentAssessmentStudentEntity.getAssessmentEntity().getAssessmentID().toString(), currentAssessmentStudentEntity.getStudentID())));
                return mapper.toStructure(createAssessmentStudentWithHistory(currentAssessmentStudentEntity));
            } else {
                assessmentStudentEntity.setStudentID(UUID.fromString(studentApiStudent.getStudentID()));
                assessmentStudentEntity.setNumberOfAttempts(Integer.parseInt(getNumberOfAttempts(assessmentStudentEntity.getAssessmentEntity().getAssessmentID().toString(), assessmentStudentEntity.getStudentID())));
                return mapper.toStructure(createAssessmentStudentWithHistory(assessmentStudentEntity));
            }
        }

        AssessmentStudent studentWithValidationIssues = mapper.toStructure(assessmentStudentEntity);
        studentWithValidationIssues.setAssessmentStudentValidationIssues(validationIssues);

        return studentWithValidationIssues;
    }

    public AssessmentStudentEntity createAssessmentStudentWithHistory(AssessmentStudentEntity assessmentStudentEntity) {
        AssessmentStudentEntity savedEntity = assessmentStudentRepository.save(assessmentStudentEntity);
        assessmentStudentHistoryRepository.save(this.assessmentStudentHistoryService.createAssessmentStudentHistoryEntity(assessmentStudentEntity, assessmentStudentEntity.getUpdateUser()));
        return savedEntity;
    }

    public List<AssessmentStudentValidationIssue> runValidationRules(AssessmentStudentEntity assessmentStudentEntity, SchoolTombstone schoolTombstone, Student studentApiStudent) {
        StudentRuleData studentRuleData = new StudentRuleData();
        studentRuleData.setAssessmentStudentEntity(assessmentStudentEntity);
        studentRuleData.setSchool(schoolTombstone);
        studentRuleData.setStudentApiStudent(studentApiStudent);

        return this.assessmentStudentRulesProcessor.processRules(studentRuleData);
    }

    @Async("publisherExecutor")
    public void prepareAndPublishStudentRegistration(final List<AssessmentStudentEntity> studentEntities) {
        studentEntities.stream()
                .forEach(el -> this.sendIndividualStudentAsMessageToTopic(mapper.toStructure(el)));
    }

    /**
     * Send individual student as message to topic consumer.
     */
    private void sendIndividualStudentAsMessageToTopic(final AssessmentStudent assessmentStudent) {
        final var eventPayload = JsonUtil.getJsonString(assessmentStudent);
        if (eventPayload.isPresent()) {
            final Event event = Event.builder().eventType(EventType.PUBLISH_STUDENT_REGISTRATION_EVENT).eventOutcome(EventOutcome.STUDENT_REGISTRATION_EVENT_READ).eventPayload(eventPayload.get()).build();
            final var eventString = JsonUtil.getJsonString(event);
            if (eventString.isPresent()) {
                this.messagePublisher.dispatchMessage(TopicsEnum.PUBLISH_STUDENT_REGISTRATION_TOPIC.toString(), eventString.get().getBytes());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<AssessmentEventEntity> deleteStudents(List<UUID> assessmentStudentIDs) throws JsonProcessingException {
        ArrayList<AssessmentEventEntity> events = new ArrayList<>();
        for (UUID assessmentStudentID : assessmentStudentIDs) {
            try {
                AssessmentStudentEntity deletedStudent = performDeleteStudent(assessmentStudentID);
                AssessmentEventEntity event = assessmentEventRepository.save(generateStudentDeletedEvent(deletedStudent));
                events.add(event);
            } catch (EntityNotFoundException | InvalidPayloadException e) {
                throw new InvalidPayloadException(ApiError.builder().timestamp(LocalDateTime.now()).message(e.getMessage()).status(CONFLICT).build());
            }
        }
        return events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentEventEntity deleteStudent(UUID assessmentStudentID) throws JsonProcessingException {
        AssessmentStudentEntity deletedStudent = performDeleteStudent(assessmentStudentID);
        return assessmentEventRepository.save(generateStudentDeletedEvent(deletedStudent));
    }

    private AssessmentStudentEntity performDeleteStudent(UUID assessmentStudentID) {
        Optional<AssessmentStudentEntity> entityOptional = assessmentStudentRepository.findById(assessmentStudentID);
        AssessmentStudentEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(AssessmentStudentEntity.class, "assessmentStudentID", assessmentStudentID.toString()));

        LocalDateTime sessionEnd = entity.getAssessmentEntity().getAssessmentSessionEntity().getActiveUntilDate();
        boolean sessionEnded = sessionEnd.isBefore(LocalDateTime.now());
        boolean hasResult = entity.getProficiencyScore() != null || entity.getProvincialSpecialCaseCode() != null;

        if (sessionEnded || hasResult) {
            throw new InvalidPayloadException(ApiError.builder().timestamp(LocalDateTime.now()).message("Cannot delete student. Reason: %s %s".formatted(sessionEnded ? "Session has ended. " : "", hasResult ? "Student has a proficiency score." : "").trim()).status(CONFLICT).build());
        }

        assessmentStudentHistoryRepository.deleteAllByAssessmentIDAndAssessmentStudentID(entity.getAssessmentEntity().getAssessmentID(), assessmentStudentID);
        assessmentStudentRepository.delete(entity);
        return entity;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updateSchoolOfRecord(List<AssessmentStudentEntity> students, String schoolOfRecordID, AssessmentEventEntity event) {
        students.forEach(student -> student.setSchoolOfRecordSchoolID(UUID.fromString(schoolOfRecordID)));
        assessmentStudentRepository.saveAll(students);

        this.assessmentEventRepository.findByEventId(event.getEventId()).ifPresent(existingEvent -> {
            existingEvent.setEventStatus(EventStatus.PROCESSED.toString());
            existingEvent.setUpdateDate(LocalDateTime.now());
            this.assessmentEventRepository.save(existingEvent);
        });
    }

    private AssessmentEventEntity generateStudentDeletedEvent(AssessmentStudentEntity deletedStudent) throws JsonProcessingException {
        return EventUtil.createEvent(
                ApplicationProperties.STUDENT_ASSESSMENT_API,
                ApplicationProperties.STUDENT_ASSESSMENT_API,
                JsonUtil.getJsonStringFromObject(deletedStudent.getStudentID()),
                ASSESSMENT_STUDENT_UPDATE,
                ASSESSMENT_STUDENT_UPDATED
        );
    }
}
