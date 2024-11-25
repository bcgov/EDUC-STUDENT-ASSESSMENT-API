package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.constants.EventOutcome;
import ca.bc.gov.educ.eas.api.constants.EventType;
import ca.bc.gov.educ.eas.api.constants.TopicsEnum;
import ca.bc.gov.educ.eas.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.eas.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.eas.api.messaging.MessagePublisher;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentHistoryEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentHistoryRepository;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.eas.api.struct.Event;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import ca.bc.gov.educ.eas.api.util.TransformUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentStudentService {

    private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    private final AssessmentStudentHistoryService assessmentStudentHistoryService;
    private final MessagePublisher messagePublisher;
    private final AssessmentRepository assessmentRepository;

    public AssessmentStudentEntity getStudentByID(UUID assessmentStudentID) {
        return assessmentStudentRepository.findById(assessmentStudentID).orElseThrow(() ->
                new EntityNotFoundException(AssessmentStudent.class, "assessmentStudentID", assessmentStudentID.toString())
        );
    }

    public Optional<AssessmentStudentEntity> getStudentByAssessmentIDAndStudentID(UUID assessmentID, UUID studentID) {
        return assessmentStudentRepository.findByAssessmentEntity_AssessmentIDAndStudentID(assessmentID, studentID);
    }

    public int getNumberOfAttempts(String assessmentID, UUID studentID) {
        var assessment = assessmentRepository.findById(UUID.fromString(assessmentID)).orElseThrow(() ->
                new EntityNotFoundException(AssessmentEntity.class, "Assessment", assessmentID));

        return assessmentStudentRepository.findNumberOfAttemptsForStudent(studentID, getAssessmentTypeCodeList(assessment.getAssessmentTypeCode()));
    }

    private List<String> getAssessmentTypeCodeList(String assessmentTypeCode){
        if(assessmentTypeCode.startsWith("NM")){
            return Arrays.asList(AssessmentTypeCodes.NME.getCode(), AssessmentTypeCodes.NME10.getCode(), AssessmentTypeCodes.NMF.getCode(), AssessmentTypeCodes.NMF10.getCode());
        }
        return Arrays.asList(assessmentTypeCode);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentStudentEntity updateStudent(AssessmentStudentEntity assessmentStudentEntity) {
        AssessmentStudentEntity currentAssessmentStudentEntity = assessmentStudentRepository.findById(assessmentStudentEntity.getAssessmentStudentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentStudentEntity.class, "AssessmentStudent", assessmentStudentEntity.getAssessmentStudentID().toString())
        );
        BeanUtils.copyProperties(assessmentStudentEntity, currentAssessmentStudentEntity, "districtID", "schoolID", "studentID", "givenName", "surName", "pen", "localID", "isElectronicExam", "proficiencyScore", "courseStatusCode", "assessmentStudentStatusCode", "createUser", "createDate");
        TransformUtil.uppercaseFields(currentAssessmentStudentEntity);
        return createAssessmentStudentWithHistory(currentAssessmentStudentEntity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentStudentEntity createStudent(AssessmentStudentEntity assessmentStudentEntity) {
        return createAssessmentStudentWithHistory(assessmentStudentEntity);
    }

    public AssessmentStudentEntity createAssessmentStudentWithHistory(AssessmentStudentEntity assessmentStudentEntity) {
        AssessmentStudentEntity savedEntity = assessmentStudentRepository.save(assessmentStudentEntity);
        assessmentStudentHistoryRepository.save(this.assessmentStudentHistoryService.createAssessmentStudentHistoryEntity(assessmentStudentEntity, assessmentStudentEntity.getUpdateUser()));
        return savedEntity;
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
    public void deleteStudent(UUID assessmentStudentID) {
        Optional<AssessmentStudentEntity> entityOptional = assessmentStudentRepository.findById(assessmentStudentID);
        AssessmentStudentEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(AssessmentStudentEntity.class, "assessmentStudentID", assessmentStudentID.toString()));

        LocalDateTime sessionEnd = entity.getAssessmentEntity().getSessionEntity().getActiveUntilDate();
        boolean hasResult = entity.getProficiencyScore() != null || entity.getProvincialSpecialCaseCode() != null;

        if(sessionEnd.isAfter(LocalDateTime.now()) && !hasResult){
            List<AssessmentStudentHistoryEntity> studentHistoryEntities = assessmentStudentHistoryRepository.findAllByAssessmentIDAndAssessmentStudentID(entity.getAssessmentEntity().getAssessmentID(), assessmentStudentID);
            if(!studentHistoryEntities.isEmpty()) {
                assessmentStudentHistoryRepository.deleteAll(studentHistoryEntities);
            }
            assessmentStudentRepository.delete(entity);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete student.  Reason: " +
                            (sessionEnd.isBefore(LocalDateTime.now()) ? "Session has ended. " : "") +
                            (hasResult ? "Student has a proficiency score." : "")
            );
        }

    }

}
