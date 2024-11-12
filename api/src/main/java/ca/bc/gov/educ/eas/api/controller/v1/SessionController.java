package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.endpoint.v1.SessionEndpoint;
import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentMapper;
import ca.bc.gov.educ.eas.api.mappers.v1.SessionMapper;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.service.v1.SessionService;
import ca.bc.gov.educ.eas.api.struct.v1.Assessment;
import ca.bc.gov.educ.eas.api.struct.v1.Session;
import ca.bc.gov.educ.eas.api.util.RequestUtil;
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
    private static final AssessmentMapper assessmentMapper = AssessmentMapper.mapper;
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
    public Session updateSession(UUID sessionID, Session session) {
        RequestUtil.setAuditColumnsForUpdate(session);
        SessionEntity sessionEntity = getSessionService().updateSession(sessionID, mapper.toEntity(session));
        return mapper.toStructure(sessionEntity);
    }

    @Override
    public List<Assessment> getAssessmentsForSession(UUID sessionID) {
        return getSessionService().getAssessmentsForSession(sessionID).stream().map(assessmentMapper::toStructure).collect(Collectors.toList());
    }
}
