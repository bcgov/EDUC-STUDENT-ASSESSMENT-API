package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidParameterException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentCriteriaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionCriteriaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.SessionApprovalOrchestrator;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentApproval;
import ca.bc.gov.educ.assessment.api.util.SchoolYearUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * This class encapsulates assessment session functionalities.
 * Supported CRUD operations: SELECT ALL and UPDATE.
 */
@Service
@Slf4j
public class SessionService {

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentSessionRepository assessmentSessionRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentSessionCriteriaRepository assessmentSessionCriteriaRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentRepository assessmentRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentService assessmentService;

    @Getter(AccessLevel.PRIVATE)
    private final SessionApprovalOrchestrator sessionApprovalOrchestrator;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentStudentRepository assessmentStudentRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;

    @Getter(AccessLevel.PRIVATE)
    private final SagaService sagaService;

    private static final String ASSESSMENT_API = "ASSESSMENT_API";

    @Autowired
    public SessionService(final AssessmentSessionRepository assessmentSessionRepository, AssessmentSessionCriteriaRepository assessmentSessionCriteriaRepository, AssessmentRepository assessmentRepository, AssessmentService assessmentService, SessionApprovalOrchestrator sessionApprovalOrchestrator, AssessmentStudentRepository assessmentStudentRepository, AssessmentStudentHistoryRepository assessmentStudentHistoryRepository, SagaService sagaService) {
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentSessionCriteriaRepository = assessmentSessionCriteriaRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentService = assessmentService;
        this.sessionApprovalOrchestrator = sessionApprovalOrchestrator;
        this.assessmentStudentRepository = assessmentStudentRepository;
        this.assessmentStudentHistoryRepository = assessmentStudentHistoryRepository;
        this.sagaService = sagaService;
    }

    public List<AssessmentSessionEntity> getAllSessions() {
        return this.getAssessmentSessionRepository().findAllByActiveFromDateLessThanEqualOrderByActiveUntilDateDesc(LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTransferRegistrationsUser(UUID sessionID, String userID, AssessmentReportTypeCode reportTypeCode) {
        var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "sessionID", sessionID.toString()));
        switch (reportTypeCode) {
            case ALL_SESSION_REGISTRATIONS:
                var currentDate = LocalDateTime.now();
                log.info("Updating download date for all non-exempted assessment students in session {}", sessionID);
                assessmentStudentRepository.updateDownloadDataAllByAssessmentSessionAndNoExemption(sessionID, currentDate, userID, currentDate);
                log.info("Completed updating download date for session {}", sessionID);
                log.info("Inserting history records for all non-exempted assessment students in session {}", sessionID);
                assessmentStudentHistoryRepository.insertHistoryForDownloadDateUpdate(sessionID, userID, currentDate);
                log.info("Completed updating download date and inserting history records for session {}", sessionID);
                session.setAssessmentRegistrationsExportUserID(userID);
                session.setAssessmentRegistrationsExportDate(LocalDateTime.now());
                break;
            case ATTEMPTS:
                session.setSessionWritingAttemptsExportUserID(userID);
                session.setSessionWritingAttemptsExportDate(LocalDateTime.now());
                break;
            case PEN_MERGES:
                session.setPenMergesExportUserID(userID);
                session.setPenMergesExportDate(LocalDateTime.now());
                break;
            default:
                break;
        }
        session.setUpdateUser(userID);
        session.setUpdateDate(LocalDateTime.now());
        assessmentSessionRepository.save(session);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public AssessmentSessionEntity approveAssessment(final AssessmentApproval assessmentApproval) {
        AssessmentSessionEntity assessmentSession = assessmentSessionRepository.findById(UUID.fromString(assessmentApproval.getSessionID())).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "sessionID", assessmentApproval.getSessionID()));

