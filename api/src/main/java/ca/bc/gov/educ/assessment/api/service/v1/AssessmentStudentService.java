package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.StudentStatusCodes;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.mappers.v1.*;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentRulesProcessor;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentValidationFieldCode;
import ca.bc.gov.educ.assessment.api.rules.assessment.AssessmentStudentValidationIssueTypeCode;
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
import java.util.stream.Collectors;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.ASSESSMENT_STUDENT_UPDATED;
import static ca.bc.gov.educ.assessment.api.constants.EventType.ASSESSMENT_STUDENT_UPDATE;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentStudentService {

    private static final AssessmentStudentListItemMapper assessmentStudentListItemMapper = AssessmentStudentListItemMapper.mapper;
    public static final String CREATE_USER = "createUser";
    public static final String UPDATE_USER = "updateUser";
    public static final String CREATE_DATE = "createDate";
    public static final String UPDATE_DATE = "updateDate";
    public static final String TARGET_STUDENT_ID = "targetStudentID";
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final StagedAssessmentStudentRepository stagedAssessmentStudentRepository;
    private final AssessmentEventRepository assessmentEventRepository;
    private final AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;
    private final AssessmentStudentHistoryService assessmentStudentHistoryService;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentRulesService assessmentRulesService;
    private final AssessmentStudentRulesProcessor assessmentStudentRulesProcessor;
    private final RestUtils restUtils;
    private final StagedStudentResultRepository stagedStudentResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;

    public static final String ASSESSMENT_STUDENT_ID = "assessmentStudentID";
    public static final String ERROR = "ERROR";

    public AssessmentStudentEntity getStudentByID(UUID assessmentStudentID) {
        return assessmentStudentRepository.findById(assessmentStudentID).orElseThrow(() ->
                new EntityNotFoundException(AssessmentStudent.class, ASSESSMENT_STUDENT_ID, assessmentStudentID.toString())
        );
    }

    @Transactional(readOnly = true)
    public AssessmentStudentEntity getStudentWithAssessmentDetailsByID(UUID assessmentStudentID, UUID assessmentID) {
        return assessmentStudentRepository.findByIdWithAssessmentDetails(assessmentStudentID, assessmentID).orElseThrow(() ->
                new EntityNotFoundException(AssessmentStudent.class, ASSESSMENT_STUDENT_ID, assessmentStudentID.toString())
        );
    }

    public List<AssessmentStudentEntity> getStudentByStudentId(UUID studentID) {
        return assessmentStudentRepository.findByStudentID(studentID);
    }

    public List<StagedAssessmentStudentEntity> getStagedStudentByStudentId(UUID studentID) {
        return stagedAssessmentStudentRepository.findByStudentID(studentID);
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

    @Transactional(propagation = Propagation.MANDATORY)
    public Pair<AssessmentStudentListItem, AssessmentEventEntity> updateStudent(AssessmentStudentEntity assessmentStudentEntity, boolean allowRuleOverride, String source) {
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

        var student = processStudent(assessmentStudentEntity, currentAssessmentStudentEntity, false, allowRuleOverride, source);

        AssessmentEventEntity event = null;
        if(student.getAssessmentStudentValidationIssues() == null || student.getAssessmentStudentValidationIssues().isEmpty()) {
            event = generateStudentUpdatedEvent(student.getStudentID());
            assessmentEventRepository.save(event);
        }

        return Pair.of(student, event);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public AssessmentStudentEntity saveAssessmentStudentWithHistoryInCurrentTransaction(AssessmentStudentEntity assessmentStudentEntity) {
        AssessmentStudentEntity savedEntity = assessmentStudentRepository.save(assessmentStudentEntity);
        assessmentStudentHistoryRepository.save(this.assessmentStudentHistoryService.createAssessmentStudentHistoryEntity(assessmentStudentEntity, assessmentStudentEntity.getUpdateUser()));
        return savedEntity;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Pair<AssessmentStudentListItem, AssessmentEventEntity> createStudent(AssessmentStudentEntity assessmentStudentEntity, boolean allowRuleOverride, String source) {
        AssessmentEntity currentAssessmentEntity = assessmentRepository.findById(assessmentStudentEntity.getAssessmentEntity().getAssessmentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentEntity.class, "Assessment", assessmentStudentEntity.getAssessmentEntity().getAssessmentID().toString())
        );
        assessmentStudentEntity.setAssessmentEntity(currentAssessmentEntity);
        assessmentStudentEntity.setStudentStatusCode(StudentStatusCodes.ACTIVE.getCode());
        var student = processStudent(assessmentStudentEntity, null, true, allowRuleOverride, source);

        AssessmentEventEntity event = null;
        if(student.getAssessmentStudentValidationIssues() != null) {
            event = generateStudentUpdatedEvent(student.getStudentID());
            assessmentEventRepository.save(event);
        }

        return Pair.of(student, event);
    }

    private AssessmentStudentListItem processStudent(AssessmentStudentEntity assessmentStudentEntity, AssessmentStudentEntity currentAssessmentStudentEntity, boolean newAssessmentStudentRegistration, boolean allowRuleOverride, String source) {
        SchoolTombstone schoolTombstone = restUtils.getSchoolBySchoolID(assessmentStudentEntity.getSchoolOfRecordSchoolID().toString()).orElse(null);

        UUID studentCorrelationID = UUID.randomUUID();
        log.info("Retrieving student record for PEN ::{} with correlationID :: {}", assessmentStudentEntity.getPen(), studentCorrelationID);
        Student studentApiStudent = restUtils.getStudentByPEN(studentCorrelationID, assessmentStudentEntity.getPen()).orElseThrow(() ->
                new EntityNotFoundException(Student.class, "Student", assessmentStudentEntity.getPen()));

        List<AssessmentStudentValidationIssue> validationIssues = runValidationRules(assessmentStudentEntity, schoolTombstone, studentApiStudent, allowRuleOverride, source);

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
            overrideProficiencyScoreIfSpecialCase(assessmentStudentEntity);
            if (currentAssessmentStudentEntity != null) {
                BeanUtils.copyProperties(assessmentStudentEntity, currentAssessmentStudentEntity, "schoolOfRecordSchoolID", "studentID", "givenName", "surname", "pen", "localID", "courseStatusCode", CREATE_USER, CREATE_DATE);
                TransformUtil.uppercaseFields(currentAssessmentStudentEntity);
                currentAssessmentStudentEntity.setNumberOfAttempts(Integer.parseInt(getNumberOfAttempts(currentAssessmentStudentEntity.getAssessmentEntity().getAssessmentID().toString(), currentAssessmentStudentEntity.getStudentID())));
                setSchoolOfRecordAtWriteIfExempt(currentAssessmentStudentEntity);
                return assessmentStudentListItemMapper.toStructure(saveAssessmentStudentWithHistory(currentAssessmentStudentEntity));
            } else {
                assessmentStudentEntity.setStudentID(UUID.fromString(studentApiStudent.getStudentID()));
                assessmentStudentEntity.setNumberOfAttempts(Integer.parseInt(getNumberOfAttempts(assessmentStudentEntity.getAssessmentEntity().getAssessmentID().toString(), assessmentStudentEntity.getStudentID())));
                setSchoolOfRecordAtWriteIfExempt(assessmentStudentEntity);
                return assessmentStudentListItemMapper.toStructure(saveAssessmentStudentWithHistory(assessmentStudentEntity));
            }
        }
        if(currentAssessmentStudentEntity == null) { //set studentID on creates with validation issues for the event to use
            assessmentStudentEntity.setStudentID(UUID.fromString(studentApiStudent.getStudentID()));
        }
        AssessmentStudentListItem studentWithValidationIssues = assessmentStudentListItemMapper.toStructure(assessmentStudentEntity);
        studentWithValidationIssues.setAssessmentStudentValidationIssues(validationIssues);

        return studentWithValidationIssues;
    }
    
    private void overrideProficiencyScoreIfSpecialCase(AssessmentStudentEntity assessmentStudentEntity){
        if(StringUtils.isNotBlank(assessmentStudentEntity.getProvincialSpecialCaseCode())){
            assessmentStudentEntity.setProficiencyScore(null);
        }
    }

    private void setSchoolOfRecordAtWriteIfExempt(AssessmentStudentEntity assessmentStudentEntity) {
        if(assessmentStudentEntity.getSchoolOfRecordSchoolID() != null && assessmentStudentEntity.getSchoolAtWriteSchoolID() == null && StringUtils.isNotBlank(assessmentStudentEntity.getProvincialSpecialCaseCode())) {
            assessmentStudentEntity.setSchoolAtWriteSchoolID(assessmentStudentEntity.getSchoolOfRecordSchoolID());
        }
    }

    public AssessmentStudentEntity saveAssessmentStudentWithHistory(AssessmentStudentEntity assessmentStudentEntity) {
        AssessmentStudentEntity savedEntity = assessmentStudentRepository.save(assessmentStudentEntity);
        assessmentStudentHistoryRepository.save(this.assessmentStudentHistoryService.createAssessmentStudentHistoryEntity(assessmentStudentEntity, assessmentStudentEntity.getUpdateUser()));
        return savedEntity;
    }

    public List<AssessmentStudentValidationIssue> runValidationRules(AssessmentStudentEntity assessmentStudentEntity, SchoolTombstone schoolTombstone, Student studentApiStudent, boolean allowRuleOverride, String source) {
        StudentRuleData studentRuleData = new StudentRuleData();
        studentRuleData.setAssessmentStudentEntity(assessmentStudentEntity);
        studentRuleData.setSchool(schoolTombstone);
        studentRuleData.setStudentApiStudent(studentApiStudent);
        studentRuleData.setAllowRuleOverride(allowRuleOverride);
        studentRuleData.setSource(source);
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
        AssessmentStudentEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(AssessmentStudentEntity.class, ASSESSMENT_STUDENT_ID, assessmentStudentID.toString()));

        LocalDateTime sessionEnd = entity.getAssessmentEntity().getAssessmentSessionEntity().getActiveUntilDate();
        boolean sessionEnded = !allowRuleOverride && sessionEnd.isBefore(LocalDateTime.now());
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
        students.forEach(student -> {
            student.setSchoolOfRecordSchoolID(UUID.fromString(schoolOfRecordID));
            saveAssessmentStudentWithHistoryInCurrentTransaction(student);
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

        List<AssessmentResultsSummary> rowData = new ArrayList<>();
        for (AssessmentEntity assessment : assessments) {
            if(!assessment.getAssessmentForms().isEmpty()) {
                List<UUID> formIds = assessment.getAssessmentForms().stream().map(AssessmentFormEntity::getAssessmentFormID).toList();
                if(session.getCompletionDate() == null) {
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
    public List<AssessmentEventEntity> flipFlagsForNoWriteStudents(UUID sessionID) {
       var students = assessmentStudentRepository.findAllStudentsRegisteredThatHaveNotWritten(sessionID);

       List<AssessmentEventEntity> events = new ArrayList<>();
       students.forEach(student -> {
          var event = generateStudentUpdatedEvent(student.toString());
          assessmentEventRepository.save(event);
          events.add(event);
       });

       return events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markStagedStudentsReadyForTransferOrDelete() {
        LocalDateTime updateTime = LocalDateTime.now();

        int updatedCount = stagedAssessmentStudentRepository.updateAllStagedStudentsForTransferOrDelete(
            ApplicationProperties.STUDENT_ASSESSMENT_API,
            updateTime
        );

        log.debug("Successfully marked {} staged students as ready for transfer or deletion", updatedCount);

        return updatedCount;
    }

    public List<StagedAssessmentStudentEntity> findBatchOfTransferStudentIds(int batchSize) {
        Pageable pageable = PageRequest.of(0, batchSize);
        return stagedAssessmentStudentRepository.findStudentIdsByStatusOrderByUpdateDate("TRANSFER", pageable);
    }

    public List<StagedAssessmentStudentEntity> findBatchOfDeleteStudentIds(int batchSize) {
        Pageable pageable = PageRequest.of(0, batchSize);
        return stagedAssessmentStudentRepository.findStudentIdsByStatusOrderByUpdateDate("DELETE", pageable);
    }

    public StagedAssessmentStudentEntity getStagedStudentById(UUID stagedStudentId) {
        return stagedAssessmentStudentRepository.findById(stagedStudentId).orElseThrow(() ->
                new EntityNotFoundException(StagedAssessmentStudentEntity.class, "stagedStudentId", stagedStudentId.toString())
        );
    }

    public void deleteStagedStudent(StagedAssessmentStudentEntity stagedAssessmentStudentEntity) {
        stagedAssessmentStudentRepository.deleteById(stagedAssessmentStudentEntity.getAssessmentStudentID());
    }

    public void deleteStagedStudents(List<StagedAssessmentStudentEntity> stagedAssessmentStudents) {
        stagedAssessmentStudentRepository.deleteAll(stagedAssessmentStudents);
    }

    @Transactional(readOnly = true)
    public AssessmentStudentShowItem getStudentWithAssessmentDetailsById(UUID assessmentStudentID, UUID assessmentID) {
        AssessmentStudentEntity entity = assessmentStudentRepository.findByIdWithAssessmentDetails(assessmentStudentID, assessmentID)
                .orElseThrow(() -> new EntityNotFoundException(AssessmentStudent.class, ASSESSMENT_STUDENT_ID, assessmentStudentID.toString()));

        // Map the main DTO while we're inside the transaction so Hibernate Session is open
        AssessmentStudentShowItem dto = AssessmentStudentShowItemMapper.mapper.toStructure(entity);

        // Explicitly populate form -> components only for this DTO
        if (entity.getAssessmentEntity() != null && dto.getAssessmentDetails() != null && dto.getAssessmentDetails().getAssessmentForms() != null) {
            // build lookup by form id (string) for DTO forms
            var formDtoMap = dto.getAssessmentDetails().getAssessmentForms().stream()
                    .filter(f -> f.getAssessmentFormID() != null)
                    .collect(Collectors.toMap(AssessmentForm::getAssessmentFormID, f -> f));

            // map components from entities to DTOs and set them on matching DTO form
            for (var formEntity : entity.getAssessmentEntity().getAssessmentForms()) {
                if (formEntity == null) continue;
                var formId = formEntity.getAssessmentFormID() == null ? null : formEntity.getAssessmentFormID().toString();
                if (formId == null) continue;
                AssessmentForm formDto = formDtoMap.get(formId);
                if (formDto == null) continue;

                if (formEntity.getAssessmentComponentEntities() != null && !formEntity.getAssessmentComponentEntities().isEmpty()) {
                    List<AssessmentComponent> comps = formEntity.getAssessmentComponentEntities().stream()
                            .map(AssessmentComponentMapper.mapper::toStructure)
                            .toList();
                    formDto.setAssessmentComponents(comps);
                }
            }
        }

        return dto;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<List<AssessmentStudentValidationIssue>,  List<AssessmentEventEntity>> transferStudentAssessments(AssessmentStudentMoveRequest assessmentStudentTransfer) {

        String sourceStudentID = String.valueOf(assessmentStudentTransfer.getSourceStudentID());
        String targetStudentID = String.valueOf(assessmentStudentTransfer.getTargetStudentID());
        
        log.info("Transferring {} assessment students from {} to {}", assessmentStudentTransfer.getStudentAssessmentIDsToMove().size(), assessmentStudentTransfer.getSourceStudentID(), assessmentStudentTransfer.getTargetStudentID());

        UUID correlationID = UUID.randomUUID();
        Set<String> studentIDs = new HashSet<>();
        studentIDs.add(sourceStudentID);
        studentIDs.add(targetStudentID);
        
        List<Student> students = restUtils.getStudents(correlationID, studentIDs);

        Student sourceStudentApiStudent = students.stream()
                .filter(s -> s.getStudentID().equals(sourceStudentID))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(Student.class, "sourceStudentID", sourceStudentID));

        Student targetStudentApiStudent = students.stream()
                .filter(s -> s.getStudentID().equals(targetStudentID))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(Student.class, TARGET_STUDENT_ID, targetStudentID));

        GradStudentRecord targetGradStudent = restUtils.getGradStudentRecordByStudentID(correlationID, assessmentStudentTransfer.getTargetStudentID())
            .orElseThrow(() -> new EntityNotFoundException(GradStudentRecord.class, TARGET_STUDENT_ID, targetStudentID));

        Pair<List<AssessmentStudentEntity>, List<AssessmentStudentValidationIssue>> validationResult = validateAssessments(sourceStudentApiStudent, targetStudentApiStudent, assessmentStudentTransfer.getStudentAssessmentIDsToMove());

        List<AssessmentStudentEntity> validatedEntities = validationResult.getLeft();
        List<AssessmentStudentValidationIssue> allValidationIssues = validationResult.getRight();
        List<AssessmentEventEntity> events = new ArrayList<>();

        if (allValidationIssues.isEmpty()) {
            for (AssessmentStudentEntity assessmentStudentEntity : validatedEntities) {
                assessmentStudentEntity.setStudentID(assessmentStudentTransfer.getTargetStudentID());
                assessmentStudentEntity.setPen(targetStudentApiStudent.getPen());
                assessmentStudentEntity.setSchoolOfRecordSchoolID(UUID.fromString(targetGradStudent.getSchoolOfRecordId()));
                assessmentStudentEntity.setUpdateUser(assessmentStudentTransfer.getUpdateUser());
                assessmentStudentEntity.setUpdateDate(LocalDateTime.now());
                saveAssessmentStudentWithHistory(assessmentStudentEntity);
            }
            events.add(generateStudentUpdatedEvent(sourceStudentID));
            events.add(generateStudentUpdatedEvent(targetStudentID));
            assessmentEventRepository.saveAll(events);
        }
        
        return Pair.of(allValidationIssues, events);
    }

    private Pair<List<AssessmentStudentEntity>, List<AssessmentStudentValidationIssue>> validateAssessments(Student sourceStudentApiStudent, Student targetStudentApiStudent, List<UUID> assessmentStudentIDs) {
        
        List<AssessmentStudentValidationIssue> allValidationIssues = new ArrayList<>();
        
        // Validation 1: Check if transferring to same PEN
        if (sourceStudentApiStudent.getPen().equals(targetStudentApiStudent.getPen())) {
            AssessmentStudentValidationIssue issue = AssessmentStudentValidationIssue.builder()
                .validationIssueSeverityCode(ERROR)
                .validationIssueCode("TRANSFER_SAME_PEN")
                .validationIssueFieldCode("PEN")
                .validationMessage("Cannot transfer assessment to the same student.")
                .build();
            allValidationIssues.add(issue);
            return Pair.of(new ArrayList<>(), allValidationIssues);
        }
        
        // Validation 2: Check if target PEN is merged
        if ("M".equals(targetStudentApiStudent.getStatusCode())) {
            AssessmentStudentValidationIssue issue = AssessmentStudentValidationIssue.builder()
                .validationIssueSeverityCode(ERROR)
                .validationIssueCode("TRANSFER_TO_MERGED_PEN")
                .validationIssueFieldCode("PEN")
                .validationMessage("Cannot transfer assessment to a merged PEN.")
                .build();
            allValidationIssues.add(issue);
            return Pair.of(new ArrayList<>(), allValidationIssues);
        }

        List<AssessmentStudentEntity> validatedEntities = new ArrayList<>();
        for (UUID assessmentStudentID : assessmentStudentIDs) {
            AssessmentStudentEntity entity = assessmentStudentRepository.findById(assessmentStudentID)
                    .orElseThrow(() -> new EntityNotFoundException(AssessmentStudentEntity.class, ASSESSMENT_STUDENT_ID, assessmentStudentID.toString()));

            // Validation 3: Must have a result (proficiency score OR special case)
            boolean hasProficiencyScore = entity.getProficiencyScore() != null;
            boolean hasProvincialSpecialCaseCode = entity.getProvincialSpecialCaseCode() != null;
            boolean hasResult = hasProficiencyScore || hasProvincialSpecialCaseCode;
            boolean hasValidationIssue = false;
            if (!hasResult) {
                AssessmentStudentValidationIssue issue = AssessmentStudentValidationIssue.builder()
                    .validationIssueSeverityCode(ERROR)
                    .validationIssueCode("TRANSFER_NO_RESULT")
                    .validationIssueFieldCode("ASSESSMENT_STUDENT_ID")
                    .validationMessage("Can only transfer assessments that have a proficiency score or special case code.")
                    .assessmentStudentID(assessmentStudentID.toString())
                    .build();
                allValidationIssues.add(issue);
                hasValidationIssue = true;
            }
            //Validation 4: Target student must not already have the same assessment in the same session
            boolean sourceStudentHasDuplicate = assessmentRulesService.hasStudentAssessmentDuplicate(UUID.fromString(sourceStudentApiStudent.getStudentID()), entity.getAssessmentEntity(), assessmentStudentID);
            if(sourceStudentHasDuplicate) {
                AssessmentStudentValidationIssue issue = AssessmentStudentValidationIssue.builder()
                    .validationIssueSeverityCode(ERROR)
                    .validationIssueCode("TRANSFER_HAS_DUPLICATE")
                    .validationIssueFieldCode("ASSESSMENT_STUDENT_ID")
                    .validationMessage("Cannot transfer assessment to student who already has the same assessment/session.")
                    .assessmentStudentID(assessmentStudentID.toString())
                    .build();
                allValidationIssues.add(issue);
                hasValidationIssue = true;
            }
            if(hasValidationIssue) {
                continue;
            }
            validatedEntities.add(entity);
        }
        return Pair.of(validatedEntities, allValidationIssues);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<List<AssessmentStudentListItem>, AssessmentEventEntity> mergeStudentAssessments(AssessmentStudentMoveRequest assessmentStudentMerge) {
        List<AssessmentStudentListItem> response = new ArrayList<>();
        var targetStudentID = assessmentStudentMerge.getTargetStudentID();
        var sourceStudentID = assessmentStudentMerge.getSourceStudentID();
        var correlationID = UUID.randomUUID();

        Student targetStudent = restUtils.getStudents(correlationID, Set.of(targetStudentID.toString())).stream()
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException(Student.class, TARGET_STUDENT_ID, targetStudentID.toString()));
        GradStudentRecord targetStudentGrad = restUtils.getGradStudentRecordByStudentID(correlationID, targetStudentID)
            .orElseThrow(() -> new EntityNotFoundException(Student.class, TARGET_STUDENT_ID, targetStudentID.toString()));
        var targetSchool = targetStudentGrad.getSchoolOfRecordId();

        Map<String, AssessmentStudentEntity> assessmentStudentsToMerge = getAssessmentStudentsToMerge(assessmentStudentMerge.getStudentAssessmentIDsToMove(), sourceStudentID);
        Map<String, AssessmentStudentEntity> existingAssessmentStudents = getAssessmentStudentsAsMap(targetStudentID);

        Set<String> assessmentStudentWithValidationIssues = validateMergeConflicts(existingAssessmentStudents, assessmentStudentsToMerge, response);

        List<AssessmentStudentEntity> assessmentsToOverwrite = prepareAssessmentsToOverwrite(existingAssessmentStudents, assessmentStudentsToMerge, assessmentStudentWithValidationIssues, targetStudent, targetSchool, assessmentStudentMerge);
        List<AssessmentStudentEntity> assessmentsToAdd = prepareAssessmentsToAdd(existingAssessmentStudents, assessmentStudentsToMerge, targetStudent, targetSchool, assessmentStudentMerge);

        assessmentsToAdd.forEach(assessment -> {
            AssessmentEntity currentAssessmentEntity = assessmentRepository.findById(assessment.getAssessmentEntity().getAssessmentID()).orElseThrow(() ->
                    new EntityNotFoundException(AssessmentEntity.class, "Assessment", assessment.getAssessmentEntity().getAssessmentID().toString())
            );
            assessment.setAssessmentEntity(currentAssessmentEntity);
            assessment.setStudentStatusCode(StudentStatusCodes.ACTIVE.getCode());
            response.add(processStudent(assessment, null, true, true, "GRAD"));
        });
        assessmentsToOverwrite.forEach(assessment -> {
            AssessmentStudentEntity currentAssessmentStudentEntity = assessmentStudentRepository.findById(assessment.getAssessmentStudentID()).orElseThrow(() ->
                    new EntityNotFoundException(AssessmentStudentEntity.class, "AssessmentStudent", assessment.getAssessmentStudentID().toString())
            );
            assessment.setAssessmentEntity(currentAssessmentStudentEntity.getAssessmentEntity());
            response.add(processStudent(assessment, currentAssessmentStudentEntity, false, true, "GRAD"));
        });

        AssessmentEventEntity event = null;
        if(!response.stream().filter(assessment -> assessment.getAssessmentStudentValidationIssues() == null).toList().isEmpty()) {
            event = generateStudentUpdatedEvent(targetStudentID.toString());
            assessmentEventRepository.save(event);
        }
        return Pair.of(response, event);
    }

    private Set<String> validateMergeConflicts(
        Map<String, AssessmentStudentEntity> existingAssessmentStudents,
        Map<String, AssessmentStudentEntity> assessmentStudentsToMerge,
        List<AssessmentStudentListItem> response) {
        Set<String> assessmentStudentWithValidationIssues = new HashSet<>();
        existingAssessmentStudents.entrySet().stream()
            .filter(entry -> assessmentStudentsToMerge.containsKey(entry.getKey()))
            .forEach(entry -> {
                AssessmentStudentListItem existingStudentAssessment = assessmentStudentListItemMapper.toStructure(entry.getValue());
                if(existingStudentAssessment.getProficiencyScore() != null) {
                    assessmentStudentWithValidationIssues.add(entry.getKey());
                    existingStudentAssessment.setAssessmentStudentValidationIssues(List.of(
                        AssessmentStudentValidationIssue.builder()
                            .validationIssueSeverityCode(ERROR)
                            .validationIssueCode(AssessmentStudentValidationIssueTypeCode.MERGE_HAS_SCORE.getCode())
                            .validationIssueFieldCode(AssessmentStudentValidationFieldCode.PROFICIENCY_SCORE.getCode())
                            .validationMessage(AssessmentStudentValidationIssueTypeCode.MERGE_HAS_SCORE.getMessage())
                            .assessmentStudentID(existingStudentAssessment.toString())
                            .build()
                    ));
                    response.add(existingStudentAssessment);
                }
            });
        return assessmentStudentWithValidationIssues;
    }

    private AssessmentStudentEntity copyAssessmentStudentForMerge(
        AssessmentStudentEntity sourceAssessment,
        AssessmentStudentEntity existingAssessment,
        Student targetStudent,
        String targetSchool,
        AssessmentStudentMoveRequest mergeRequest,
        LocalDateTime now,
        boolean isNewAssessment) {
        var mergedAssessmentStudent = new AssessmentStudentEntity();

        // Copy all properties except IDs, metadata, and collections
        BeanUtils.copyProperties(sourceAssessment, mergedAssessmentStudent,
            ASSESSMENT_STUDENT_ID, CREATE_USER, UPDATE_USER, CREATE_DATE, UPDATE_DATE,
            "assessmentStudentComponentEntities");

        // Keep existing ID for updates, or let it be generated for new records
        if (!isNewAssessment && existingAssessment != null) {
            mergedAssessmentStudent.setAssessmentStudentID(existingAssessment.getAssessmentStudentID());
        }
        // Not all details are copied over. They need to come from target student
        mergedAssessmentStudent.setStudentID(UUID.fromString(targetStudent.getStudentID()));
        mergedAssessmentStudent.setPen(targetStudent.getPen());
        if(targetSchool != null) {
            mergedAssessmentStudent.setSchoolOfRecordSchoolID(UUID.fromString(targetSchool));
        }
        mergedAssessmentStudent.setGivenName(targetStudent.getLegalFirstName());
        mergedAssessmentStudent.setSurname(targetStudent.getLegalLastName());
        mergedAssessmentStudent.setLocalID(targetStudent.getLocalID());
        mergedAssessmentStudent.setUpdateDate(now);
        mergedAssessmentStudent.setUpdateUser(mergeRequest.getUpdateUser());

        if (isNewAssessment) {
            mergedAssessmentStudent.setCreateDate(now);
            mergedAssessmentStudent.setCreateUser(mergeRequest.getCreateUser());
        }
        // Deep copy all child entities (components, answers, choices, etc.)
        if (sourceAssessment.getAssessmentStudentComponentEntities() != null &&
            !sourceAssessment.getAssessmentStudentComponentEntities().isEmpty()) {
            Set<AssessmentStudentComponentEntity> copiedComponents = new HashSet<>();
            for (AssessmentStudentComponentEntity sourceComponent : sourceAssessment.getAssessmentStudentComponentEntities()) {
                copiedComponents.add(copyAssessmentStudentComponent(sourceComponent, mergedAssessmentStudent,
                    mergeRequest.getCreateUser(), now, mergeRequest.getUpdateUser(), now));
            }
            mergedAssessmentStudent.setAssessmentStudentComponentEntities(copiedComponents);
        }

        return mergedAssessmentStudent;
    }

    private List<AssessmentStudentEntity> prepareAssessmentsToOverwrite(
        Map<String, AssessmentStudentEntity> existingAssessmentStudents,
        Map<String, AssessmentStudentEntity> assessmentStudentsToMerge,
        Set<String> assessmentStudentWithValidationIssues,
        Student targetStudent,
        String targetSchool,
        AssessmentStudentMoveRequest mergeRequest) {
        LocalDateTime now = LocalDateTime.now();
        return existingAssessmentStudents.entrySet().stream()
            .filter(entry -> assessmentStudentsToMerge.containsKey(entry.getKey())
                && !assessmentStudentWithValidationIssues.contains(entry.getKey()))
            .map(entry -> {
                var existingAssessmentStudent = entry.getValue();
                var incomingAssessmentStudent = assessmentStudentsToMerge.get(entry.getKey());
                return copyAssessmentStudentForMerge(incomingAssessmentStudent, existingAssessmentStudent,
                    targetStudent, targetSchool, mergeRequest, now, false);
            })
            .toList();
    }

    private List<AssessmentStudentEntity> prepareAssessmentsToAdd(
        Map<String, AssessmentStudentEntity> existingAssessmentStudents,
        Map<String, AssessmentStudentEntity> assessmentStudentsToMerge,
        Student targetStudent,
        String targetSchool,
        AssessmentStudentMoveRequest mergeRequest) {
        LocalDateTime now = LocalDateTime.now();
        return assessmentStudentsToMerge.entrySet().stream()
            .filter(entry -> !existingAssessmentStudents.containsKey(entry.getKey()))
            .map(entry -> {
                var assessment = entry.getValue();
                return copyAssessmentStudentForMerge(assessment, null,
                    targetStudent, targetSchool, mergeRequest, now, true);
            })
            .toList();
    }

    private Map<String, AssessmentStudentEntity> getAssessmentStudentsAsMap(UUID studentID) {
        if(studentID != null) {
            log.debug("getAssessmentStudentsAsMap: {}", studentID);
            List<AssessmentStudentEntity> assessmentStudentEntities = assessmentStudentRepository.findByStudentID(studentID);
            log.debug("Retrieved {} assessment students for studentID = {}", assessmentStudentEntities.size(), studentID);
            return assessmentStudentEntities.stream()
                .collect(Collectors.toMap(
                    assessmentStudent -> assessmentStudent.getAssessmentEntity().getAssessmentID().toString(),
                    assessmentStudent -> assessmentStudent
                ));
        }
        return Collections.emptyMap();
    }

    private Map<String, AssessmentStudentEntity> getAssessmentStudentsToMerge(List<UUID> studentAssessmentIDs, UUID sourceStudentID) {
        List<AssessmentStudentEntity> assessmentStudentEntities = assessmentStudentRepository.findAllById(studentAssessmentIDs);
        if(assessmentStudentEntities.size() != studentAssessmentIDs.size()) {
            Set<UUID> foundIds = assessmentStudentEntities.stream()
                .map(AssessmentStudentEntity::getAssessmentStudentID)
                .collect(Collectors.toSet());
            List<UUID> missingIds = studentAssessmentIDs.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
            throw new EntityNotFoundException(AssessmentStudentEntity.class, "studentAssessmentIDs", missingIds.toString()
            );
        }
        List<AssessmentStudentEntity> mismatchedAssessmentStudents = assessmentStudentEntities.stream()
            .filter(assessmentStudent -> !sourceStudentID.equals(assessmentStudent.getStudentID()))
            .toList();

        if(!mismatchedAssessmentStudents.isEmpty()) {
            List<UUID> mismatchedIDs = mismatchedAssessmentStudents.stream()
                .map(AssessmentStudentEntity::getStudentID)
                .toList();
            throw new IllegalArgumentException(String.format("Assessment students do not belong to source student. Mismatched IDs: %s", mismatchedIDs));
        }

        return assessmentStudentEntities.stream()
            .collect(Collectors.toMap(
                assessmentStudent -> assessmentStudent.getAssessmentEntity().getAssessmentID().toString(),
                assessmentStudent -> assessmentStudent
            ));
    }

    private AssessmentStudentComponentEntity copyAssessmentStudentComponent(AssessmentStudentComponentEntity source, 
                                                                           AssessmentStudentEntity newParent,
                                                                           String createUser, LocalDateTime createDate,
                                                                           String updateUser, LocalDateTime updateDate) {
        var newComponent = new AssessmentStudentComponentEntity();
        BeanUtils.copyProperties(source, newComponent,
            "assessmentStudentComponentID", "assessmentStudentEntity", CREATE_USER, UPDATE_USER,
            CREATE_DATE, UPDATE_DATE, "assessmentStudentAnswerEntities", "assessmentStudentChoiceEntities");
        newComponent.setAssessmentStudentEntity(newParent);
        newComponent.setCreateUser(createUser);
        newComponent.setCreateDate(createDate);
        newComponent.setUpdateUser(updateUser);
        newComponent.setUpdateDate(updateDate);

        // Copy answers
        if (source.getAssessmentStudentAnswerEntities() != null && !source.getAssessmentStudentAnswerEntities().isEmpty()) {
            source.getAssessmentStudentAnswerEntities().forEach(sourceAnswer ->
                newComponent.getAssessmentStudentAnswerEntities().add(copyAssessmentStudentAnswer(sourceAnswer, newComponent,
                    createUser, createDate, updateUser, updateDate)));
        }

        // Copy choices and their question sets
        if (source.getAssessmentStudentChoiceEntities() != null && !source.getAssessmentStudentChoiceEntities().isEmpty()) {
            source.getAssessmentStudentChoiceEntities().forEach(sourceChoice ->
                newComponent.getAssessmentStudentChoiceEntities().add(copyAssessmentStudentChoice(sourceChoice, newComponent,
                    createUser, createDate, updateUser, updateDate)));
        }

        return newComponent;
    }

    private AssessmentStudentAnswerEntity copyAssessmentStudentAnswer(AssessmentStudentAnswerEntity source, 
                                                                      AssessmentStudentComponentEntity newParent,
                                                                      String createUser, LocalDateTime createDate,
                                                                      String updateUser, LocalDateTime updateDate) {
        var newAnswer = new AssessmentStudentAnswerEntity();
        BeanUtils.copyProperties(source, newAnswer,
            "assessmentStudentAnswerID", "assessmentStudentComponentEntity", CREATE_USER, UPDATE_USER, CREATE_DATE, UPDATE_DATE);
        newAnswer.setAssessmentStudentComponentEntity(newParent);
        newAnswer.setCreateUser(createUser);
        newAnswer.setCreateDate(createDate);
        newAnswer.setUpdateUser(updateUser);
        newAnswer.setUpdateDate(updateDate);
        return newAnswer;
    }

    private AssessmentStudentChoiceEntity copyAssessmentStudentChoice(AssessmentStudentChoiceEntity source, 
                                                                       AssessmentStudentComponentEntity newParent,
                                                                       String createUser, LocalDateTime createDate,
                                                                       String updateUser, LocalDateTime updateDate) {
        var newChoice = new AssessmentStudentChoiceEntity();
        BeanUtils.copyProperties(source, newChoice,
            "assessmentStudentChoiceID", "assessmentStudentComponentEntity", CREATE_USER, UPDATE_USER,
            CREATE_DATE, UPDATE_DATE, "assessmentStudentChoiceQuestionSetEntities");
        newChoice.setAssessmentStudentComponentEntity(newParent);
        newChoice.setCreateUser(createUser);
        newChoice.setCreateDate(createDate);
        newChoice.setUpdateUser(updateUser);
        newChoice.setUpdateDate(updateDate);

        // Copy question sets
        if (source.getAssessmentStudentChoiceQuestionSetEntities() != null && !source.getAssessmentStudentChoiceQuestionSetEntities().isEmpty()) {
            source.getAssessmentStudentChoiceQuestionSetEntities().forEach(sourceQuestionSet ->
                newChoice.getAssessmentStudentChoiceQuestionSetEntities().add(copyAssessmentStudentChoiceQuestionSet(sourceQuestionSet, newChoice,
                    createUser, createDate, updateUser, updateDate)));
        }

        return newChoice;
    }

    private AssessmentStudentChoiceQuestionSetEntity copyAssessmentStudentChoiceQuestionSet(AssessmentStudentChoiceQuestionSetEntity source, 
                                                                                            AssessmentStudentChoiceEntity newParent,
                                                                                            String createUser, LocalDateTime createDate,
                                                                                            String updateUser, LocalDateTime updateDate) {
        var newQuestionSet = new AssessmentStudentChoiceQuestionSetEntity();
        BeanUtils.copyProperties(source, newQuestionSet,
            "assessmentStudentChoiceQuestionSetID", "assessmentStudentChoiceEntity", CREATE_USER, UPDATE_USER, CREATE_DATE, UPDATE_DATE);
        newQuestionSet.setAssessmentStudentChoiceEntity(newParent);
        newQuestionSet.setCreateUser(createUser);
        newQuestionSet.setCreateDate(createDate);
        newQuestionSet.setUpdateUser(updateUser);
        newQuestionSet.setUpdateDate(updateDate);
        return newQuestionSet;
    }
}
