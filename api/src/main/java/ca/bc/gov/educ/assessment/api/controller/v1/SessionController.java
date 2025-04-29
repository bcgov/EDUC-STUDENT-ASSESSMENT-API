package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.endpoint.v1.SessionEndpoint;
import ca.bc.gov.educ.assessment.api.mappers.v1.SessionMapper;
import ca.bc.gov.educ.assessment.api.model.v1.SessionEntity;
import ca.bc.gov.educ.assessment.api.service.v1.SessionService;
import ca.bc.gov.educ.assessment.api.struct.v1.Session;
import ca.bc.gov.educ.assessment.api.util.RequestUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller for assessment session management
 */
@RestController
@Slf4j
@AllArgsConstructor
public class SessionController implements SessionEndpoint {

    private static final SessionMapper mapper = SessionMapper.mapper;

    @Getter(AccessLevel.PRIVATE)
    private final SessionService sessionService;

    @Override
    public List<Session> getAllSessions() {
        return getSessionService().getAllSessions().stream().map(mapper::toStructure).collect(Collectors.toList());
    }

    @Override
    public List<Session> getSessionsBySchoolYear(String schoolYear) {
        return getSessionService().getSessionsBySchoolYear(schoolYear.replace("-","/")).stream().map(mapper::toStructure).collect(Collectors.toList());
    }

    @Override
    public List<Session> getActiveSessions() {
        return getSessionService().getActiveSessions().stream().map(mapper::toStructure).collect(Collectors.toList());
    }

    @Override
    public Session updateSession(UUID sessionID, Session session) {
        RequestUtil.setAuditColumnsForUpdate(session);
        SessionEntity sessionEntity = getSessionService().updateSession(sessionID, mapper.toEntity(session));
        return mapper.toStructure(sessionEntity);
    }

}
