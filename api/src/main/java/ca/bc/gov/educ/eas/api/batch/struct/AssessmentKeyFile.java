package ca.bc.gov.educ.eas.api.batch.struct;

import java.util.ArrayList;
import java.util.List;

public class AssessmentKeyFile {

    private List<AssessmentKeyDetail> assessmentKeyData;

    public List<AssessmentKeyDetail> getAssessmentKeyData() {
        if(assessmentKeyData == null){
            assessmentKeyData = new ArrayList<>();
        }
        return assessmentKeyData;
    }
}
