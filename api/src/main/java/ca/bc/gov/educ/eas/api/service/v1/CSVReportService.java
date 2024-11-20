package ca.bc.gov.educ.eas.api.service.v1;


import ca.bc.gov.educ.eas.api.constants.v1.reports.AllStudentRegistrationsHeader;
import ca.bc.gov.educ.eas.api.constants.v1.reports.EASReportTypeCode;
import ca.bc.gov.educ.eas.api.exception.EasAPIRuntimeException;
import ca.bc.gov.educ.eas.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.eas.api.rest.RestUtils;
import ca.bc.gov.educ.eas.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.eas.api.struct.v1.reports.DownloadableReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class CSVReportService {
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final RestUtils restUtils;
    private static final String SCHOOL_ID = "schoolID";

    public DownloadableReportResponse generateSessionRegistrationsReport(UUID sessionID) {
        List<AssessmentStudentEntity> results = assessmentStudentRepository.findByAssessmentEntity_SessionEntity_SessionID(sessionID);
        List<String> headers = Arrays.stream(AllStudentRegistrationsHeader.values()).map(AllStudentRegistrationsHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers.toArray(String[]::new))
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (AssessmentStudentEntity result : results) {
                var school = restUtils.getSchoolBySchoolID(result.getSchoolID().toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, SCHOOL_ID, result.getSchoolID().toString()));

                Optional<SchoolTombstone> assessmentCenter = Optional.empty();
                if(result.getAssessmentCenterID() != null) {
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

    private List<String> prepareEnrolmentFteDataForCsv(AssessmentStudentEntity student, SchoolTombstone school, Optional<SchoolTombstone> assessmentCenter) {
        return new ArrayList<>(Arrays.asList(
                school.getMincode(),
                student.getPen(),
                student.getGivenName(),
                student.getSurName(),
                assessmentCenter.isPresent() ? assessmentCenter.get().getMincode() : "",
                student.getAssessmentEntity().getAssessmentTypeCode()
        ));
    }

}
