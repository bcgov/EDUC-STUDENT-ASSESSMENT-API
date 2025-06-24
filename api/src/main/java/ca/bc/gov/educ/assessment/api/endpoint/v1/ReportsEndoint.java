package ca.bc.gov.educ.assessment.api.endpoint.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RequestMapping( URL.BASE_URL_REPORT)
public interface ReportsEndoint {

    @GetMapping("/{sessionID}/{type}/download")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORTS')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    DownloadableReportResponse getDownloadableReport(@PathVariable UUID sessionID, @PathVariable(name = "type") String type);

    @GetMapping("/{sessionID}/school/{schoolID}/{type}/download")
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_REPORTS')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST")})
    DownloadableReportResponse getDownloadableReportForSchool(@PathVariable UUID sessionID, @PathVariable UUID schoolID, @PathVariable(name = "type") String type);
}
