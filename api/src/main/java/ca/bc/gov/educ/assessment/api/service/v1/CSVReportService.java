package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.constants.v1.PenStatusCodeDesc;
import ca.bc.gov.educ.assessment.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.*;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMergeResult;
import ca.bc.gov.educ.assessment.api.struct.v1.SummaryByFormQueryResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.SummaryByGradeQueryResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.SimpleHeadcountResultsTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class CSVReportService {
    private static final String SCHOOL_ID = "schoolID";
    private static final String SESSION_ID = "sessionID";
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final AssessmentFormRepository assessmentFormRepository;
    private final StudentMergeService studentMergeService;
    private final RestUtils restUtils;
    private final AssessmentStudentLightRepository assessmentStudentLightRepository;
    private final StagedAssessmentStudentLightRepository stagedAssessmentStudentLightRepository;
    private final SummaryReportService summaryReportService;

    public DownloadableReportResponse generateSessionRegistrationsReport(UUID sessionID) {
        var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        List<AssessmentStudentEntity> results = assessmentStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionID(sessionID);
        List<String> headers = Arrays.stream(AllStudentRegistrationsHeader.values()).map(AllStudentRegistrationsHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(List.of("Registered Students                                            Date: " + LocalDate.now()));
            csvPrinter.printRecord(List.of("Assessment Centres for Session " + session.getCourseYear() + "/" + session.getCourseMonth()));
            csvPrinter.printRecord(List.of("-----------------------------------------------------------------------------------------------"));

            csvPrinter.printRecord(headers);

            for (AssessmentStudentEntity result : results) {
                if(StringUtils.isBlank(result.getProvincialSpecialCaseCode()) || result.getProvincialSpecialCaseCode().equals(ProvincialSpecialCaseCodes.EXEMPT.getCode())) {
                    var school = restUtils.getSchoolBySchoolID(result.getSchoolOfRecordSchoolID().toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, SCHOOL_ID, result.getSchoolOfRecordSchoolID().toString()));

                    Optional<SchoolTombstone> assessmentCenter = Optional.empty();
                    if (result.getAssessmentCenterSchoolID() != null) {
                        assessmentCenter = restUtils.getSchoolBySchoolID(result.getAssessmentCenterSchoolID().toString());
                    }
                    List<String> csvRowData = prepareRegistrationDataForCsv(result, school, assessmentCenter);
                    csvPrinter.printRecord(csvRowData);    
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(AssessmentReportTypeCode.ALL_SESSION_REGISTRATIONS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateNumberOfAttemptsReport(UUID sessionID) {
        assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        List<AssessmentStudentEntity> results = assessmentStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionID(sessionID);
        List<String> headers = Arrays.stream(NumberOfAttemptsHeader.values()).map(NumberOfAttemptsHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(List.of("Student Assessment Attempts                        Date: " + LocalDate.now()));
            csvPrinter.printRecord(List.of("--------------------------------------------------------------------------------"));

            csvPrinter.printRecord(headers);

            for (AssessmentStudentEntity result : results) {
                List<String> csvRowData = prepareNumberOfAttemptsDataForCsv(result);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(AssessmentReportTypeCode.ATTEMPTS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generatePenMergesReport() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime fromDate = LocalDate.now().minusMonths(13).atStartOfDay();
        LocalDateTime toDate = LocalDate.now().atStartOfDay();
        List<StudentMergeResult> results = studentMergeService.getMergedStudentsForDateRange(fromDate.format(formatter), toDate.format(formatter));
        List<String> headers = Arrays.stream(PenMergesHeader.values()).map(PenMergesHeader::getCode).toList();

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setQuoteMode(QuoteMode.NONE)
                .setEscape('\\')
                .build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);


            csvPrinter.printRecord(List.of("Student Assessment Merged PENS                                Date: " + LocalDate.now()));
            csvPrinter.printRecord(List.of("--------------------------------------------------------------------------------"));
            csvPrinter.printRecord(headers);

            for (StudentMergeResult result : results) {
                if (StringUtils.isNotEmpty(result.getCurrentPEN()) && StringUtils.isNotEmpty(result.getMergedPEN())) {
                    csvPrinter.printRecord(StringUtils.rightPad(result.getCurrentPEN(), PenMergesHeader.CURRENT_PEN.getCode().length(), StringUtils.SPACE), StringUtils.rightPad(result.getMergedPEN(), PenMergesHeader.MERGED_PEN.getCode().length(), StringUtils.SPACE));
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(AssessmentReportTypeCode.PEN_MERGES.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateSessionResultsBySchoolReport(UUID sessionID, UUID schoolID) {
        var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        var schoolTombstone = this.restUtils.getSchoolBySchoolID(schoolID.toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, SCHOOL_ID, schoolID.toString()));

        List<AssessmentStudentEntity> results = assessmentStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolID(sessionID, schoolID);
        List<String> headers = Arrays.stream(SessionResultsHeader.values()).map(SessionResultsHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(headers);

            for (AssessmentStudentEntity result : results) {
                Optional<SchoolTombstone> assessmentCenter = (result.getAssessmentCenterSchoolID() != null) ? restUtils.getSchoolBySchoolID(result.getAssessmentCenterSchoolID().toString()) : Optional.empty();
                List<String> csvRowData = prepareResultsDataForCsv(result, session, schoolTombstone, assessmentCenter);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType("%s-%s%s-Results.csv".formatted(schoolTombstone.getMincode(), session.getCourseYear(), session.getCourseMonth()));
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateRegistrationDetailReport(UUID sessionID) {
        assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));

        List<AssessmentStudentLightEntity> students;
        Optional<AssessmentStudentLightEntity> studentEntityOpt = assessmentStudentLightRepository.findBySessionIDAndDownloadDateIsNotNull(sessionID);
        if(studentEntityOpt.isPresent()) {
            students = assessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndDownloadDateIsNotNull(sessionID);
        } else {
            students = assessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionID(sessionID);
        }

        List<String> headers = Arrays.stream(RegistrationDetailsHeader.values()).map(RegistrationDetailsHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(headers);

            for (AssessmentStudentLightEntity result : students) {
                Optional<SchoolTombstone> school = (result.getSchoolAtWriteSchoolID() != null) ? restUtils.getSchoolBySchoolID(result.getSchoolAtWriteSchoolID().toString()) : Optional.empty();
                List<String> csvRowData = prepareRegistrationDetailsDataForCsv(result, school);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(REGISTRATION_DETAIL_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }
    
    private boolean sessionIsApproved(AssessmentSessionEntity assessmentSessionEntity) {
        return assessmentSessionEntity.getApprovalStudentCertSignDate() != null && assessmentSessionEntity.getApprovalAssessmentAnalysisSignDate() != null && assessmentSessionEntity.getApprovalAssessmentDesignSignDate() != null;
    }
    
    public DownloadableReportResponse generateSummaryResultsByGradeInSession(UUID sessionID) {
        var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));

        List<String> headers = Arrays.stream(SummaryResultsByGradeHeader.values()).map(SummaryResultsByGradeHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(headers);

            List<SummaryByGradeQueryResponse> gradeSummaries;
            if(sessionIsApproved(session)) {
                gradeSummaries = assessmentStudentLightRepository.getSummaryByGradeForSession(sessionID);
            }else{
                gradeSummaries = stagedAssessmentStudentLightRepository.getSummaryByGradeForSession(sessionID);
            }

            populateCSVPrinterForGradeSummary(gradeSummaries, csvPrinter, session);

            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SUMMARY_BY_GRADE_FOR_SESSION.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateSummaryByFormInSession(UUID sessionID) {
        var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        var forms = assessmentFormRepository.findAllByAssessmentEntity_AssessmentSessionEntity_SessionID(sessionID);

        List<String> headers = Arrays.stream(SummaryByFormHeader.values()).map(SummaryByFormHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(headers);

            List<SummaryByFormQueryResponse> formSummaries;
            if(sessionIsApproved(session)) {
                formSummaries = assessmentStudentLightRepository.getSummaryByFormForSession(sessionID);
            }else{
                formSummaries = stagedAssessmentStudentLightRepository.getSummaryByFormForSession(sessionID);
            }
            
            populateCSVPrinterForFormSummary(formSummaries, csvPrinter, forms);

            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SUMMARY_BY_FORM_FOR_SESSION.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }
    
    public DownloadableReportResponse generateAllDetailedStudentsInSession(UUID sessionID) {
        var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        var forms = assessmentFormRepository.findAllByAssessmentEntity_AssessmentSessionEntity_SessionID(sessionID);

        List<String> headers = Arrays.stream(AllStudentDetailedRegistrationDetailsHeader.values()).map(AllStudentDetailedRegistrationDetailsHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(headers);

            if(sessionIsApproved(session)) {
                List<AssessmentStudentLightEntity>  students = assessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusIn(sessionID, List.of("ACTIVE"));
                populateCSVPrinterForApproval(students, forms, csvPrinter);
            }else{
                List<StagedAssessmentStudentLightEntity> students = stagedAssessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStagedAssessmentStudentStatusIn(sessionID, List.of("ACTIVE"));
                populateCSVPrinterForStaged(students, forms, csvPrinter);
            }
            
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(ALL_DETAILED_STUDENTS_IN_SESSION_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    private void populateCSVPrinterForFormSummary(List<SummaryByFormQueryResponse> results, CSVPrinter csvPrinter, List<AssessmentFormEntity> forms) throws IOException {
        for (SummaryByFormQueryResponse result : results) {
            var form = result.getFormID() != null ? forms.stream()
                    .filter(assessmentFormEntity ->  assessmentFormEntity.getAssessmentFormID().equals(result.getFormID())).findFirst()
                    .orElseThrow(() -> new EntityNotFoundException(AssessmentFormEntity.class, "AssessmentForm", result.getFormID().toString())).getFormCode() : "";
            List<String> csvRowData = prepareFormSummaryDetailsDataForCsv(result, form);
            csvPrinter.printRecord(csvRowData);
        }
    }

    private void populateCSVPrinterForGradeSummary(List<SummaryByGradeQueryResponse> students, CSVPrinter csvPrinter, AssessmentSessionEntity session) throws IOException {
        for (SummaryByGradeQueryResponse result : students) {
            List<String> csvRowData = prepareGradeSummaryDetailsDataForCsv(result, session);
            csvPrinter.printRecord(csvRowData);
        }
    }
    
    private void populateCSVPrinterForApproval(List<AssessmentStudentLightEntity> students, List<AssessmentFormEntity> forms, CSVPrinter csvPrinter) throws IOException {
        for (AssessmentStudentLightEntity result : students) {
            Optional<SchoolTombstone> school = (result.getSchoolAtWriteSchoolID() != null) ? restUtils.getSchoolBySchoolID(result.getSchoolAtWriteSchoolID().toString()) : Optional.empty();
            Optional<SchoolTombstone> assessmentCenter = (result.getAssessmentCenterSchoolID() != null) ? restUtils.getSchoolBySchoolID(result.getAssessmentCenterSchoolID().toString()) : Optional.empty();
            var form = result.getAssessmentFormID() != null ? forms.stream()
                    .filter(assessmentFormEntity ->  assessmentFormEntity.getAssessmentFormID().equals(result.getAssessmentFormID())).findFirst()
                    .orElseThrow(() -> new EntityNotFoundException(AssessmentFormEntity.class, "AssessmentForm", result.getAssessmentFormID().toString())).getFormCode() : "";
            List<String> csvRowData = prepareAllStudentDetailsRegistrationDetailsDataForCsv(result, school, form, assessmentCenter);
            csvPrinter.printRecord(csvRowData);
        }
    }

    private void populateCSVPrinterForStaged(List<StagedAssessmentStudentLightEntity> students, List<AssessmentFormEntity> forms, CSVPrinter csvPrinter) throws IOException {
        for (StagedAssessmentStudentLightEntity result : students) {
            Optional<SchoolTombstone> school = (result.getSchoolAtWriteSchoolID() != null) ? restUtils.getSchoolBySchoolID(result.getSchoolAtWriteSchoolID().toString()) : Optional.empty();
            Optional<SchoolTombstone> assessmentCenter = (result.getAssessmentCenterSchoolID() != null) ? restUtils.getSchoolBySchoolID(result.getAssessmentCenterSchoolID().toString()) : Optional.empty();
            var form = result.getAssessmentFormID() != null ? forms.stream()
                    .filter(assessmentFormEntity ->  assessmentFormEntity.getAssessmentFormID().equals(result.getAssessmentFormID())).findFirst()
                    .orElseThrow(() -> new EntityNotFoundException(AssessmentFormEntity.class, "AssessmentForm", result.getAssessmentFormID().toString())).getFormCode() : "";
            List<String> csvRowData = prepareAllStudentDetailsRegistrationDetailsDataForCsv(result, school, form, assessmentCenter);
            csvPrinter.printRecord(csvRowData);
        }
    }

    public DownloadableReportResponse generatePenIssuesReport(UUID sessionID) {
        assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        List<StagedAssessmentStudentLightEntity> results = stagedAssessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStagedAssessmentStudentStatusIn(sessionID, List.of("MERGED", "NOPENFOUND"));

        List<String> headers = Arrays.stream(PenIssuesHeader.values()).map(PenIssuesHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(headers);

            for (StagedAssessmentStudentLightEntity result : results) {
                List<String> csvRowData = preparePenIssuesForCsv(result);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(PEN_ISSUES_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateAssessmentRegistrationTotalsBySchoolReport(UUID sessionID) {
        SimpleHeadcountResultsTable assessmentRegistrationTotalsBySchoolReportTable = summaryReportService.getAssessmentRegistrationTotalsBySchool(sessionID);
        List<String> headers =  assessmentRegistrationTotalsBySchoolReportTable.getHeaders();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(headers);

            for (Map<String, String> result : assessmentRegistrationTotalsBySchoolReportTable.getRows()) {
                List<String> csvRowData = prepareAssessmentRegistrationTotalsBySchoolForCsv(result, headers);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            DownloadableReportResponse downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(REGISTRATION_SUMMARY_BY_SCHOOL.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    private List<String> prepareNumberOfAttemptsDataForCsv(AssessmentStudentEntity student) {
        return new ArrayList<>(Arrays.asList(
                student.getAssessmentEntity().getAssessmentTypeCode(),
                student.getPen(),
                student.getNumberOfAttempts() != null ? Integer.toString(student.getNumberOfAttempts()) : null
        ));
    }

    private List<String> prepareRegistrationDataForCsv(AssessmentStudentEntity student, SchoolTombstone school, Optional<SchoolTombstone> assessmentCenter) {
        return new ArrayList<>(Arrays.asList(
                school.getMincode(),
                student.getPen(),
                student.getGivenName(),
                student.getSurname(),
                assessmentCenter.isPresent() ? assessmentCenter.get().getMincode() : "",
                student.getAssessmentEntity().getAssessmentTypeCode()
        ));
    }

    private List<String> prepareResultsDataForCsv(AssessmentStudentEntity student, AssessmentSessionEntity assessmentSession, SchoolTombstone school, Optional<SchoolTombstone> assessmentCenter) {
        return new ArrayList<>(Arrays.asList(
                "%s%s".formatted(assessmentSession.getCourseYear(), assessmentSession.getCourseMonth()),
                school.getMincode(),
                student.getAssessmentEntity().getAssessmentTypeCode(),
                student.getPen(),
                student.getLocalID(),
                student.getSurname(),
                student.getGivenName(),
                student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : "",
                student.getProvincialSpecialCaseCode(),
                assessmentCenter.isPresent() ? assessmentCenter.get().getMincode() : ""
        ));
    }

    private List<String> prepareFormSummaryDetailsDataForCsv(SummaryByFormQueryResponse formSummary, String form) {
        return new ArrayList<>(Arrays.asList(
                formSummary.getAssessmentTypeCode(),
                form,
                Long.toString(formSummary.getProfScore1()),
                Long.toString(formSummary.getProfScore2()),
                Long.toString(formSummary.getProfScore3()),
                Long.toString(formSummary.getProfScore4()),
                Long.toString(formSummary.getAegCount()),
                Long.toString(formSummary.getNcCount()),
                Long.toString(formSummary.getDsqCount()),
                Long.toString(formSummary.getXmtCount()),
                Long.toString(formSummary.getTotal())
        ));
    }


    private List<String> prepareGradeSummaryDetailsDataForCsv(SummaryByGradeQueryResponse gradeSummary, AssessmentSessionEntity session) {
        return new ArrayList<>(Arrays.asList(
                gradeSummary.getAssessmentTypeCode(),
                session.getCourseYear() + session.getCourseMonth(),
                gradeSummary.getGrade(),
                Long.toString(gradeSummary.getProfScore1()),
                Long.toString(gradeSummary.getProfScore2()),
                Long.toString(gradeSummary.getProfScore3()),
                Long.toString(gradeSummary.getProfScore4()),
                Long.toString(gradeSummary.getAegCount()),
                Long.toString(gradeSummary.getNcCount()),
                Long.toString(gradeSummary.getDsqCount()),
                Long.toString(gradeSummary.getXmtCount()),
                Long.toString(gradeSummary.getTotal())
        ));
    }

    private List<String> prepareAllStudentDetailsRegistrationDetailsDataForCsv(AssessmentStudentLightEntity student, Optional<SchoolTombstone> school, String form, Optional<SchoolTombstone> assessmentCenter) {
        return new ArrayList<>(Arrays.asList(
                student.getPen(),
                student.getAssessmentEntity().getAssessmentTypeCode(),
                student.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear() + student.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth(),
                form,
                school.isPresent() ? school.get().getMincode(): "",
                school.isPresent() ? school.get().getSchoolCategoryCode(): "",
                student.getGradeAtRegistration(),
                student.getMcTotal() != null ? student.getMcTotal().toString() : "",
                student.getOeTotal() != null ? student.getOeTotal().toString() : "",
                student.getRawScore() != null ? student.getRawScore().toString() : "",
                student.getIrtScore(),
                student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : "",
                student.getProvincialSpecialCaseCode(),
                assessmentCenter.isPresent() ? assessmentCenter.get().getMincode(): ""
        ));
    }

    private List<String> prepareAllStudentDetailsRegistrationDetailsDataForCsv(StagedAssessmentStudentLightEntity student, Optional<SchoolTombstone> school, String form, Optional<SchoolTombstone> assessmentCenter) {
        return new ArrayList<>(Arrays.asList(
                student.getPen(),
                student.getAssessmentEntity().getAssessmentTypeCode(),
                student.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear() + student.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth(),
                form,
                school.isPresent() ? school.get().getMincode(): "",
                school.isPresent() ? school.get().getSchoolCategoryCode(): "",
                student.getGradeAtRegistration(),
                student.getMcTotal() != null ? student.getMcTotal().toString() : "",
                student.getOeTotal() != null ? student.getOeTotal().toString() : "",
                student.getRawScore() != null ? student.getRawScore().toString() : "",
                student.getIrtScore(),
                student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : "",
                student.getProvincialSpecialCaseCode(),
                assessmentCenter.isPresent() ? assessmentCenter.get().getMincode(): ""
        ));
    }

    private List<String> prepareRegistrationDetailsDataForCsv(AssessmentStudentLightEntity student, Optional<SchoolTombstone> school) {
        return new ArrayList<>(Arrays.asList(
                student.getPen(),
                student.getGradeAtRegistration(),
                student.getSurname(),
                student.getAssessmentEntity().getAssessmentTypeCode(),
                student.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear() + student.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth(),
                school.isPresent() ? school.get().getMincode(): "",
                student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : "",
                student.getProvincialSpecialCaseCode()
        ));
    }

    private List<String> preparePenIssuesForCsv(StagedAssessmentStudentLightEntity student) {
        return new ArrayList<>(Arrays.asList(
                student.getPen(),
                student.getMergedPen(),
                PenStatusCodeDesc.valueOf(student.getStagedAssessmentStudentStatus()).getCode()
        ));
    }

    private List<String> prepareAssessmentRegistrationTotalsBySchoolForCsv(Map<String, String> result, List<String> headers) {
        List<String> toReturn = new ArrayList<>();
        for (String header : headers) {
            if (!result.containsKey(header)) {
                toReturn.add("");
                continue;
            }
            toReturn.add(result.get(header));
        }
        return toReturn;
    }
}
