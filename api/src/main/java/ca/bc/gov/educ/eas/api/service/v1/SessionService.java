package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentCriteriaEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentSessionCriteriaEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentSessionCriteriaRepository;
import ca.bc.gov.educ.eas.api.repository.v1.SessionRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * This class encapsulates assessment session functionalities.
 * Supported CRUD operations: SELECT ALL and UPDATE.
 */
@Service
@Slf4j
public class SessionService {

    @Getter(AccessLevel.PRIVATE)
    private final SessionRepository sessionRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentSessionCriteriaRepository assessmentSessionCriteriaRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentRepository assessmentRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentService assessmentService;

    private static final String EAS_API = "EAS_API";

    @Autowired
    public SessionService(final SessionRepository sessionRepository, AssessmentSessionCriteriaRepository assessmentSessionCriteriaRepository, AssessmentRepository assessmentRepository, AssessmentService assessmentService) {
        this.sessionRepository = sessionRepository;
        this.assessmentSessionCriteriaRepository = assessmentSessionCriteriaRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentService = assessmentService;
    }

    public List<SessionEntity> getAllSessions() {
        return this.getSessionRepository().findAllByActiveFromDateLessThanEqualOrderByActiveUntilDateDesc(LocalDateTime.now());
    }

    public List<SessionEntity> getSessionsBySchoolYear(String schoolYear) {
        return this.getSessionRepository().findBySchoolYear(schoolYear);
    }

    public List<SessionEntity> getActiveSessions() {
        return this.getSessionRepository().findAllByActiveFromDateLessThanEqualAndActiveUntilDateGreaterThanEqual(LocalDateTime.now(), LocalDateTime.now());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SessionEntity updateSession(UUID sessionID, SessionEntity updatedSessionEntity) {
        val optionalSession = getSessionRepository().findById(sessionID);
        if (optionalSession.isPresent()) {
            SessionEntity sessionEntity = optionalSession.get();
            log.debug("Assessment session update, current :: {}, new :: {}", sessionEntity, updatedSessionEntity);
            sessionEntity.setActiveFromDate(updatedSessionEntity.getActiveFromDate());
            sessionEntity.setActiveUntilDate(updatedSessionEntity.getActiveUntilDate());
            return getSessionRepository().save(sessionEntity);
        } else {
            throw new EntityNotFoundException(SessionEntity.class, "SessionEntity", sessionID.toString());
        }
    }

    public void createAllAssessmentSessionsForSchoolYear(int schoolYearStart){
        List<AssessmentSessionCriteriaEntity> activeSessionCriteria = assessmentSessionCriteriaRepository.findAllByEffectiveDateLessThanEqualAndExpiryDateGreaterThanEqual(LocalDateTime.now(), LocalDateTime.now());
        List<SessionEntity> newSessions = populateSessionEntities(activeSessionCriteria, schoolYearStart);
        sessionRepository.saveAll(newSessions);
    }

    public List<SessionEntity> populateSessionEntities(List<AssessmentSessionCriteriaEntity> sessionTypes, int schoolYearStart){
        List<SessionEntity> newSessionEntities = new ArrayList<>();
        LocalDateTime activeFromDate = LocalDateTime.of(schoolYearStart, 10, 1, 0, 0);

        for(AssessmentSessionCriteriaEntity sessionType : sessionTypes){
            String sessionMonth = String.valueOf(sessionType.getSessionEnd().getMonthValue());
            int sessionYear = sessionMonth.equalsIgnoreCase("11") ? schoolYearStart : schoolYearStart + 1;
            LocalDateTime endOfSessionDate = LocalDateTime.of(sessionYear, sessionType.getSessionEnd().getMonth(), sessionType.getSessionEnd().getDayOfMonth(), 0, 0);

            SessionEntity session = SessionEntity.builder()
                    .schoolYear(String.valueOf(schoolYearStart + 1))
                    .courseYear(String.valueOf(sessionYear))
                    .courseMonth(sessionMonth)
                    .activeFromDate(activeFromDate)
                    .activeUntilDate(endOfSessionDate)
                    .createUser(EAS_API)
                    .createDate(LocalDateTime.now())
                    .updateUser(EAS_API)
                    .updateDate(LocalDateTime.now())
                    .build();

            Set<AssessmentEntity> assessments = populateAssessmentsForSession(sessionType, session);
            session.setAssessments(assessments);
            newSessionEntities.add(session);
        }

        return newSessionEntities;
    }

    private Set<AssessmentEntity> populateAssessmentsForSession(AssessmentSessionCriteriaEntity sessionCriterion, SessionEntity session){
        Set<AssessmentEntity> newAssessments = new HashSet<>();

        for (AssessmentCriteriaEntity assessmentType : sessionCriterion.getAssessmentCriteriaEntities()){
            if(assessmentType.getExpiryDate().isAfter(LocalDateTime.now()) && assessmentType.getEffectiveDate().isBefore(LocalDateTime.now())){
                AssessmentEntity newAssessment = AssessmentEntity.builder()
                        .assessmentTypeCode(assessmentType.getAssessmentTypeCodeEntity().getAssessmentTypeCode())
                        .sessionEntity(session)
                        .createUser(EAS_API)
                        .createDate(LocalDateTime.now())
                        .updateUser(EAS_API)
                        .updateDate(LocalDateTime.now())
                        .build();

                newAssessments.add(newAssessment);
            }
        }

        return newAssessments;
    }
}
