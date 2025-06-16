package ca.bc.gov.educ.assessment.api.constants;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.CourseStatusCodes;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentAPIConstantsTest extends BaseAssessmentAPITest {

    @Test
    void test_AssessmentTypeCodes() {
        Optional<AssessmentTypeCodes> assessmentTypeCode = AssessmentTypeCodes.findByValue("LTE10");
        assertThat(assessmentTypeCode.get().name()).isEqualTo(AssessmentTypeCodes.LTE10.name());
    }

    @Test
    void test_CourseStatusCodes() {
        Optional<CourseStatusCodes> courseStatusCode = CourseStatusCodes.findByValue("A");
        assertThat(courseStatusCode.get().name()).isEqualTo(CourseStatusCodes.ACTIVE.name());
    }

}
