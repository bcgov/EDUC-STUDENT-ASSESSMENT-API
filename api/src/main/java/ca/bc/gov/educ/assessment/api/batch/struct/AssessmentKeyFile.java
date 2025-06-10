package ca.bc.gov.educ.assessment.api.batch.struct;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AssessmentKeyFile {

    private List<AssessmentKeyDetails> assessmentKeyData;

    public List<AssessmentKeyDetails> getAssessmentKeyData() {
        if(assessmentKeyData == null){
            assessmentKeyData = new ArrayList<>();
        }
        return assessmentKeyData;
    }
}
