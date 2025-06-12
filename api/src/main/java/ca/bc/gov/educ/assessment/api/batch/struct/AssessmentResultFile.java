package ca.bc.gov.educ.assessment.api.batch.struct;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssessmentResultFile {

    private List<AssessmentResultsDetails> assessmentResultData;

    public List<AssessmentResultDetails> getAssessmentKeyData() {
        if(assessmentResultData == null){
            assessmentResultData = new ArrayList<>();
        }
        return assessmentResultData;
    }
}
