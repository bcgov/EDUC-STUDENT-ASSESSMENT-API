package ca.bc.gov.educ.assessment.api.batch.struct;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssessmentResultFile {

    private List<AssessmentResultDetails> assessmentResultData;

    public List<AssessmentResultDetails> getAssessmentResultData() {
        if(assessmentResultData == null){
            assessmentResultData = new ArrayList<>();
        }
        return assessmentResultData;
    }
}