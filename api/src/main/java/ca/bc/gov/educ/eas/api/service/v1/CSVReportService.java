package ca.bc.gov.educ.eas.api.service.v1;


import ca.bc.gov.educ.eas.api.constants.v1.reports.AllStudentRegistrationsHeader;
import ca.bc.gov.educ.eas.api.constants.v1.reports.EASReportTypeCode;
import ca.bc.gov.educ.eas.api.constants.v1.reports.PenMergesHeader;
import ca.bc.gov.educ.eas.api.exception.EasAPIRuntimeException;
import ca.bc.gov.educ.eas.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.SessionRepository;
import ca.bc.gov.educ.eas.api.rest.RestUtils;
import ca.bc.gov.educ.eas.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMergeResult;
import ca.bc.gov.educ.eas.api.struct.v1.reports.DownloadableReportResponse;
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
    private final SessionRepository sessionRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final StudentMergeService studentMergeService;
    private final RestUtils restUtils;

    public DownloadableReportResponse generateSessionRegistrationsReport(UUID sessionID) {
        var session = sessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(SessionEntity.class, "sessionID", sessionID.toString()));
        List<AssessmentStudentEntity> results = assessmentStudentRepository.findByAssessmentEntity_SessionEntity_SessionID(sessionID);
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
                var school = restUtils.getSchoolBySchoolID(result.getSchoolID().toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, SCHOOL_ID, result.getSchoolID().toString()));

                Optional<SchoolTombstone> assessmentCenter = Optional.empty();
                if (result.getAssessmentCenterID() != null) {
                    assessmentCenter = restUtils.getSchoolBySchoolID(result.getAssessmentCenterID().toString());
                }
                List<String> csvRowData = prepareEnrolmentFteDataForCsv(result, school, assessmentCenter);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(EASReportTypeCode.ALL_SESSION_REGISTRATIONS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new EasAPIRuntimeException(e);
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
            downloadableReport.setReportType(EASReportTypeCode.PEN_MERGES.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new EasAPIRuntimeException(e);
        }
    }

    private List<String> prepareEnrolmentFteDataForCsv(AssessmentStudentEntity student, SchoolTombstone school, Optional<SchoolTombstone> assessmentCenter) {
        return new ArrayList<>(Arrays.asList(
                school.getMincode(),
                student.getPen(),
                student.getGivenName(),
                student.getSurname(),
                assessmentCenter.isPresent() ? assessmentCenter.get().getMincode() : "",
                student.getAssessmentEntity().getAssessmentTypeCode()
        ));
    }

}
