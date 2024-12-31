package ca.bc.gov.educ.eas.api.endpoint.v1;

import ca.bc.gov.educ.eas.api.constants.v1.URL;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentKeyFile;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentKeyFileUpload;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface AssessmentFileUploadEndpoint {

    @PostMapping(URL.ASSESSMENTS_KEY_URL+"/{sessionID}/file")
    //@PreAuthorize("hasAuthority('SCOPE_WRITE_EAS_SESSIONS')")
    @PreAuthorize("hasAuthority('SCOPE_WRITE_EAS_ASSESSMENT_KEYS')")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "CREATED"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    @Tag(name = "Endpoint to upload a Assessment Key file and convert to json structure.", description = "Endpoint to upload a Assessment Key file and convert to json structure")
    @Schema(name = "FileUpload", implementation = AssessmentKeyFileUpload.class)
    ResponseEntity<AssessmentKeyFile> processAssessmentKeyBatchFile(@Validated @RequestBody AssessmentKeyFileUpload fileUpload, @PathVariable(name = "sessionID") String sessionID);
}
