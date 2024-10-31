package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentSessionCriteriaEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private final AssessmentService assessmentService;

    @Autowired
    public SessionService(final SessionRepository sessionRepository, AssessmentSessionCriteriaRepository assessmentSessionCriteriaRepository, AssessmentService assessmentService) {
        this.sessionRepository = sessionRepository;
        this.assessmentSessionCriteriaRepository = assessmentSessionCriteriaRepository;
        this.assessmentService = assessmentService;
    }

    /**
     * Retrieves all managed assessment sessions.
     * @return List of SessionEntity
     */
    public List<SessionEntity> getAllSessions() {
        return this.getSessionRepository().findAll(Sort.by(Sort.Direction.DESC, "activeFromDate"));
    }

    /**
     * Updates the assessment session.
     * @param sessionID  Assessment SessionId
     * @param updatedSessionEntity Updated session object
     * @return Persisted SessionEntity
     */
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
        List<AssessmentSessionCriteriaEntity> activeSessionCriteria = assessmentSessionCriteriaRepository.findAllByEffectiveDateGreaterThanEqualAndExpiryDateLessThanEqual(LocalDateTime.now(), LocalDateTime.now());
        List<SessionEntity> newSessions = populateSessionEntities(activeSessionCriteria, schoolYearStart);
        List<SessionEntity> savedSessions = sessionRepository.saveAll(newSessions);

        assessmentService.createAllAssessmentsForSchoolYear(savedSessions);
    }

    public List<SessionEntity> populateSessionEntities(List<AssessmentSessionCriteriaEntity> sessionTypes, int schoolYearStart){
        List<SessionEntity> newSessionEntities = new ArrayList<>();
        LocalDateTime activeFromDate = LocalDateTime.of(schoolYearStart, 10, 1, 0, 0);

        for(AssessmentSessionCriteriaEntity sessionType : sessionTypes){
            String sessionMonth = String.valueOf(sessionType.getSessionEnd().getMonth());
            int sessionYear = sessionMonth.equalsIgnoreCase("11") ? schoolYearStart : schoolYearStart + 1;
            LocalDateTime endOfSessionDate = LocalDateTime.of(sessionYear, sessionType.getSessionEnd().getMonth(), sessionType.getSessionEnd().getDayOfMonth(), 0, 0);

            SessionEntity session = SessionEntity.builder()
                    .schoolYear(String.valueOf(schoolYearStart + 1))
                    .courseYear(String.valueOf(sessionYear))
                    .courseMonth(sessionMonth)
                    .activeFromDate(activeFromDate)
                    .activeUntilDate(endOfSessionDate)
                    .createUser("EAS_API")
                    .createDate(LocalDateTime.now())
                    .updateUser("EAS_API")
                    .updateDate(LocalDateTime.now())
                    .build();

            newSessionEntities.add(session);
        }

        return newSessionEntities;
    }
}
