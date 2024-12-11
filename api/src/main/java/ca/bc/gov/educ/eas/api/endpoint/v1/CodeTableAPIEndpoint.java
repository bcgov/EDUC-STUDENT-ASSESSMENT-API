package ca.bc.gov.educ.eas.api.endpoint.v1;

import ca.bc.gov.educ.eas.api.constants.v1.URL;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentTypeCode;
import ca.bc.gov.educ.eas.api.struct.v1.ProvincialSpecialCaseCode;
import ca.bc.gov.educ.eas.api.struct.v1.ValidationIssueTypeCode;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

public interface CodeTableAPIEndpoint {

    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_SESSIONS')")
    @GetMapping(URL.ASSESSMENT_TYPE_CODE_URL)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Schema(name = "AssessmentTypeCode", implementation = AssessmentTypeCode.class)
    List<AssessmentTypeCode> getAssessTypeCodes();

    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_SESSIONS')")
    @GetMapping(URL.PROVINCIAL_SPECIALCASE_CODE_URL)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Schema(name = "AssessmentTypeCode", implementation = AssessmentTypeCode.class)
    List<ProvincialSpecialCaseCode> getProvincialSpecialCaseCodes();

    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_SESSIONS')")
    @GetMapping(URL.VALIDATION_ISSUE_TYPE_CODE_URL)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Schema(name = "AssessmentTypeCode", implementation = ValidationIssueTypeCode.class)
    List<ValidationIssueTypeCode> getAssessmentStudentValidationIssueTypeCodes();
}
