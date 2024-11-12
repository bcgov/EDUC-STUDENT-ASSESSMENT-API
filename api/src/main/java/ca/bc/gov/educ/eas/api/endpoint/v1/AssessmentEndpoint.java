package ca.bc.gov.educ.eas.api.endpoint.v1;

import ca.bc.gov.educ.eas.api.constants.v1.URL;
import ca.bc.gov.educ.eas.api.struct.OnCreate;
import ca.bc.gov.educ.eas.api.struct.v1.Assessment;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.groups.Default;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RequestMapping(URL.ASSESSMENTS_URL)
public interface AssessmentEndpoint {
    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_SESSIONS')")
    @GetMapping("/{assessmentID}")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
    @Tag(name= "Assessment Entity", description = "Endpoints for assessment entity.")
    Assessment getAssessment(@PathVariable UUID assessmentID);

    @GetMapping( "")
    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_SESSIONS')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
    List<Assessment> getAssessments(@RequestParam(name = "typeCode", required = false) String typeCode, @RequestParam(name="schoolYearEnd", required = false) String schoolYearEnd);

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_WRITE_EAS_SESSIONS')")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    Assessment createAssessment(@Validated({Default.class, OnCreate.class}) @RequestBody Assessment assessment);

    @PreAuthorize("hasAuthority('SCOPE_WRITE_EAS_SESSIONS')")
    @PutMapping("/{assessmentID}")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
    @Tag(name = "Assessment Entity", description = "Endpoints for assessment entity.")
    Assessment updateAssessment(@PathVariable UUID assessmentID, @Validated @RequestBody Assessment assessment);

    @PreAuthorize("hasAuthority('SCOPE_WRITE_EAS_SESSIONS')")
    @DeleteMapping("/{assessmentID}")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "NO CONTENT"), @ApiResponse(responseCode = "404", description = "NOT FOUND")})
    @Tag(name = "Assessment Entity", description = "Endpoints for assessment entity.")
    @Schema(name = "Assessment", implementation = Assessment.class)
    @ResponseStatus(NO_CONTENT)
    ResponseEntity<Void> deleteAssessment(@PathVariable UUID assessmentID);
}
