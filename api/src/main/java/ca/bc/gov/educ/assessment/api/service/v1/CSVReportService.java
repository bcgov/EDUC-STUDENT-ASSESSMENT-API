package ca.bc.gov.educ.assessment.api.service.v1;


import ca.bc.gov.educ.assessment.api.constants.v1.*;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.*;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.PaginatedResponse;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.external.sdc.v1.Collection;
import ca.bc.gov.educ.assessment.api.struct.external.sdc.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.KeySummaryReportResult;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMergeResult;
import ca.bc.gov.educ.assessment.api.struct.v1.SummaryByFormQueryResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.SummaryByGradeQueryResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.NumberOfAttemptsStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.SimpleHeadcountResultsTable;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentReportTypeCode.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class CSVReportService {
    private static final String SCHOOL_ID = "schoolID";
    private static final String SESSION_ID = "sessionID";
    private static final String STUDENTS_KEY = "students";
    private static final String STUDENT_TO_COLLECTION_SNAPSHOT_DATE_MAP_KEY = "studentToCollectionSnapshotDateMap";
    private static final String COLLECTION_TYPE_KEY = "collectionType";
    private static final String SEPTEMBER = "SEPTEMBER";
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final AssessmentFormRepository assessmentFormRepository;
    private final StudentMergeService studentMergeService;
    private final List<String> activeStatus = List.of("ACTIVE");
    private final RestUtils restUtils;
    private final AssessmentStudentLightRepository assessmentStudentLightRepository;
    private final StagedAssessmentStudentLightRepository stagedAssessmentStudentLightRepository;
    private final SummaryReportService summaryReportService;
    private final DOARReportService doarReportService;
    private final AssessmentStudentSearchService assessmentStudentSearchService;

    public DownloadableReportResponse generateSessionRegistrationsReport(UUID sessionID) {
        var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        List<AssessmentStudentEntity> results = assessmentStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCodeIn(sessionID, activeStatus);
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
                if(StringUtils.isBlank(result.getProvincialSpecialCaseCode()) || !result.getProvincialSpecialCaseCode().equals(ProvincialSpecialCaseCodes.EXEMPT.getCode())) {
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
        assessmentSessionRepository.findById(sessionID)
                .orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));

        List<String> headers = Arrays.stream(NumberOfAttemptsHeader.values())
                .map(NumberOfAttemptsHeader::getCode)
                .toList();

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(List.of("Student Assessment Attempts                        Date: " + LocalDate.now()));
            csvPrinter.printRecord(List.of("--------------------------------------------------------------------------------"));
            csvPrinter.printRecord(headers);

            // Process both streams sequentially, writing directly to CSV without holding in memory
            try (Stream<NumberOfAttemptsStudent> streamNotNM = assessmentStudentRepository.streamNumberOfAttemptsCountsNotNM()) {
                streamNotNM.forEach(result -> {
                    try {
                        List<String> csvRowData = prepareNumberOfAttemptsDataForCsv(result);
                        csvPrinter.printRecord(csvRowData);
                    } catch (IOException e) {
                        throw new StudentAssessmentAPIRuntimeException(e);
                    }
                });
            }

            try (Stream<NumberOfAttemptsStudent> streamNM = assessmentStudentRepository.streamNumberOfAttemptsCountsNM()) {
                streamNM.forEach(result -> {
                    try {
                        List<String> csvRowData = prepareNumberOfAttemptsDataForCsv(result);
                        csvPrinter.printRecord(csvRowData);
                    } catch (IOException e) {
                        throw new StudentAssessmentAPIRuntimeException(e);
                    }
                });
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

        List<AssessmentStudentEntity> results = assessmentStudentRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndSchoolAtWriteSchoolIDAndStudentStatusCodeIn(sessionID, schoolID, activeStatus);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);
            
            csvPrinter.printRecord(Arrays.stream(SessionResultsHeader.values()).map(SessionResultsHeader::getCode).toList());

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
        Optional<AssessmentStudentLightEntity> studentEntityOpt = assessmentStudentLightRepository.findBySessionIDAndDownloadDateIsNotNullAndProvincialSpecialCaseCodeNotAndStudentStatusCodeActive(sessionID, ProvincialSpecialCaseCodes.EXEMPT.getCode());
        if(studentEntityOpt.isPresent()) {
            students = assessmentStudentLightRepository.findByDownloadDateIsNotNullAndProvincialSpecialCaseCodeNotAndStudentStatusCode(sessionID, ProvincialSpecialCaseCodes.EXEMPT.getCode(), StudentStatusCodes.ACTIVE.getCode());
        } else {
            students = assessmentStudentLightRepository.findBProvincialSpecialCaseCodeNotAndStudentStatusCode(sessionID, ProvincialSpecialCaseCodes.EXEMPT.getCode(), StudentStatusCodes.ACTIVE.getCode());
        }

        List<String> headers = Arrays.stream(RegistrationDetailsHeader.values()).map(RegistrationDetailsHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(headers);

            for (AssessmentStudentLightEntity result : students) {
                Optional<SchoolTombstone> school = (result.getSchoolOfRecordSchoolID() != null) ? restUtils.getSchoolBySchoolID(result.getSchoolOfRecordSchoolID().toString()) : Optional.empty();
                Optional<District> district = (school.isPresent() && school.get().getDistrictId() != null) ? restUtils.getDistrictByDistrictID(school.get().getDistrictId()) : Optional.empty();
                List<String> csvRowData = prepareRegistrationDetailsDataForCsv(result, school, district);
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
        return assessmentSessionEntity.getCompletionDate() != null;
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
            
            populateCSVPrinterForFormSummary(formSummaries, csvPrinter, forms, session);

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
                List<AssessmentStudentLightEntity>  students = assessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStudentStatusCodeIn(sessionID, List.of("ACTIVE"));
                populateCSVPrinterForApproval(students, forms, csvPrinter);
            } else {
                List<StagedAssessmentStudentLightEntity> students = stagedAssessmentStudentLightRepository.findByAssessmentEntity_AssessmentSessionEntity_SessionIDAndStagedAssessmentStudentStatusIn(sessionID, List.of("ACTIVE", "MERGED"));
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

    private void populateCSVPrinterForFormSummary(List<SummaryByFormQueryResponse> results, CSVPrinter csvPrinter, List<AssessmentFormEntity> forms, AssessmentSessionEntity session) throws IOException {
        for (SummaryByFormQueryResponse result : results) {
            var form = result.getFormID() != null ? forms.stream()
                    .filter(assessmentFormEntity ->  assessmentFormEntity.getAssessmentFormID().equals(result.getFormID())).findFirst()
                    .orElseThrow(() -> new EntityNotFoundException(AssessmentFormEntity.class, "AssessmentForm", result.getFormID().toString())).getFormCode() : "";
            List<String> csvRowData = prepareFormSummaryDetailsDataForCsv(result, form, session);
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

    public DownloadableReportResponse generateDetailedDOARBySchool(UUID sessionID, UUID schoolID, String assessmentTypeCode) {
        var schoolTombstone = this.restUtils.getSchoolBySchoolID(schoolID.toString()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, SCHOOL_ID, schoolID.toString()));
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);
            csvPrinter.printRecord(getDOARHeaders(assessmentTypeCode));

            for (List<String> row : doarReportService.generateDetailedDOARBySchoolAndAssessmentType(sessionID, schoolTombstone, assessmentTypeCode)) {
                csvPrinter.printRecord(row);
            }
            csvPrinter.flush();

            DownloadableReportResponse downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(assessmentTypeCode);
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateKeyReport(UUID sessionID, String assessmentTypeCode) {
        var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        AssessmentEntity assessmentEntity = session.getAssessments().stream().filter(entity -> entity.getAssessmentTypeCode().equalsIgnoreCase(assessmentTypeCode)).findFirst().orElseThrow(() -> new EntityNotFoundException(AssessmentEntity.class, "assessmentTypeCode", assessmentTypeCode));

        List<AssessmentChoiceEntity> choices = assessmentEntity.getAssessmentForms().stream()
                .flatMap(assessmentFormEntity -> assessmentFormEntity.getAssessmentComponentEntities().stream())
                .flatMap(assessmentComponentEntity -> assessmentComponentEntity.getAssessmentChoiceEntities().stream()).toList();
        List<AssessmentQuestionEntity> questions = assessmentEntity.getAssessmentForms().stream()
                .flatMap(assessmentFormEntity -> assessmentFormEntity.getAssessmentComponentEntities().stream())
                .flatMap(assessmentComponentEntity -> assessmentComponentEntity.getAssessmentQuestionEntities().stream()).toList();


        List<String> headers = Arrays.stream(KeySummaryHeader.values()).map(KeySummaryHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(headers);
            
            var keySummaryRowsList = new ArrayList<KeySummaryReportResult>();
            questions.forEach(question -> {
                keySummaryRowsList.add(getKeySummaryReportResult(question));
            });

            choices.forEach(choice -> {
                keySummaryRowsList.add(getKeySummaryReportResult(choice, questions));
            });

            var sortedList = keySummaryRowsList.stream().sorted(Comparator.comparing(KeySummaryReportResult::getFormCode)
                    .thenComparing(KeySummaryReportResult::getComponentType)
                    .thenComparing(KeySummaryReportResult::getItemNumber)
                    .thenComparing(KeySummaryReportResult::getQuestionNumber)).toList();
            
            var sessionString = session.getCourseYear() +"/"+ session.getCourseMonth();
            for (KeySummaryReportResult result : sortedList) {
                List<String> csvRowData = prepareKeySummaryForCsv(result, assessmentTypeCode, sessionString);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(assessmentTypeCode + "-key-summary");
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateDataForItemAnalysis(UUID sessionID, String assessmentTypeCode) {
        AssessmentSessionEntity assessmentSession = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));

        List<AssessmentStudentLightEntity> students = assessmentStudentLightRepository.findByAssessmentTypeCodeAndSessionIDAndProficiencyScoreNotNullOrProvincialSpecialCaseNotNull(assessmentTypeCode, sessionID);
        List<String> assignedStudentIds = students.stream().map(AssessmentStudentLightEntity::getPen).toList();

        List<String> headers = Arrays.stream(DataItemAnalysisHeader.values()).map(DataItemAnalysisHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            csvPrinter.printRecord(headers);

            // Get the last four collections before assessmentSession
            // and find appropriate SDC data based on session month
            var lastFourCollections = restUtils.getLastFourCollections(assessmentSession);
            var sdcData = findAppropriateSDCData(lastFourCollections, assignedStudentIds, assessmentSession.getCourseMonth());

            // Extract data from the result map
            Object studentsObj = sdcData.get(STUDENTS_KEY);
            Object snapshotDateMapObj = sdcData.get(STUDENT_TO_COLLECTION_SNAPSHOT_DATE_MAP_KEY);
            var usedCollectionType = (String) sdcData.get(COLLECTION_TYPE_KEY);

            if (!(studentsObj instanceof List<?> studentsList) ||
                !(snapshotDateMapObj instanceof Map<?, ?> snapshotDateMap)) {
                throw new StudentAssessmentAPIRuntimeException("Invalid SDC data structure received");
            }

            List<SdcSchoolCollectionStudent> sdcStudents = studentsList.stream()
                    .filter(SdcSchoolCollectionStudent.class::isInstance)
                    .map(SdcSchoolCollectionStudent.class::cast)
                    .toList();

            Map<String, String> studentToCollectionSnapshotDateMap = snapshotDateMap.entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof String)
                    .collect(Collectors.toMap(
                            entry -> (String) entry.getKey(),
                            entry -> (String) entry.getValue()
                    ));

            Map<String, SdcSchoolCollectionStudent> studentMap = sdcStudents.stream().collect(Collectors.toMap(SdcSchoolCollectionStudent::getAssignedPen, Function.identity()));

            log.info("Generating item analysis report for session {}/{} using {} collection with {} students", assessmentSession.getCourseYear(), assessmentSession.getCourseMonth(), usedCollectionType, sdcStudents.size());

            var sessionString = assessmentSession.getCourseYear() +"/"+ assessmentSession.getCourseMonth();
            for (AssessmentStudentLightEntity student : students) {
                SdcSchoolCollectionStudent sdcStudent = studentMap.get(student.getPen());
                String collectionSnapshotDate = studentToCollectionSnapshotDateMap.get(student.getPen());

                List<String> csvRowData = prepareStudentForItemAnalysis(student, sdcStudent, sessionString, collectionSnapshotDate);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(assessmentTypeCode + "-data-item-analysis");
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentAssessmentAPIRuntimeException(e);
        }
    }

    private List<String> getDOARHeaders(String assessmentTypeCode) {
        return switch (assessmentTypeCode) {
            case "NME10" -> Arrays.stream(NMEDoarHeader.values()).map(NMEDoarHeader::getCode).toList();
            case "NMF10" -> Arrays.stream(NMFDoarHeader.values()).map(NMFDoarHeader::getCode).toList();
            case "LTE10", "LTE12" -> Arrays.stream(LTEDoarHeader.values()).map(LTEDoarHeader::getCode).toList();
            case "LTP12" -> Arrays.stream(LTP12DoarHeader.values()).map(LTP12DoarHeader::getCode).toList();
            case "LTP10" -> Arrays.stream(LTP10DoarHeader.values()).map(LTP10DoarHeader::getCode).toList();
            case "LTF12" -> Arrays.stream(LTF12DoarHeader.values()).map(LTF12DoarHeader::getCode).toList();
            default -> Collections.emptyList();
        };
    }

    private List<String> prepareNumberOfAttemptsDataForCsv(NumberOfAttemptsStudent student) {
        return new ArrayList<>(Arrays.asList(
                student.getAssessmentTypeCode(),
                student.getPen(),
                student.getNumberOfAttempts()
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
                Optional.ofNullable(school).map(s -> "=\"%s\"".formatted(s.getMincode())).orElse(""),
                student.getAssessmentEntity().getAssessmentTypeCode(),
                student.getPen(),
                student.getLocalID(),
                student.getSurname(),
                student.getGivenName(),
                student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : "",
                student.getProvincialSpecialCaseCode(),
                assessmentCenter.map(s -> "=\"%s\"".formatted(s.getMincode())).orElse("")
        ));
    }

    private List<String> prepareFormSummaryDetailsDataForCsv(SummaryByFormQueryResponse formSummary, String form, AssessmentSessionEntity assessmentSession) {
        return new ArrayList<>(Arrays.asList(
                formSummary.getAssessmentTypeCode(),
                assessmentSession.getCourseYear() + assessmentSession.getCourseMonth(),
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

    private List<String> prepareRegistrationDetailsDataForCsv(AssessmentStudentLightEntity student, Optional<SchoolTombstone> school, Optional<District> district) {
        return new ArrayList<>(Arrays.asList(
                student.getPen(),
                student.getGradeAtRegistration(),
                student.getSurname(),
                student.getAssessmentEntity().getAssessmentTypeCode(),
                student.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear() + student.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth(),
                school.isPresent() ? school.get().getMincode(): "",
                district.isPresent() ? district.get().getDistrictNumber(): "",
                school.isPresent() ? school.get().getSchoolCategoryCode(): ""
        ));
    }

    private List<String> preparePenIssuesForCsv(StagedAssessmentStudentLightEntity student) {
        return new ArrayList<>(Arrays.asList(
                student.getPen(),
                student.getMergedPen(),
                PenStatusCodeDesc.valueOf(student.getStagedAssessmentStudentStatus()).getCode()
        ));
    }

    private List<String> prepareKeySummaryForCsv(KeySummaryReportResult result, String assessmentTypeCode, String session) {
        return new ArrayList<>(Arrays.asList(
                assessmentTypeCode,
                session,
                result.getFormCode(),
                result.getComponentType(),
                result.getItemNumber().toString(),
                StringUtils.isNotBlank(result.getAssessmentQuestionID()) ? "Mark" : "Choice",
                StringUtils.isNotBlank(result.getQuestionNumber()) ? result.getQuestionNumber() : null,
                result.getQuestionValue() != null ? result.getQuestionValue().toString() : null,
                result.getScaleFactor() != null ? String.valueOf(new BigDecimal(result.getScaleFactor()).divide(new BigDecimal(100))) : null,
                calculateScaledValue(result),
                result.getMaxQuestionValue() != null ? result.getMaxQuestionValue().toString() : null,
                result.getCognitiveLevelCode(),
                result.getTaskCode(),
                result.getClaimCode(),
                result.getContextCode(),
                result.getConceptCode(),
                result.getAssessmentSection()
        ));
    }
    
    private KeySummaryReportResult getKeySummaryReportResult(AssessmentQuestionEntity ques) {
        KeySummaryReportResult result = new KeySummaryReportResult();
        result.setAssessmentQuestionID(ques.getAssessmentQuestionID().toString());
        result.setFormCode(ques.getAssessmentComponentEntity().getAssessmentFormEntity().getFormCode());
        result.setComponentType(getComponentType(ques.getAssessmentComponentEntity()));
        result.setQuestionNumber(ques.getQuestionNumber() != null ? ques.getQuestionNumber().toString() : null);
        result.setCognitiveLevelCode(ques.getCognitiveLevelCode());
        result.setTaskCode(ques.getTaskCode());
        result.setClaimCode(ques.getClaimCode());
        result.setContextCode(ques.getContextCode());
        result.setConceptCode(ques.getConceptCode());
        result.setAssessmentSection(ques.getAssessmentSection());
        result.setItemNumber(ques.getItemNumber());
        result.setQuestionValue(ques.getQuestionValue());
        result.setMaxQuestionValue(ques.getMaxQuestionValue());
        result.setMasterQuestionNumber(ques.getMasterQuestionNumber());
        result.setIrtIncrement(ques.getIrtIncrement());
        result.setPreloadAnswer(ques.getPreloadAnswer());
        result.setIrt(ques.getIrt());
        result.setScaleFactor(ques.getScaleFactor());
        return result;
    }

    private KeySummaryReportResult getKeySummaryReportResult(AssessmentChoiceEntity choiceEntity, List<AssessmentQuestionEntity> questions) {
        KeySummaryReportResult result = new KeySummaryReportResult();
        result.setAssessmentChoiceID(choiceEntity.getAssessmentChoiceID().toString());
        result.setFormCode(choiceEntity.getAssessmentComponentEntity().getAssessmentFormEntity().getFormCode());
        result.setComponentType(getComponentType(choiceEntity.getAssessmentComponentEntity()));
        result.setItemNumber(choiceEntity.getItemNumber());
        result.setMasterQuestionNumber(choiceEntity.getMasterQuestionNumber());
        result.setQuestionNumber(getQuestionNumbers(choiceEntity, questions));
        return result;
    }
    
    private List<String> prepareStudentForItemAnalysis(AssessmentStudentLightEntity student, SdcSchoolCollectionStudent sdcStudent, String session, String collectionSnapshotDate) {
        List<String> enrolledPrograms = new ArrayList<>();
        String sdcStudentNativeAcnestry = "0";
        String sdcStudentGender = "NA";
        String mincode = "";

        if (student.getSchoolAtWriteSchoolID() != null) {
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(student.getSchoolAtWriteSchoolID().toString());
            if (school.isPresent()) {
                mincode = school.get().getMincode();
            } else {
                mincode = student.getSchoolAtWriteSchoolID().toString();
            }
        }

        if (sdcStudent != null) {
            if (sdcStudent.getEnrolledProgramCodes() != null && !sdcStudent.getEnrolledProgramCodes().isEmpty()) {
                Pattern pattern = Pattern.compile(".{2}");
                Matcher matcher = pattern.matcher(sdcStudent.getEnrolledProgramCodes());

                while (matcher.find()) {
                    enrolledPrograms.add(matcher.group());
                }
            }

            sdcStudentNativeAcnestry = sdcStudent.getNativeAncestryInd() != null && sdcStudent.getNativeAncestryInd().equalsIgnoreCase("Y") ? "1" : "0";
            sdcStudentGender = sdcStudent.getGender() != null ? sdcStudent.getGender() : "NA";
        }

        String formCode = null;
        if (student.getAssessmentFormID() != null && student.getAssessmentEntity().getAssessmentForms() != null && !student.getAssessmentEntity().getAssessmentForms().isEmpty()) {
            formCode = student.getAssessmentEntity().getAssessmentForms().stream()
                    .filter(assessmentFormEntity -> assessmentFormEntity.getAssessmentFormID().equals(student.getAssessmentFormID()))
                    .findFirst()
                    .map(AssessmentFormEntity::getFormCode)
                    .orElse(null);
        }

        return new ArrayList<>(Arrays.asList(
                student.getPen(),
                session,
                student.getGradeAtRegistration(),
                mincode,
                student.getAssessmentEntity().getAssessmentTypeCode(),
                formCode,
                sdcStudentGender,
                enrolledPrograms.contains("05") ? "1" : "0", //Francophone
                enrolledPrograms.contains("11") ? "1" : "0", // Early French Immersion
                enrolledPrograms.contains("14") ? "1" : "0", // Late French Immersion
                enrolledPrograms.contains("17") ? "1" : "0", // ELL
                sdcStudentNativeAcnestry, //Indigenous Ancestry
                collectionSnapshotDate,
                student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : null,
                student.getProvincialSpecialCaseCode() != null ? ProvincialSpecialCaseCodes.findByValue(student.getProvincialSpecialCaseCode()).isPresent() ? ProvincialSpecialCaseCodes.findByValue(student.getProvincialSpecialCaseCode()).get().getDescription() : null : null,
                student.getMcTotal() != null ? student.getMcTotal().toString() : null,
                student.getOeTotal() != null ? student.getOeTotal().toString() : null,
                student.getRawScore() != null ? student.getRawScore().toString() : null,
                student.getIrtScore()
        ));
    }

    private String calculateScaledValue(KeySummaryReportResult question) {
        if(question.getQuestionValue() != null && question.getScaleFactor() != null) {
            return String.valueOf(question.getQuestionValue().multiply(new BigDecimal(question.getScaleFactor())).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP));
        }
        return "";
    }

    private String getQuestionNumbers(AssessmentChoiceEntity choice, List<AssessmentQuestionEntity> questions) {
        return questions
                .stream()
                .filter(questionEntity -> Objects.equals(questionEntity.getAssessmentComponentEntity().getAssessmentComponentID(), choice.getAssessmentComponentEntity().getAssessmentComponentID())
                        && Objects.equals(questionEntity.getMasterQuestionNumber(), choice.getMasterQuestionNumber()))
                .map(AssessmentQuestionEntity::getQuestionNumber)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String getComponentType(AssessmentComponentEntity component) {
        if(component.getComponentTypeCode().equalsIgnoreCase("MUL_CHOICE")) {
            return "Selected Response";
        } else if(component.getComponentTypeCode().equalsIgnoreCase("OPEN_ENDED") && component.getComponentSubTypeCode().equalsIgnoreCase("ORAL")) {
            return "Constructed Response - Oral";
        }
        return "Constructed Response - Written";
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

    /**
     * Determines the ordered list of SDC collection types to use based on the assessment session month.
     *
     * @param courseMonth The course month from the session (01, 04, 06, 11)
     * @return List of collection types in order of preference
     */
    private List<String> getSDCCollectionPriorityOrder(String courseMonth) {
        return switch (courseMonth) {
            case "01", "11" ->
                    List.of(SEPTEMBER);
            case "04" ->
                    List.of(SEPTEMBER, "FEBRUARY");
            case "06" ->
                    List.of(SEPTEMBER, "FEBRUARY", "MAY");
            default ->
                    List.of(SEPTEMBER);
        };
    }

    /**
     * Finds the appropriate SDC collection and students based on the session month and fallback logic.
     * Blends data from multiple collections, prioritizing September data but using fallback collections
     * to fill in missing students. Stops fetching once all students have SDC data.
     *
     * @param lastFourCollections The available collections
     * @param assignedStudentIds The assigned student IDs to search for
     * @param courseMonth The course month from the assessment session
     * @return A map containing the selected collection and blended student data with collection mapping
     */
    private Map<String, Object> findAppropriateSDCData(PaginatedResponse<Collection> lastFourCollections, List<String> assignedStudentIds, String courseMonth) {
        List<String> collectionPriority = getSDCCollectionPriorityOrder(courseMonth);

        Map<String, SdcSchoolCollectionStudent> blendedStudentMap = new HashMap<>();
        Map<String, String> studentToCollectionSnapshotDateMap = new HashMap<>();
        Collection primaryCollection = null;
        List<String> collectionsUsed = new ArrayList<>();

        // Process collections in priority order, blending data
        for (String collectionType : collectionPriority) {
            var collection = lastFourCollections.getContent().stream()
                    .filter(c -> collectionType.equals(c.getCollectionTypeCode()))
                    .findFirst();

            if (collection.isPresent()) {
                log.info("Checking {} collection (ID: {}, Snapshot Date: {}) for SDC students", collectionType, collection.get().getCollectionID(), collection.get().getSnapshotDate());
                var sdcStudents = restUtils.get1701DataForStudents(collection.get().getCollectionID(), assignedStudentIds);

                if (!sdcStudents.isEmpty()) {
                    // Set primary collection to the first one we find with data
                    if (primaryCollection == null) {
                        primaryCollection = collection.get();
                    }

                    collectionsUsed.add(collectionType);

                    // Add students from this collection, but don't overwrite higher priority data
                    for (SdcSchoolCollectionStudent student : sdcStudents) {
                        if (!blendedStudentMap.containsKey(student.getAssignedPen())) {
                            blendedStudentMap.put(student.getAssignedPen(), student);
                            studentToCollectionSnapshotDateMap.put(student.getAssignedPen(), collection.get().getSnapshotDate());
                        }
                    }

                    log.info("Added {} students from {} collection (total unique students now: {})",
                            sdcStudents.size(), collectionType, blendedStudentMap.size());

                    // Early exit: If we have SDC data for all students, no need to check further collections
                    if (blendedStudentMap.size() >= assignedStudentIds.size()) {
                        log.info("Found SDC data for all {} students after checking {} collection(s). Stopping search early.",
                                assignedStudentIds.size(), collectionsUsed.size());
                        break;
                    }
                }
            }
        }

        // Return blended data if any students were found
        if (!blendedStudentMap.isEmpty()) {
            List<SdcSchoolCollectionStudent> blendedStudentList = new ArrayList<>(blendedStudentMap.values());

            Map<String, Object> result = new HashMap<>();
            result.put("collection", primaryCollection);
            result.put(STUDENTS_KEY, blendedStudentList);
            result.put(STUDENT_TO_COLLECTION_SNAPSHOT_DATE_MAP_KEY, studentToCollectionSnapshotDateMap);
            result.put(COLLECTION_TYPE_KEY, collectionsUsed.getFirst());
            result.put("collectionsUsed", collectionsUsed);

            log.info("Using blended data for course month {} with {} unique students from collections: {}",
                    courseMonth, blendedStudentList.size(), String.join(", ", collectionsUsed));
            return result;
        }

        // No students found in any collection - return empty result
        log.warn("No SDC students found in any available collections for course month {}", courseMonth);
        Map<String, Object> result = new HashMap<>();
        result.put("collection", null);
        result.put(STUDENTS_KEY, Collections.emptyList());
        result.put(STUDENT_TO_COLLECTION_SNAPSHOT_DATE_MAP_KEY, Collections.emptyMap());
        result.put(COLLECTION_TYPE_KEY, "NONE");
        result.put("collectionsUsed", Collections.emptyList());
        return result;
    }

    /**
     * Generate assessment student search report and stream directly to HTTP response.
     * This method provides TRUE constant memory usage by streaming from database to HTTP response without holding the entire CSV or all student entities in memory.
     *
     * @param searchCriteriaListJson JSON string with search criteria
     * @param response HTTP response to stream CSV to
     * @throws IOException if writing to response fails
     */
    public void generateAssessmentStudentSearchReportStream(String searchCriteriaListJson, jakarta.servlet.http.HttpServletResponse response) throws IOException {
        List<Sort.Order> sorts = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        Specification<AssessmentStudentEntity> specs = assessmentStudentSearchService.setSpecificationAndSortCriteria("", searchCriteriaListJson, objectMapper, sorts);

        List<String> headers = Arrays.stream(AssessmentStudentSearchReportHeader.values())
                .map(AssessmentStudentSearchReportHeader::getCode)
                .toList();

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"StudentAssessmentSearch-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv\"");

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().build();

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);
             Stream<AssessmentStudentEntity> studentStream = assessmentStudentRepository.streamAll(specs)) {

            csvPrinter.printRecord(headers);

            studentStream
                    .map(this::prepareAssessmentStudentSearchDataForCsv)
                    .forEach(csvRowData -> {
                        try {
                            csvPrinter.printRecord(csvRowData);
                            csvPrinter.flush();
                        } catch (IOException e) {
                            throw new StudentAssessmentAPIRuntimeException(e);
                        }
                    });

            csvPrinter.flush();
        }
    }

    private List<String> prepareAssessmentStudentSearchDataForCsv(AssessmentStudentEntity student) {
        // Fetch school information
        Optional<SchoolTombstone> schoolOfRecord = student.getSchoolOfRecordSchoolID() != null
                ? restUtils.getSchoolBySchoolID(student.getSchoolOfRecordSchoolID().toString())
                : Optional.empty();

        Optional<SchoolTombstone> schoolAtWrite = student.getSchoolAtWriteSchoolID() != null
                ? restUtils.getSchoolBySchoolID(student.getSchoolAtWriteSchoolID().toString())
                : Optional.empty();

        // Get assessment info
        String assessmentCode = student.getAssessmentEntity() != null
                ? student.getAssessmentEntity().getAssessmentTypeCode()
                : "";

        String assessmentSession = "";
        if (student.getAssessmentEntity() != null && student.getAssessmentEntity().getAssessmentSessionEntity() != null) {
            AssessmentSessionEntity session = student.getAssessmentEntity().getAssessmentSessionEntity();
            assessmentSession = session.getCourseYear() + "/" + session.getCourseMonth();
        }

        // Get special case description
        String specialCase = "";
        if (StringUtils.isNotBlank(student.getProvincialSpecialCaseCode())) {
            Optional<ProvincialSpecialCaseCodes> specialCaseCode = ProvincialSpecialCaseCodes.findByValue(student.getProvincialSpecialCaseCode());
            specialCase = specialCaseCode.map(ProvincialSpecialCaseCodes::getDescription).orElse(student.getProvincialSpecialCaseCode());
        }

        return new ArrayList<>(Arrays.asList(
                student.getPen() != null ? student.getPen() : "",
                student.getSurname() != null ? student.getSurname() : "",
                student.getGivenName() != null ? student.getGivenName() : "",
                student.getGradeAtRegistration() != null ? student.getGradeAtRegistration() : "",
                schoolOfRecord.map(SchoolTombstone::getMincode).orElse(""),
                schoolAtWrite.map(SchoolTombstone::getMincode).orElse(""),
                assessmentCode,
                assessmentSession,
                student.getProficiencyScore() != null ? student.getProficiencyScore().toString() : "",
                specialCase
        ));
    }
}
