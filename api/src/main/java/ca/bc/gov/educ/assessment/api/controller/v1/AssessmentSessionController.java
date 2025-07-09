package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.endpoint.v1.AssessmentSessionEndpoint;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.mappers.v1.SessionMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.service.v1.SessionService;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentApproval;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentSession;
import ca.bc.gov.educ.assessment.api.util.RequestUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * Controller for assessment session management
 */
@RestController
@Slf4j
@AllArgsConstructor
public class AssessmentSessionController implements AssessmentSessionEndpoint {

    private static final SessionMapper mapper = SessionMapper.mapper;

    @Getter(AccessLevel.PRIVATE)
    private final SessionService sessionService;

    @Override
    public List<AssessmentSession> getAllSessions() {
        return getSessionService().getAllSessions().stream().map(mapper::toStructure).collect(Collectors.toList());
    }

    @Override
    public List<AssessmentSession> getSessionsBySchoolYear(String schoolYear) {
        return getSessionService().getSessionsBySchoolYear(schoolYear.replace("-","/")).stream().map(mapper::toStructure).collect(Collectors.toList());
    }

    @Override
    public List<AssessmentSession> getActiveSessions() {
        var sessions = getSessionService().getActiveSessions();
        if(sessions == null || sessions.isEmpty()) {
            return new ArrayList<>();
        }
        return sessions.stream().map(mapper::toStructure).collect(Collectors.toList());
    }

    @Override
    public AssessmentSession updateSession(UUID sessionID, AssessmentSession assessmentSession) {
        RequestUtil.setAuditColumnsForUpdate(assessmentSession);
        AssessmentSessionEntity assessmentSessionEntity = getSessionService().updateSession(sessionID, mapper.toEntity(assessmentSession));
        return mapper.toStructure(assessmentSessionEntity);
    }

    @Override
    public AssessmentApproval approveAssessmentSession(UUID sessionID, AssessmentApproval assessmentApproval) {
        if(sessionID == null || !sessionID.toString().equalsIgnoreCase(assessmentApproval.getSessionID()) ) {
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid session ID.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }else if(StringUtils.isBlank(assessmentApproval.getApprovalStudentCertUserID())
                && StringUtils.isBlank(assessmentApproval.getApprovalAssessmentDesignUserID())
                && StringUtils.isBlank(assessmentApproval.getApprovalAssessmentAnalysisUserID())) {
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid data, at least one approval is required.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        RequestUtil.setAuditColumnsForUpdate(assessmentApproval);
        getSessionService().approveAssessment(assessmentApproval);
        return assessmentApproval;
    }

}
