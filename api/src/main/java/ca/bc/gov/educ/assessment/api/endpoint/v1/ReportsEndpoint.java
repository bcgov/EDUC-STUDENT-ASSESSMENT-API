package ca.bc.gov.educ.assessment.api.endpoint.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.SimpleHeadcountResultsTable;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RequestMapping(URL.BASE_URL_REPORT)
public interface ReportsEndpoint {

    @GetMapping("/{sessionID}/{type}/download/{updateUser}")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional()
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    DownloadableReportResponse getDownloadableReport(@PathVariable UUID sessionID, @PathVariable(name = "type") String type,  @PathVariable(name = "updateUser") String updateUser);

    @GetMapping("/{sessionID}/randomSessionSchoolsZip")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional()
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    ResponseEntity<InputStreamResource> getDownloadableRandomZip(@PathVariable UUID sessionID);

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
    void getAssessmentStudentSearchReport(@RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson, HttpServletResponse response) throws IOException;

    @GetMapping("/assessment-registrations/search/download")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_STUDENT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    void getAssessmentRegistrationSearchReport(@RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson, HttpServletResponse response) throws IOException;

    @GetMapping("/school/{schoolID}/assessment-completions/current-students/download")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    void getAssessmentCompletionCurrentStudentsReportForSchool(@PathVariable UUID schoolID, HttpServletResponse response) throws IOException;

    @GetMapping("/district/{districtID}/assessment-completions/current-students/download")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    void getAssessmentCompletionCurrentStudentsReportForDistrict(@PathVariable UUID districtID, HttpServletResponse response) throws IOException;

    @GetMapping("/{sessionID}/school/{schoolID}/results/available")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    boolean checkSchoolReportAvailability(@PathVariable UUID sessionID, @PathVariable UUID schoolID, @RequestParam(required = false) String assessmentTypeCode);

    @GetMapping("/{sessionID}/{type}/available")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    boolean checkSessionReportAvailability(@PathVariable UUID sessionID, @PathVariable(name = "type") String type);

    @GetMapping("/{sessionID}/school/{schoolID}/{type}/available")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    boolean checkSchoolReportTypeAvailability(@PathVariable UUID sessionID, @PathVariable UUID schoolID, @PathVariable(name = "type") String type);

    @GetMapping("/student/{studentID}/{type}/available")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    boolean checkStudentReportAvailability(@PathVariable UUID studentID, @PathVariable(name = "type") String type);

    @GetMapping("/{sessionID}/district/{districtID}/schools-with-results")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    List<UUID> getDistrictSchoolsWithResults(@PathVariable UUID sessionID, @PathVariable UUID districtID);

}
