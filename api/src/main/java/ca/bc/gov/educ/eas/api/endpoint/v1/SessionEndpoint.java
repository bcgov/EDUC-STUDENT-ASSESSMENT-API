package ca.bc.gov.educ.eas.api.endpoint.v1;

import ca.bc.gov.educ.eas.api.constants.v1.URL;
import ca.bc.gov.educ.eas.api.struct.v1.Session;
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
public interface SessionEndpoint {

    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_SESSIONS')")
    @GetMapping
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    List<Session> getAllSessions();

    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_SESSIONS')")
    @GetMapping("/school-year/{schoolYear}")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    List<Session> getSessionsBySchoolYear(@PathVariable String schoolYear);

    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_SESSIONS')")
    @GetMapping("/active")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    List<Session> getActiveSessions();

    @PreAuthorize("hasAuthority('SCOPE_WRITE_EAS_SESSIONS')")
    @PutMapping("/{sessionID}")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND."), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    Session updateSession(@PathVariable UUID sessionID, @Validated @RequestBody Session session);

}
