package ca.bc.gov.educ.assessment.api.endpoint.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.SimpleHeadcountResultsTable;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@RequestMapping( URL.BASE_URL_REPORT)
public interface ReportsEndpoint {

    @GetMapping("/{sessionID}/{type}/download/{updateUser}")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional()
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    DownloadableReportResponse getDownloadableReport(@PathVariable UUID sessionID, @PathVariable(name = "type") String type,  @PathVariable(name = "updateUser") String updateUser);

    @GetMapping("/{sessionID}/school/{schoolID}/{type}/download")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    DownloadableReportResponse getDownloadableReportForSchool(@PathVariable UUID sessionID, @PathVariable UUID schoolID, @PathVariable(name = "type") String type);

    @GetMapping("/{sessionID}/{type}")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    SimpleHeadcountResultsTable getSummaryReports(@PathVariable UUID sessionID, @PathVariable(name = "type") String type);

    @GetMapping("/student/{studentID}/{type}/download")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    DownloadableReportResponse getStudentReport(@PathVariable UUID studentID, @PathVariable String type);

    @GetMapping("/student-pen/{pen}/{type}/download")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    DownloadableReportResponse getStudentReport(@PathVariable String pen, @PathVariable String type);

    @GetMapping("/assessment-students/search/download")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_STUDENT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    DownloadableReportResponse getAssessmentStudentSearchReport(
            @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);
}
