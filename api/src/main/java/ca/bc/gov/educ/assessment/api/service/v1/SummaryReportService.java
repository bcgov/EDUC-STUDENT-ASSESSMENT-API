package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.reports.RegistrationSummaryHeader;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
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

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;

    public SimpleHeadcountResultsTable getRegistrationSummaryCount(UUID sessionID) {

        AssessmentSessionEntity validSession =
                assessmentSessionRepository.findById(sessionID)
                        .orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "sessionID", sessionID.toString()));
        List<UUID> assessmentIDs = validSession.getAssessments().stream().map(AssessmentEntity::getAssessmentID).toList();
        List<RegistrationSummaryResult> summaryResults = assessmentStudentRepository.getRegistrationSummaryByAssessmentIDs(assessmentIDs);

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
}
