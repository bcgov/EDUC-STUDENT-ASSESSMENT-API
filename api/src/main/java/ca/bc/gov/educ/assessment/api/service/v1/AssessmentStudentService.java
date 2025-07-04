package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.constants.EventStatus;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentRulesProcessor;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentValidationIssue;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentRuleData;
import ca.bc.gov.educ.assessment.api.util.AssessmentUtil;
import ca.bc.gov.educ.assessment.api.util.EventUtil;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import ca.bc.gov.educ.assessment.api.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.ASSESSMENT_STUDENT_UPDATED;
import static ca.bc.gov.educ.assessment.api.constants.EventType.ASSESSMENT_STUDENT_UPDATE;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentStudentService {

    private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
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

    public List<UUID> getAllStudentIDsInSessionFromResultsStaging(UUID assessmentSessionID) {
        return stagedAssessmentStudentRepository.findAllStagedStudentsInSession(assessmentSessionID);
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

        return Integer.toString(assessmentStudentRepository.findNumberOfAttemptsForStudent(studentID, AssessmentUtil.getAssessmentTypeCodeList(assessment.getAssessmentTypeCode())));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<AssessmentStudent, AssessmentEventEntity> updateStudent(AssessmentStudentEntity assessmentStudentEntity) throws JsonProcessingException {
        AssessmentStudentEntity currentAssessmentStudentEntity = assessmentStudentRepository.findById(assessmentStudentEntity.getAssessmentStudentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentStudentEntity.class, "AssessmentStudent", assessmentStudentEntity.getAssessmentStudentID().toString())
        );

        assessmentStudentEntity.setAssessmentEntity(currentAssessmentStudentEntity.getAssessmentEntity());
        var student = processStudent(assessmentStudentEntity, currentAssessmentStudentEntity);

        var event = generateStudentUpdatedEvent(student.getStudentID());
        assessmentEventRepository.save(event);

        return Pair.of(student, event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentStudentEntity createStudentWithoutValidation(AssessmentStudentEntity assessmentStudentEntity) {
        return saveAssessmentStudentWithHistory(assessmentStudentEntity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAllStudentsInSessionAsDownloaded(UUID assessmentSessionID, String updateUser) {
        var currentDate = LocalDateTime.now();
        assessmentStudentRepository.updateDownloadDataAllByAssessmentSessionAndNoExemption(assessmentSessionID, currentDate, updateUser, currentDate);
        assessmentStudentHistoryRepository.insertHistoryForDownloadDateUpdate(assessmentSessionID, updateUser, currentDate);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<AssessmentStudent, AssessmentEventEntity> createStudent(AssessmentStudentEntity assessmentStudentEntity) throws JsonProcessingException {
        AssessmentEntity currentAssessmentEntity = assessmentRepository.findById(assessmentStudentEntity.getAssessmentEntity().getAssessmentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentEntity.class, "Assessment", assessmentStudentEntity.getAssessmentEntity().getAssessmentID().toString())
        );
        assessmentStudentEntity.setAssessmentEntity(currentAssessmentEntity);
        var student = processStudent(assessmentStudentEntity, null);
        var event = generateStudentUpdatedEvent(student.getStudentID());
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
                BeanUtils.copyProperties(assessmentStudentEntity, currentAssessmentStudentEntity, "schoolID", "studentID", "givenName", "surName", "pen", "localID", "courseStatusCode", "createUser", "createDate");
                TransformUtil.uppercaseFields(currentAssessmentStudentEntity);
                currentAssessmentStudentEntity.setNumberOfAttempts(Integer.parseInt(getNumberOfAttempts(currentAssessmentStudentEntity.getAssessmentEntity().getAssessmentID().toString(), currentAssessmentStudentEntity.getStudentID())));
                return mapper.toStructure(saveAssessmentStudentWithHistory(currentAssessmentStudentEntity));
            } else {
                assessmentStudentEntity.setStudentID(UUID.fromString(studentApiStudent.getStudentID()));
                assessmentStudentEntity.setNumberOfAttempts(Integer.parseInt(getNumberOfAttempts(assessmentStudentEntity.getAssessmentEntity().getAssessmentID().toString(), assessmentStudentEntity.getStudentID())));
                return mapper.toStructure(saveAssessmentStudentWithHistory(assessmentStudentEntity));
            }
        }

        AssessmentStudent studentWithValidationIssues = mapper.toStructure(assessmentStudentEntity);
        studentWithValidationIssues.setAssessmentStudentValidationIssues(validationIssues);

        return studentWithValidationIssues;
    }

    public AssessmentStudentEntity saveAssessmentStudentWithHistory(AssessmentStudentEntity assessmentStudentEntity) {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<AssessmentEventEntity> deleteStudents(List<UUID> assessmentStudentIDs) throws JsonProcessingException {
        ArrayList<AssessmentEventEntity> events = new ArrayList<>();
        for (UUID assessmentStudentID : assessmentStudentIDs) {
            try {
                AssessmentStudentEntity deletedStudent = performDeleteStudent(assessmentStudentID);
                AssessmentEventEntity event = assessmentEventRepository.save(generateStudentUpdatedEvent(deletedStudent.getStudentID().toString()));
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
        return assessmentEventRepository.save(generateStudentUpdatedEvent(deletedStudent.getStudentID().toString()));
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

    public AssessmentEventEntity generateStudentUpdatedEvent(String studentID) {
        try {
            return EventUtil.createEvent(
                    ApplicationProperties.STUDENT_ASSESSMENT_API,
                    ApplicationProperties.STUDENT_ASSESSMENT_API,
                    JsonUtil.getJsonStringFromObject(studentID),
                    ASSESSMENT_STUDENT_UPDATE,
                    ASSESSMENT_STUDENT_UPDATED
            );
        } catch (JsonProcessingException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

}
