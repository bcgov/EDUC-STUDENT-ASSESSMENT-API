package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.*;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMergeResult;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
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


@Service
@Slf4j
@RequiredArgsConstructor
public class CSVReportService {
    private static final String SCHOOL_ID = "schoolID";
    private static final String SESSION_ID = "sessionID";
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final StudentMergeService studentMergeService;
    private final RestUtils restUtils;

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

}
