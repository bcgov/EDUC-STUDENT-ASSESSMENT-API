package ca.bc.gov.educ.assessment.api.controller.v1;


import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentStudentReportTypeCode;
import ca.bc.gov.educ.assessment.api.endpoint.v1.ReportsEndpoint;
import ca.bc.gov.educ.assessment.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.assessment.api.exception.errors.ApiError;
import ca.bc.gov.educ.assessment.api.reports.ISRReportService;
import ca.bc.gov.educ.assessment.api.reports.DOARSummaryReportService;
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

    private final SchoolStudentsInSessionReportService schoolStudentsInSessionReportService;
    private final SchoolStudentsByAssessmentReportService schoolStudentsByAssessmentReportService;
    private final ISRReportService isrReportService;
    private final CSVReportService csvReportService;
    private final XAMFileService xamFileService;
    private final SummaryReportService summaryReportService;
    private final SessionService sessionService;
    private final DOARSummaryReportService doarSummaryReportService;

    @Override
    public DownloadableReportResponse getDownloadableReport(UUID sessionID, String type, String updateUser) {
        Optional<AssessmentReportTypeCode> code = AssessmentReportTypeCode.findByValue(type);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        switch (code.get()) {
            case ALL_SESSION_REGISTRATIONS:
                log.info("Generating All Session Registrations Report for sessionID :: {}", sessionID);
                var registrations = csvReportService.generateSessionRegistrationsReport(sessionID);
                log.info("Generated All Session Registrations Report for sessionID :: {}", sessionID);
                log.info("Recording transfer registrations user for sessionID :: {}", sessionID);
                sessionService.recordTransferRegistrationsUser(sessionID, updateUser, AssessmentReportTypeCode.ALL_SESSION_REGISTRATIONS);
                log.info("Recorded All Session Registrations Report for sessionID :: {}", sessionID);
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
            case NME_KEY_SUMMARY:
                return csvReportService.generateKeyReport(sessionID, "NME10");
            case NMF_KEY_SUMMARY:
                return csvReportService.generateKeyReport(sessionID, "NMF10");
            case LTE10_KEY_SUMMARY:
                return csvReportService.generateKeyReport(sessionID, "LTE10");
            case LTE12_KEY_SUMMARY:
                return csvReportService.generateKeyReport(sessionID, "LTE12");
            case LTP10_KEY_SUMMARY:
                return csvReportService.generateKeyReport(sessionID, "LTP10");
            case LTP12_KEY_SUMMARY:
                return csvReportService.generateKeyReport(sessionID, "LTP12");
            case LTF12_KEY_SUMMARY:
                return csvReportService.generateKeyReport(sessionID, "LTF12");
            case NME_ITEM_ANALYSIS:
                return csvReportService.generateDataForItemAnalysis(sessionID, "NME10");
            case NMF_ITEM_ANALYSIS:
                return csvReportService.generateDataForItemAnalysis(sessionID, "NMF10");
            case LTE10_ITEM_ANALYSIS:
                return csvReportService.generateDataForItemAnalysis(sessionID, "LTE10");
            case LTE12_ITEM_ANALYSIS:
                return csvReportService.generateDataForItemAnalysis(sessionID, "LTE12");
            case LTP10_ITEM_ANALYSIS:
                return csvReportService.generateDataForItemAnalysis(sessionID, "LTP10");
            case LTP12_ITEM_ANALYSIS:
                return csvReportService.generateDataForItemAnalysis(sessionID, "LTP12");
            case LTF12_ITEM_ANALYSIS:
                return csvReportService.generateDataForItemAnalysis(sessionID, "LTF12");
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
            case NME_DETAILED_DOAR:
                return csvReportService.generateDetailedDOARBySchool(sessionID, schoolID, "NME10");
            case NMF_DETAILED_DOAR:
                return csvReportService.generateDetailedDOARBySchool(sessionID, schoolID, "NMF10");
            case LTE10_DETAILED_DOAR:
                return csvReportService.generateDetailedDOARBySchool(sessionID, schoolID, "LTE10");
            case LTE12_DETAILED_DOAR:
                return csvReportService.generateDetailedDOARBySchool(sessionID, schoolID, "LTE12");
            case LTP10_DETAILED_DOAR:
                return csvReportService.generateDetailedDOARBySchool(sessionID, schoolID, "LTP10");
            case LTP12_DETAILED_DOAR:
                return csvReportService.generateDetailedDOARBySchool(sessionID, schoolID, "LTP12");
            case LTF12_DETAILED_DOAR:
                return csvReportService.generateDetailedDOARBySchool(sessionID, schoolID, "LTF12");
            case DOAR_SUMMARY:
                return doarSummaryReportService.generateDOARSummaryReport(sessionID, schoolID);
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

    @Override
    public DownloadableReportResponse getStudentReport(UUID studentID, String type) {
        Optional<AssessmentStudentReportTypeCode> code = AssessmentStudentReportTypeCode.findByValue(type);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }
        
       return isrReportService.generateIndividualStudentReport(studentID);
    }
}
