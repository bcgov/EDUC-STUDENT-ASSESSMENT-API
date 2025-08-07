package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.reports.AssessmentRegistrationTotalsBySchoolHeader;
import ca.bc.gov.educ.assessment.api.constants.v1.reports.RegistrationSummaryHeader;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentLightEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentLightRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.AssessmentRegistrationTotalsBySchoolResult;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.RegistrationSummaryResult;
import ca.bc.gov.educ.assessment.api.struct.v1.reports.SimpleHeadcountResultsTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static ca.bc.gov.educ.assessment.api.constants.v1.reports.RegistrationSummaryHeader.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SummaryReportService {

    private static final String SESSION_ID = "sessionID";
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final AssessmentStudentLightRepository assessmentStudentLightRepository;
    private final RestUtils restUtils;

    public SimpleHeadcountResultsTable getRegistrationSummaryCount(UUID sessionID) {

        AssessmentSessionEntity validSession =
                assessmentSessionRepository.findById(sessionID)
                        .orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        List<UUID> assessmentIDs = validSession.getAssessments().stream().map(AssessmentEntity::getAssessmentID).toList();

        Optional<AssessmentStudentLightEntity> studentEntityOpt = assessmentStudentLightRepository.findBySessionIDAndDownloadDateIsNotNull(sessionID);
        List<RegistrationSummaryResult> summaryResults;
        if (studentEntityOpt.isPresent()) {
            summaryResults = assessmentStudentRepository.getRegistrationSummaryByAssessmentIDsAndDownloadDateNotNull(assessmentIDs);
        } else {
            summaryResults = assessmentStudentRepository.getRegistrationSummaryByAssessmentIDsAndDownloadDateNull(assessmentIDs);
        }

        SimpleHeadcountResultsTable resultsTable = new SimpleHeadcountResultsTable();
        var headerList = new ArrayList<String>();
        for (RegistrationSummaryHeader header : RegistrationSummaryHeader.values()) {
            headerList.add(header.getCode());
        }
        resultsTable.setHeaders(headerList);

        var rows = new ArrayList<Map<String, String>>();

        summaryResults.forEach(result -> {
            var rowMap = new HashMap<String, String>();

            Optional<AssessmentEntity> assessment = validSession.getAssessments().stream().filter(
                    assessmentEntity -> Objects.equals(assessmentEntity.getAssessmentID().toString(), result.getAssessmentID())).findFirst();
            rowMap.put(ASSESSMENT_TYPE.getCode(), assessment.get().getAssessmentTypeCode());
            rowMap.put(GRADE_08_COUNT.getCode(), result.getGrade8Count());
            rowMap.put(GRADE_09_COUNT.getCode(), result.getGrade9Count());
            rowMap.put(GRADE_10_COUNT.getCode(), result.getGrade10Count());
            rowMap.put(GRADE_11_COUNT.getCode(), result.getGrade11Count());
            rowMap.put(GRADE_12_COUNT.getCode(), result.getGrade12Count());
            rowMap.put(GRADE_AD_COUNT.getCode(), result.getGradeADCount());
            rowMap.put(GRADE_OT_COUNT.getCode(), result.getGradeOTCount());
            rowMap.put(GRADE_HS_COUNT.getCode(), result.getGradeHSCount());
            rowMap.put(GRADE_AN_COUNT.getCode(), result.getGradeANCount());
            rowMap.put(TOTAL.getCode(), result.getTotal());
            rows.add(rowMap);
        });
        rows.add(createTotalRow(rows));
        resultsTable.setRows(rows);
        return resultsTable;
    }

    public SimpleHeadcountResultsTable getAssessmentRegistrationTotalsBySchool(UUID sessionID) {
        AssessmentSessionEntity validSession =  assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, SESSION_ID, sessionID.toString()));
        List<UUID> assessmentIDs = new ArrayList<>();
        HashMap<String, AssessmentEntity> validAssessments = new HashMap<>();
        validSession.getAssessments().forEach(assessment -> {
            assessmentIDs.add(assessment.getAssessmentID());
            validAssessments.put(String.valueOf(assessment.getAssessmentID()), assessment);
        });

        List<AssessmentRegistrationTotalsBySchoolResult> registrations;
        Optional<AssessmentStudentLightEntity> studentEntityOpt = assessmentStudentLightRepository.findBySessionIDAndDownloadDateIsNotNull(sessionID);
        if (studentEntityOpt.isPresent()) {
            registrations = assessmentStudentRepository.getRegistrationSummaryByAssessmentIDsAndSchoolIDsAndDownloadDateIsNotNull(assessmentIDs);
        } else {
            registrations = assessmentStudentRepository.getRegistrationSummaryByAssessmentIDsAndSchoolIDs(assessmentIDs);
        }

        HashMap<String, Integer> headers = new  HashMap<>();
        headers.put(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode(), AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getOrder());
        headers.put(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode(), AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getOrder());
        headers.put(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode(), AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getOrder());

        SimpleHeadcountResultsTable resultsTable = new SimpleHeadcountResultsTable();
        ArrayList<Map<String, String>> rows = new ArrayList<>();

        registrations.forEach(result -> {
            HashMap<String, Integer> neededHeaders = generateNeededHeadersForAssessmentRegistrationTotalsBySchoolResult(result);
            headers.putAll(neededHeaders);
            HashMap<String, String> rowMap = generateRowForAssessmentRegistrationTotalsBySchoolResult(result, validAssessments);
            rows.add(rowMap);
        });
        resultsTable.setHeaders(headers.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList());
        resultsTable.setRows(rows);
        return resultsTable;
    }

    private HashMap<String, String> createTotalRow(ArrayList<Map<String, String>> rows) {
        var rowMap = new HashMap<String, String>();
        rowMap.put(ASSESSMENT_TYPE.getCode(), "TOTAL");
        rowMap.put(GRADE_08_COUNT.getCode(), getTotalByGrade(GRADE_08_COUNT.getCode(), rows));
        rowMap.put(GRADE_09_COUNT.getCode(), getTotalByGrade(GRADE_09_COUNT.getCode(), rows));
        rowMap.put(GRADE_10_COUNT.getCode(), getTotalByGrade(GRADE_10_COUNT.getCode(), rows));
        rowMap.put(GRADE_11_COUNT.getCode(), getTotalByGrade(GRADE_11_COUNT.getCode(), rows));
        rowMap.put(GRADE_12_COUNT.getCode(), getTotalByGrade(GRADE_12_COUNT.getCode(), rows));
        rowMap.put(GRADE_AD_COUNT.getCode(), getTotalByGrade(GRADE_AD_COUNT.getCode(), rows));
        rowMap.put(GRADE_OT_COUNT.getCode(), getTotalByGrade(GRADE_OT_COUNT.getCode(), rows));
        rowMap.put(GRADE_HS_COUNT.getCode(), getTotalByGrade(GRADE_HS_COUNT.getCode(), rows));
        rowMap.put(GRADE_AN_COUNT.getCode(), getTotalByGrade(GRADE_AN_COUNT.getCode(), rows));
        rowMap.put(TOTAL.getCode(), getTotalByGrade(TOTAL.getCode(), rows));

        return rowMap;
    }

    private String getTotalByGrade(String grade, ArrayList<Map<String, String>> rows) {
        List<String> totalList = rows.stream().map(row -> row.entrySet().stream().filter(result -> result.getKey().equalsIgnoreCase(grade)).findFirst())
                .map(Optional::get)
                .map(Map.Entry::getValue)
                .toList();

        int total =  totalList.stream().mapToInt(Integer::valueOf).sum();
        return String.valueOf(total);
    }

    private HashMap<String, String> generateRowForAssessmentRegistrationTotalsBySchoolResult(AssessmentRegistrationTotalsBySchoolResult result, HashMap<String, AssessmentEntity> validAssessments) {
        HashMap<String, String> rowMap = new HashMap<>();
        Optional<SchoolTombstone> schoolTombstone = this.restUtils.getSchoolBySchoolID(result.getSchoolOfRecordSchoolID());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.ASSESSMENT_TYPE.getCode(), validAssessments.containsKey(result.getAssessmentID()) ? validAssessments.get(result.getAssessmentID()).getAssessmentTypeCode() : result.getAssessmentID());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.SCHOOL.getCode(), schoolTombstone.isPresent() ? schoolTombstone.get().getMincode() : result.getSchoolOfRecordSchoolID());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_08_COUNT.getCode(), result.getGrade8Count());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_09_COUNT.getCode(), result.getGrade9Count());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode(),  result.getGrade10Count());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_11_COUNT.getCode(), result.getGrade11Count());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_12_COUNT.getCode(), result.getGrade12Count());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_AD_COUNT.getCode(), result.getGradeADCount());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_OT_COUNT.getCode(), result.getGradeOTCount());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_HS_COUNT.getCode(), result.getGradeHSCount());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_AN_COUNT.getCode(), result.getGradeANCount());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.BLANK_GRADE_COUNT.getCode(), result.getBlankGradeCount());
        rowMap.put(AssessmentRegistrationTotalsBySchoolHeader.TOTAL.getCode(), result.getTotal());
        return rowMap;
    }

    private HashMap<String, Integer> generateNeededHeadersForAssessmentRegistrationTotalsBySchoolResult(AssessmentRegistrationTotalsBySchoolResult result) {
        HashMap<String, Integer> neededHeaders = new HashMap<>();
        if (!result.getGrade8Count().equals("0")) {
            neededHeaders.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_08_COUNT.getCode(), AssessmentRegistrationTotalsBySchoolHeader.GRADE_08_COUNT.getOrder());
        }
        if (!result.getGrade9Count().equals("0")) {
            neededHeaders.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_09_COUNT.getCode(), AssessmentRegistrationTotalsBySchoolHeader.GRADE_09_COUNT.getOrder());
        }
        if (!result.getGrade10Count().equals("0")) {
            neededHeaders.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getCode(), AssessmentRegistrationTotalsBySchoolHeader.GRADE_10_COUNT.getOrder());
        }
        if (!result.getGrade11Count().equals("0")) {
            neededHeaders.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_11_COUNT.getCode(), AssessmentRegistrationTotalsBySchoolHeader.GRADE_11_COUNT.getOrder());
        }
        if (!result.getGrade12Count().equals("0")) {
            neededHeaders.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_12_COUNT.getCode(), AssessmentRegistrationTotalsBySchoolHeader.GRADE_12_COUNT.getOrder());
        }
        if (!result.getGradeADCount().equals("0")) {
            neededHeaders.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_AD_COUNT.getCode(), AssessmentRegistrationTotalsBySchoolHeader.GRADE_AD_COUNT.getOrder());
        }
        if (!result.getGradeOTCount().equals("0")) {
            neededHeaders.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_OT_COUNT.getCode(), AssessmentRegistrationTotalsBySchoolHeader.GRADE_OT_COUNT.getOrder());
        }
        if (!result.getGradeHSCount().equals("0")) {
            neededHeaders.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_HS_COUNT.getCode(), AssessmentRegistrationTotalsBySchoolHeader.GRADE_HS_COUNT.getOrder());
        }
        if (!result.getGradeANCount().equals("0")) {
            neededHeaders.put(AssessmentRegistrationTotalsBySchoolHeader.GRADE_AN_COUNT.getCode(), AssessmentRegistrationTotalsBySchoolHeader.GRADE_AN_COUNT.getOrder());
        }
        if (!result.getBlankGradeCount().equals("0")) {
            neededHeaders.put(AssessmentRegistrationTotalsBySchoolHeader.BLANK_GRADE_COUNT.getCode(), AssessmentRegistrationTotalsBySchoolHeader.BLANK_GRADE_COUNT.getOrder());
        }
        return neededHeaders;
    }
}
