package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.constants.EventStatus;
import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentRulesProcessor;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.*;
import ca.bc.gov.educ.assessment.api.util.AssessmentUtil;
import ca.bc.gov.educ.assessment.api.util.EventUtil;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import ca.bc.gov.educ.assessment.api.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.util.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;

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
    private final StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    private final AssessmentEventRepository assessmentEventRepository;
    private final AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    private final AssessmentStudentHistoryService assessmentStudentHistoryService;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentStudentRulesProcessor assessmentStudentRulesProcessor;
    private final RestUtils restUtils;
    private final StagedStudentResultRepository stagedStudentResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;

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
    public Pair<AssessmentStudent, AssessmentEventEntity> updateStudent(AssessmentStudentEntity assessmentStudentEntity, boolean allowRuleOverride) {
        AssessmentStudentEntity currentAssessmentStudentEntity = assessmentStudentRepository.findById(assessmentStudentEntity.getAssessmentStudentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentStudentEntity.class, "AssessmentStudent", assessmentStudentEntity.getAssessmentStudentID().toString())
        );
        if(currentAssessmentStudentEntity.getAssessmentEntity().getAssessmentID().equals(assessmentStudentEntity.getAssessmentEntity().getAssessmentID())) {
            assessmentStudentEntity.setAssessmentEntity(currentAssessmentStudentEntity.getAssessmentEntity());
        } else {
            var updatedAssessmentEntity = assessmentRepository.findById(assessmentStudentEntity.getAssessmentEntity().getAssessmentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentEntity.class, "AssessmentEntity", assessmentStudentEntity.getAssessmentEntity().getAssessmentID().toString())
            );
            assessmentStudentEntity.setAssessmentEntity(updatedAssessmentEntity);
        }

        var student = processStudent(assessmentStudentEntity, currentAssessmentStudentEntity, false, allowRuleOverride);

        var event = generateStudentUpdatedEvent(student.getStudentID());
        assessmentEventRepository.save(event);

        return Pair.of(student, event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AssessmentStudentEntity saveAssessmentStudentWithHistoryInCurrentTransaction(AssessmentStudentEntity assessmentStudentEntity) {
        AssessmentStudentEntity savedEntity = assessmentStudentRepository.save(assessmentStudentEntity);
        assessmentStudentHistoryRepository.save(this.assessmentStudentHistoryService.createAssessmentStudentHistoryEntity(assessmentStudentEntity, assessmentStudentEntity.getUpdateUser()));
        return savedEntity;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AssessmentStudentEntity createStudentWithoutValidationInCurrentTransaction(AssessmentStudentEntity assessmentStudentEntity) {
        return saveAssessmentStudentWithHistoryInCurrentTransaction(assessmentStudentEntity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<AssessmentStudent, AssessmentEventEntity> createStudent(AssessmentStudentEntity assessmentStudentEntity, boolean allowRuleOverride) {
        AssessmentEntity currentAssessmentEntity = assessmentRepository.findById(assessmentStudentEntity.getAssessmentEntity().getAssessmentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentEntity.class, "Assessment", assessmentStudentEntity.getAssessmentEntity().getAssessmentID().toString())
        );
        assessmentStudentEntity.setAssessmentEntity(currentAssessmentEntity);
        assessmentStudentEntity.setStudentStatus(StudentStatusCodes.ACTIVE.getCode());
        var student = processStudent(assessmentStudentEntity, null, true, allowRuleOverride);
        var event = generateStudentUpdatedEvent(student.getStudentID());
        assessmentEventRepository.save(event);

        return Pair.of(student, event);
    }

    private AssessmentStudent processStudent(AssessmentStudentEntity assessmentStudentEntity, AssessmentStudentEntity currentAssessmentStudentEntity, boolean newAssessmentStudentRegistration, boolean allowRuleOverride) {
        SchoolTombstone schoolTombstone = restUtils.getSchoolBySchoolID(assessmentStudentEntity.getSchoolOfRecordSchoolID().toString()).orElse(null);

        UUID studentCorrelationID = UUID.randomUUID();
        log.info("Retrieving student record for PEN ::{} with correlationID :: {}", assessmentStudentEntity.getPen(), studentCorrelationID);
        Student studentApiStudent = restUtils.getStudentByPEN(studentCorrelationID, assessmentStudentEntity.getPen()).orElseThrow(() ->
                new EntityNotFoundException(Student.class, "Student", assessmentStudentEntity.getPen()));

        List<AssessmentStudentValidationIssue> validationIssues = runValidationRules(assessmentStudentEntity, schoolTombstone, studentApiStudent, allowRuleOverride);

        if (newAssessmentStudentRegistration) {
            UUID gradStudentRecordCorrelationID = UUID.randomUUID();
            log.info("Retrieving GRAD Student Record for student ID :: {} with correlationID :: {}", studentApiStudent.getStudentID(), gradStudentRecordCorrelationID);
            Optional<GradStudentRecord> gradStudentRecord = restUtils.getGradStudentRecordByStudentID(gradStudentRecordCorrelationID, UUID.fromString(studentApiStudent.getStudentID()));
            String gradeFromGrad = gradStudentRecord.map(GradStudentRecord::getStudentGrade).orElse(null);
            String gradeAtRegistration = StringUtils.isEmpty(gradeFromGrad) ? studentApiStudent.getGradeCode() : gradeFromGrad;
            assessmentStudentEntity.setGradeAtRegistration(gradeAtRegistration);
            if (currentAssessmentStudentEntity != null) {
                currentAssessmentStudentEntity.setGradeAtRegistration(gradeAtRegistration);
            }
        }

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

    public List<AssessmentStudentValidationIssue> runValidationRules(AssessmentStudentEntity assessmentStudentEntity, SchoolTombstone schoolTombstone, Student studentApiStudent, boolean allowRuleOverride) {
        StudentRuleData studentRuleData = new StudentRuleData();
        studentRuleData.setAssessmentStudentEntity(assessmentStudentEntity);
        studentRuleData.setSchool(schoolTombstone);
        studentRuleData.setStudentApiStudent(studentApiStudent);
        studentRuleData.setAllowRuleOverride(allowRuleOverride);

        return this.assessmentStudentRulesProcessor.processRules(studentRuleData);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<AssessmentEventEntity> deleteStudents(List<UUID> assessmentStudentIDs, boolean allowRuleOverride) {
        ArrayList<AssessmentEventEntity> events = new ArrayList<>();
        for (UUID assessmentStudentID : assessmentStudentIDs) {
            try {
                AssessmentStudentEntity deletedStudent = performDeleteStudent(assessmentStudentID, allowRuleOverride);
                AssessmentEventEntity event = assessmentEventRepository.save(generateStudentUpdatedEvent(deletedStudent.getStudentID().toString()));
                events.add(event);
            } catch (EntityNotFoundException | InvalidPayloadException e) {
                throw new InvalidPayloadException(ApiError.builder().timestamp(LocalDateTime.now()).message(e.getMessage()).status(CONFLICT).build());
            }
        }
        return events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentEventEntity deleteStudent(UUID assessmentStudentID) {
        AssessmentStudentEntity deletedStudent = performDeleteStudent(assessmentStudentID, false);
        return assessmentEventRepository.save(generateStudentUpdatedEvent(deletedStudent.getStudentID().toString()));
    }

    private AssessmentStudentEntity performDeleteStudent(UUID assessmentStudentID, boolean allowRuleOverride) {
        Optional<AssessmentStudentEntity> entityOptional = assessmentStudentRepository.findById(assessmentStudentID);
        AssessmentStudentEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(AssessmentStudentEntity.class, "assessmentStudentID", assessmentStudentID.toString()));

        LocalDateTime sessionEnd = entity.getAssessmentEntity().getAssessmentSessionEntity().getActiveUntilDate();
        boolean sessionEnded = sessionEnd.isBefore(LocalDateTime.now());
        boolean hasProvincialSpecialCaseCode = !allowRuleOverride && entity.getProvincialSpecialCaseCode() != null;
        boolean hasProficiencyScore = entity.getProficiencyScore() != null;
        boolean hasResult = hasProficiencyScore || hasProvincialSpecialCaseCode;

        if (sessionEnded || hasResult) {
            throw new InvalidPayloadException(ApiError.builder().timestamp(LocalDateTime.now()).message("Cannot delete student. Reason: %s %s %s".formatted(sessionEnded ? "Session has ended. " : "", hasProficiencyScore ? "Student has a proficiency score." : "", hasProvincialSpecialCaseCode ? "Student has a special case code." : "").trim()).status(CONFLICT).build());
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


    public List<AssessmentResultsSummary> getResultsUploadSummary(UUID sessionID) {
        AssessmentSessionEntity session = assessmentSessionRepository.findById(sessionID)
                .orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "sessionID", sessionID.toString()));

        List<AssessmentEntity> assessments = session.getAssessments().stream().toList();
        boolean isSessionOpen = StringUtils.isBlank(session.getApprovalAssessmentAnalysisUserID())
                && StringUtils.isBlank(session.getApprovalAssessmentDesignUserID())
                && StringUtils.isBlank(session.getApprovalStudentCertUserID());

        List<AssessmentResultsSummary> rowData = new ArrayList<>();
        for (AssessmentEntity assessment : assessments) {
            if(!assessment.getAssessmentForms().isEmpty()) {
                List<UUID> formIds = assessment.getAssessmentForms().stream().map(AssessmentFormEntity::getAssessmentFormID).toList();
                if(isSessionOpen) {
                    rowData.add(getSummaryForOpenSession(assessment, formIds));
                } else {
                    rowData.add(getSummaryForApprovedSession(assessment, formIds));
                }
            }
        }
        return rowData;
    }

    private AssessmentResultsSummary getSummaryForOpenSession(AssessmentEntity assessment, List<UUID> formIds) {
        Optional<StagedAssessmentStudentEntity> student = stagedAssessmentStudentRepository.findByAssessmentIdAndAssessmentFormIdOrderByCreateDateDesc(assessment.getAssessmentID(), formIds);
        var stagedStudentResult =  stagedStudentResultRepository.findByAssessmentIdAndStagedStudentResultStatusOrderByCreateDateDesc(assessment.getAssessmentID());
        return AssessmentResultsSummary
                .builder()
                .assessmentType(assessment.getAssessmentTypeCode())
                .uploadedBy(stagedStudentResult.isPresent() ? null : student.map(StagedAssessmentStudentEntity::getCreateUser).orElse(null))
                .uploadDate(stagedStudentResult.isPresent() ? null : student.map(assessmentStudentEntity -> assessmentStudentEntity.getCreateDate().toString()).orElse(null))
                .build();
    }

    private AssessmentResultsSummary getSummaryForApprovedSession(AssessmentEntity assessment, List<UUID> formIds) {
        Optional<AssessmentStudentEntity> student = assessmentStudentRepository.findByAssessmentIdAndAssessmentFormIdOrderByCreateDateDesc(assessment.getAssessmentID(), formIds);
        return AssessmentResultsSummary
                .builder()
                .assessmentType(assessment.getAssessmentTypeCode())
                .uploadedBy(student.map(AssessmentStudentEntity::getCreateUser).orElse(null))
                .uploadDate(student.map(assessmentStudentEntity -> assessmentStudentEntity.getCreateDate().toString()).orElse(null))
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markStagedStudentsReadyForTransfer() {
        log.debug("Marking all staged students as ready for transfer");

        LocalDateTime updateTime = LocalDateTime.now();
        int updatedCount = stagedAssessmentStudentRepository.updateAllStagedAssessmentStudentStatus(
            "TRANSFER",
            "ASSESSMENT-API",
            updateTime
        );

        log.debug("Successfully marked {} staged students as ready for transfer", updatedCount);

        return updatedCount;
    }

    public List<UUID> findBatchOfTransferStudentIds(int batchSize) {
        log.debug("Finding batch of {} students with TRANSFER status", batchSize);
        Pageable pageable = PageRequest.of(0, batchSize);
        return stagedAssessmentStudentRepository.findStudentIdsByStatusOrderByUpdateDate("TRANSFER", pageable);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markStudentAsTransferInProgress(UUID studentId) {
        log.debug("Marking student {} as TRANSFERIN", studentId);

        LocalDateTime updateTime = LocalDateTime.now();
        List<UUID> studentIds = List.of(studentId);
        int updatedCount = stagedAssessmentStudentRepository.updateStagedAssessmentStudentStatusByIds(
            studentIds,
            "TRANSFER",
            "TRANSFERIN",
            "ASSESSMENT-API",
            updateTime
        );

        log.debug("Successfully marked student {} as TRANSFERIN (updated: {})", studentId, updatedCount);
        return updatedCount;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public int markStudentAsTransferredInCurrentTransaction(UUID studentId) {
        log.debug("Marking student {} as TRANSFERRED in current transaction", studentId);

        LocalDateTime updateTime = LocalDateTime.now();
        List<UUID> studentIds = List.of(studentId);
        int updatedCount = stagedAssessmentStudentRepository.updateStagedAssessmentStudentStatusByIds(
            studentIds,
            "TRANSFERIN",
            "TRANSFERRED",
            "ASSESSMENT-API",
            updateTime
        );

        log.debug("Successfully marked student {} as TRANSFERRED (updated: {})", studentId, updatedCount);
        return updatedCount;
    }

    public StagedAssessmentStudentEntity getStagedStudentById(UUID stagedStudentId) {
        return stagedAssessmentStudentRepository.findById(stagedStudentId).orElseThrow(() ->
                new EntityNotFoundException(StagedAssessmentStudentEntity.class, "stagedStudentId", stagedStudentId.toString())
        );
    }
}
