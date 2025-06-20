package ca.bc.gov.educ.assessment.api.controller.v1;


import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.endpoint.v1.ReportsEndoint;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.CSVReportService;
import ca.bc.gov.educ.assessment.api.service.v1.XAMFileService;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ReportsController implements ReportsEndoint {

    private final CSVReportService ministryReportsService;
    private final XAMFileService xamFileService;
    private final RestUtils restUtils;

    @Override
    public DownloadableReportResponse getDownloadableReport(UUID sessionID, String type) {
        Optional<AssessmentReportTypeCode> code = AssessmentReportTypeCode.findByValue(type);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        return switch (code.get()) {
            case ALL_SESSION_REGISTRATIONS -> ministryReportsService.generateSessionRegistrationsReport(sessionID);
            case ATTEMPTS -> ministryReportsService.generateNumberOfAttemptsReport(sessionID);
            case PEN_MERGES -> ministryReportsService.generatePenMergesReport();
            default -> new DownloadableReportResponse();
        };
    }

    @Override
    public DownloadableReportResponse getDownloadableReportForSchool(UUID sessionID, UUID schoolID) {
        var schoolTombstone = this.restUtils.getSchoolBySchoolID(schoolID.toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class));

        try {
            return this.xamFileService.generateXamReport(sessionID, schoolTombstone);
        } catch (Exception ex) {
            log.error("Error generating XAM report", ex);
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Error generating report.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }
    }
}
