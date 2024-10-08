package ca.bc.gov.educ.eas.api.endpoint.v1;

import ca.bc.gov.educ.eas.api.constants.v1.URL;
import ca.bc.gov.educ.eas.api.struct.v1.Session;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Definition for assessment session management
 */
@RequestMapping(URL.SESSIONS_URL)
public interface SessionEndpoint {

    /**
     * Retrieves all assessment sessions maintained in data store
     * @return List of sessions
     */
    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_SESSIONS')")
    @GetMapping
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    List<Session> getAllSessions();

    /**
     * Updates the assessment session
     * @param assessmentSessionID Identifier for assessment session
     * @param session Modified session
     * @return Updated session
     */
    @PreAuthorize("hasAuthority('SCOPE_WRITE_EAS_SESSIONS')")
    @PutMapping("/{assessmentSessionID}")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND."), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    Session updateSession(@PathVariable String assessmentSessionID, @Validated @RequestBody Session session);

}
