package ca.bc.gov.educ.assessment.api.controller.v1;


import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.endpoint.v1.ReportsEndoint;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.service.v1.CSVReportService;
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

    @Override
    public DownloadableReportResponse getDownloadableReport(UUID sessionID, String type) {
        Optional<AssessmentReportTypeCode> code = AssessmentReportTypeCode.findByValue(type);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        return switch (code.get()) {
            case ALL_SESSION_REGISTRATIONS -> ministryReportsService.generateSessionRegistrationsReport(sessionID);
            case PEN_MERGES -> ministryReportsService.generatePenMergesReport();
            default -> new DownloadableReportResponse();
        };
    }


}
