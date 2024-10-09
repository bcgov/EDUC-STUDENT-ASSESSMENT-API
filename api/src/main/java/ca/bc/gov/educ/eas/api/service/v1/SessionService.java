package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
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

    @Autowired
    public SessionService(final SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Retrieves all managed assessment sessions.
     * @return List of SessionEntity
     */
    public List<SessionEntity> getAllSessions() {
        return this.getSessionRepository().findAll(Sort.by(Sort.Direction.DESC, "courseSession"));
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
}
