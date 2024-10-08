package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.endpoint.v1.SessionEndpoint;
import ca.bc.gov.educ.eas.api.mappers.SessionMapper;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.service.v1.SessionService;
import ca.bc.gov.educ.eas.api.struct.v1.Session;
import ca.bc.gov.educ.eas.api.util.RequestUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for assessment session management
 */
@RestController
@Slf4j
public class SessionController implements SessionEndpoint {

    private static final SessionMapper mapper = SessionMapper.mapper;
    @Getter(AccessLevel.PRIVATE)
    private final SessionService sessionService;

    @Autowired
    SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Retrieves all assessment sessions.
     * @return List of sessions
     */
    @Override
    public List<Session> getAllSessions() {
        return getSessionService().getAllSessions().stream().map(mapper::toStructure).collect(Collectors.toList());
    }

    /**
     * Updates the assessment session.
     * @param assessmentSessionID Identifier for assessment session
     * @param session             Modified session
     * @return Updated session
     */
    @Override
    public Session updateSession(UUID assessmentSessionID, Session session) {
        RequestUtil.setAuditColumnsForUpdate(session);
        SessionEntity sessionEntity = getSessionService().updateSession(assessmentSessionID, mapper.toEntity(session));
        return mapper.toStructure(sessionEntity);
    }
}
