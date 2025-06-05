package ca.bc.gov.educ.assessment.api.endpoint.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentApproval;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentSession;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Definition for assessment session management
 */
@RequestMapping(URL.SESSIONS_URL)
public interface AssessmentSessionEndpoint {

    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_SESSIONS')")
    @GetMapping
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    List<AssessmentSession> getAllSessions();

    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_SESSIONS')")
    @GetMapping("/school-year/{schoolYear}")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    List<AssessmentSession> getSessionsBySchoolYear(@PathVariable String schoolYear);

    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_SESSIONS')")
    @GetMapping("/active")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    List<AssessmentSession> getActiveSessions();

    @PreAuthorize("hasAuthority('SCOPE_WRITE_ASSESSMENT_SESSIONS')")
    @PutMapping("/{sessionID}")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND."), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    AssessmentSession updateSession(@PathVariable UUID sessionID, @Validated @RequestBody AssessmentSession assessmentSession);

    @PreAuthorize("hasAuthority('SCOPE_WRITE_ASSESSMENT_SESSIONS')")
    @PostMapping("/approval/{sessionID}")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND."), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    AssessmentApproval approveAssessmentSession(@PathVariable UUID sessionID, @Validated @RequestBody AssessmentApproval assessmentApproval);

}
