package ca.bc.gov.educ.assessment.api.endpoint.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultFileUpload;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultsSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping(URL.BASE_URL)
public interface FileUploadEndpoint {
    @PostMapping("/{sessionID}/key-file")
    @PreAuthorize("hasAuthority('SCOPE_WRITE_ASSESSMENT_FILES')")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    @Tag(name = "Endpoint to upload assessment keys and convert to json structure.", description = "Endpoint to upload assessment keys and convert to json structure")
    @Schema(name = "FileUpload", implementation = AssessmentKeyFileUpload.class)
    ResponseEntity<Void> processAssessmentKeysFile(@Validated @RequestBody AssessmentKeyFileUpload fileUpload, @PathVariable(name = "sessionID") UUID sessionID);

    @PostMapping("/{session}/results-file")
    @PreAuthorize("hasAuthority('SCOPE_WRITE_ASSESSMENT_FILES')")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    @Tag(name = "Endpoint to upload assessment results and convert to json structure.", description = "Endpoint to upload assessment results and convert to json structure")
    @Schema(name = "FileUpload", implementation = AssessmentResultFileUpload.class)
    ResponseEntity<Void> processAssessmentResultsFile(@Validated @RequestBody AssessmentResultFileUpload fileUpload, @PathVariable(name = "session") String session);

    @GetMapping("/{sessionID}/result-summary")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_SESSIONS')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    List<AssessmentResultsSummary> getAssessmentResultsUploadSummary(@PathVariable UUID sessionID);

}