        // todo For GENERATE_XAM_FILES saga, sessionID is stored in assessmentStudentID field (see SessionApprovalOrchestrator.startXamFileGenerationSaga)
        // we need to refactor this to correctly use a column for assessment assessmentSession id
        var existingSaga = sagaService.findByAssessmentStudentIDAndSagaNameAndStatusNot(assessmentSession.getSessionID(), SagaEnum.GENERATE_XAM_FILES.toString(), SagaStatusEnum.COMPLETED.toString());
        if (existingSaga.isPresent()) {
            log.warn("Session approval saga already running for assessmentSession {}", assessmentSession.getSessionID());
            throw new InvalidPayloadException(ApiError.builder()
                    .timestamp(LocalDateTime.now())
                    .message("Session approval is already in progress. Please wait for the current process to complete.")
                    .status(CONFLICT)
                    .build());
        }

        if(StringUtils.isNotBlank(assessmentApproval.getApprovalStudentCertUserID()) && StringUtils.isNotBlank(assessmentSession.getApprovalStudentCertUserID())){
            throw new InvalidParameterException("Assessment assessmentSession has already been approved by Student Cert User");
        }else if(StringUtils.isNotBlank(assessmentApproval.getApprovalStudentCertUserID())){
            assessmentSession.setApprovalStudentCertUserID(assessmentApproval.getApprovalStudentCertUserID());
            assessmentSession.setApprovalStudentCertSignDate(LocalDateTime.now());
        }else if(StringUtils.isNotBlank(assessmentApproval.getApprovalAssessmentDesignUserID()) && StringUtils.isNotBlank(assessmentSession.getApprovalAssessmentDesignUserID())){
            throw new InvalidParameterException("Assessment assessmentSession has already been approved by Assessment Design User");
        }else if(StringUtils.isNotBlank(assessmentApproval.getApprovalAssessmentDesignUserID())){
            assessmentSession.setApprovalAssessmentDesignUserID(assessmentApproval.getApprovalAssessmentDesignUserID());
            assessmentSession.setApprovalAssessmentDesignSignDate(LocalDateTime.now());
        }else if(StringUtils.isNotBlank(assessmentApproval.getApprovalAssessmentAnalysisUserID()) && StringUtils.isNotBlank(assessmentSession.getApprovalAssessmentAnalysisUserID())){
            throw new InvalidParameterException("Assessment assessmentSession has already been approved by Assessment Analyst User");
        }else if(StringUtils.isNotBlank(assessmentApproval.getApprovalAssessmentAnalysisUserID())){
            assessmentSession.setApprovalAssessmentAnalysisUserID(assessmentApproval.getApprovalAssessmentAnalysisUserID());
            assessmentSession.setApprovalAssessmentAnalysisSignDate(LocalDateTime.now());
        }

        assessmentSession.setActiveUntilDate(LocalDateTime.now());

        AssessmentSessionEntity savedAssessmentSession = assessmentSessionRepository.save(assessmentSession);

        if(StringUtils.isNotBlank(savedAssessmentSession.getApprovalStudentCertUserID())
                && StringUtils.isNotBlank(savedAssessmentSession.getApprovalAssessmentDesignUserID())
                && StringUtils.isNotBlank(savedAssessmentSession.getApprovalAssessmentAnalysisUserID())) {
            log.info("All three signoffs present for assessmentSession {}. Triggering generate XAM file saga.", savedAssessmentSession.getSessionID());
            try {
                sessionApprovalOrchestrator.startXamFileGenerationSaga(savedAssessmentSession.getSessionID());
            } catch (JsonProcessingException e) {
                log.error("Error starting XAM file generation saga for assessmentSession {}: {}", savedAssessmentSession.getSessionID(), e.getMessage());
            }
        }

