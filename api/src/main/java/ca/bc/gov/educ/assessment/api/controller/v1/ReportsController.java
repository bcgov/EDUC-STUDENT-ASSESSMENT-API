package ca.bc.gov.educ.assessment.api.controller.v1;


import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.endpoint.v1.ReportsEndpoint;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.reports.SchoolStudentsByAssessmentReportService;
import ca.bc.gov.educ.assessment.api.reports.SchoolStudentsInSessionReportService;
import ca.bc.gov.educ.assessment.api.service.v1.*;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.SimpleHeadcountResultsTable;
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
public class ReportsController implements ReportsEndpoint {

    private final AssessmentStudentService assessmentStudentService;
    private final SchoolStudentsInSessionReportService schoolStudentsInSessionReportService;
    private final SchoolStudentsByAssessmentReportService schoolStudentsByAssessmentReportService;
    private final CSVReportService csvReportService;
    private final XAMFileService xamFileService;
    private final SummaryReportService summaryReportService;
    private final SessionService sessionService;

    @Override
    public DownloadableReportResponse getDownloadableReport(UUID sessionID, String type, String updateUser) {
        Optional<AssessmentReportTypeCode> code = AssessmentReportTypeCode.findByValue(type);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        switch (code.get()) {
            case ALL_SESSION_REGISTRATIONS:
                var registrations = csvReportService.generateSessionRegistrationsReport(sessionID);
                sessionService.recordTransferRegistrationsUser(sessionID, updateUser, AssessmentReportTypeCode.ALL_SESSION_REGISTRATIONS);
                return registrations;
            case ATTEMPTS:
                sessionService.recordTransferRegistrationsUser(sessionID, updateUser, AssessmentReportTypeCode.ATTEMPTS);
                return csvReportService.generateNumberOfAttemptsReport(sessionID);
            case PEN_MERGES:
                sessionService.recordTransferRegistrationsUser(sessionID, updateUser, AssessmentReportTypeCode.PEN_MERGES);
                return csvReportService.generatePenMergesReport();
            case ALL_DETAILED_STUDENTS_IN_SESSION_CSV:
                return csvReportService.generateAllDetailedStudentsInSession(sessionID);
            case SUMMARY_BY_GRADE_FOR_SESSION:
                return csvReportService.generateSummaryResultsByGradeInSession(sessionID);
            case SUMMARY_BY_FORM_FOR_SESSION:
                return csvReportService.generateSummaryByFormInSession(sessionID);
            case REGISTRATION_DETAIL_CSV:
                return csvReportService.generateRegistrationDetailReport(sessionID);
            case PEN_ISSUES_CSV:
                return csvReportService.generatePenIssuesReport(sessionID);
            case REGISTRATION_SUMMARY_BY_SCHOOL:
                return csvReportService.generateAssessmentRegistrationTotalsBySchoolReport(sessionID);
            default:
                return new DownloadableReportResponse();
        }
    }

    @Override
    public DownloadableReportResponse getDownloadableReportForSchool(UUID sessionID, UUID schoolID, String type) {
        Optional<AssessmentReportTypeCode> code = AssessmentReportTypeCode.findByValue(type);

        if (code.isEmpty()) {
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        switch (code.get()) {
            case XAM_FILE:
                try {
                    return this.xamFileService.generateXamReport(sessionID, schoolID);
                } catch (Exception ex) {
                    log.error("Error generating XAM report", ex);
                    ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Error generating report.").status(BAD_REQUEST).build();
                    throw new InvalidPayloadException(error);
                }
            case SESSION_RESULTS:
                return csvReportService.generateSessionResultsBySchoolReport(sessionID, schoolID);
            case SCHOOL_STUDENTS_IN_SESSION:
                return schoolStudentsInSessionReportService.generateSchoolStudentsInSessionReport(sessionID, schoolID);
            case SCHOOL_STUDENTS_BY_ASSESSMENT:
                return schoolStudentsByAssessmentReportService.generateSchoolStudentsByAssessmentReport(sessionID, schoolID);
            default:
                return new DownloadableReportResponse();
        }
    }

    @Override
    public SimpleHeadcountResultsTable getSummaryReports(UUID sessionID, String type) {
        Optional<AssessmentReportTypeCode> code = AssessmentReportTypeCode.findByValue(type);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        return switch(code.get()) {
            case REGISTRATION_SUMMARY -> summaryReportService.getRegistrationSummaryCount(sessionID);
            default -> new SimpleHeadcountResultsTable();
        };
    }
}