        return savedAssessmentSession;
    }

    public List<AssessmentSessionEntity> getSessionsBySchoolYear(String schoolYear) {
        return this.getAssessmentSessionRepository().findBySchoolYear(schoolYear);
    }

    public List<AssessmentSessionEntity> getActiveSessions() {
        return this.getAssessmentSessionRepository().findAllByActiveFromDateLessThanEqualAndActiveUntilDateGreaterThanEqualAndCompletionDateIsNull(LocalDateTime.now(), LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentSessionEntity updateSession(UUID sessionID, AssessmentSessionEntity updatedAssessmentSessionEntity) {
        val optionalSession = getAssessmentSessionRepository().findById(sessionID);
        if (optionalSession.isPresent()) {
            AssessmentSessionEntity assessmentSessionEntity = optionalSession.get();
            log.debug("Assessment session update, current :: {}, new :: {}", assessmentSessionEntity, updatedAssessmentSessionEntity);
            assessmentSessionEntity.setActiveFromDate(updatedAssessmentSessionEntity.getActiveFromDate());
            assessmentSessionEntity.setActiveUntilDate(updatedAssessmentSessionEntity.getActiveUntilDate());
            return getAssessmentSessionRepository().save(assessmentSessionEntity);
        } else {
            throw new EntityNotFoundException(AssessmentSessionEntity.class, "SessionEntity", sessionID.toString());
        }
    }

    public void createAllAssessmentSessionsForSchoolYear(int schoolYearStart){
        List<AssessmentSessionCriteriaEntity> activeSessionCriteria = assessmentSessionCriteriaRepository.findAllByEffectiveDateLessThanEqualAndExpiryDateGreaterThanEqual(LocalDateTime.now(), LocalDateTime.now());
        List<AssessmentSessionEntity> newSessions = populateSessionEntities(activeSessionCriteria, schoolYearStart);
        assessmentSessionRepository.saveAll(newSessions);
    }

    public List<AssessmentSessionEntity> populateSessionEntities(List<AssessmentSessionCriteriaEntity> sessionTypes, int schoolYearStart){
        List<AssessmentSessionEntity> newSessionEntities = new ArrayList<>();
        LocalDateTime activeFromDate = LocalDateTime.of(schoolYearStart, 10, 1, 0, 0);

        for(AssessmentSessionCriteriaEntity sessionType : sessionTypes){
            String sessionMonth = StringUtils.leftPad(sessionType.getSessionStartMonth(),2, "0");
            int sessionYear = sessionMonth.equalsIgnoreCase("11") ? schoolYearStart : schoolYearStart + 1;
            LocalDateTime endOfSessionDate = LocalDateTime.of(sessionYear, sessionType.getSessionEnd().getMonth(), sessionType.getSessionEnd().getDayOfMonth(), 0, 0);

            AssessmentSessionEntity session = AssessmentSessionEntity.builder()
                    .schoolYear(SchoolYearUtil.generateSchoolYearString(schoolYearStart))
                    .courseYear(String.valueOf(sessionYear))
                    .courseMonth(sessionMonth)
                    .activeFromDate(activeFromDate)
                    .activeUntilDate(endOfSessionDate)
                    .createUser(ASSESSMENT_API)
                    .createDate(LocalDateTime.now())
                    .updateUser(ASSESSMENT_API)
                    .updateDate(LocalDateTime.now())
                    .build();

            Set<AssessmentEntity> assessments = populateAssessmentsForSession(sessionType, session);
            session.setAssessments(assessments);
            newSessionEntities.add(session);
        }

        return newSessionEntities;
    }

    private Set<AssessmentEntity> populateAssessmentsForSession(AssessmentSessionCriteriaEntity sessionCriterion, AssessmentSessionEntity session){
        Set<AssessmentEntity> newAssessments = new HashSet<>();

        for (AssessmentCriteriaEntity assessmentType : sessionCriterion.getAssessmentCriteriaEntities()){
            if(assessmentType.getExpiryDate().isAfter(LocalDateTime.now()) && assessmentType.getEffectiveDate().isBefore(LocalDateTime.now())){
                AssessmentEntity newAssessment = AssessmentEntity.builder()
                        .assessmentTypeCode(assessmentType.getAssessmentTypeCode())
                        .assessmentSessionEntity(session)
                        .createUser(ASSESSMENT_API)
                        .createDate(LocalDateTime.now())
                        .updateUser(ASSESSMENT_API)
                        .updateDate(LocalDateTime.now())
                        .build();

                newAssessments.add(newAssessment);
            }
        }

        return newAssessments;
    }
}
