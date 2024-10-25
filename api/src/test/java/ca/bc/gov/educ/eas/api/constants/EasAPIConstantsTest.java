package ca.bc.gov.educ.eas.api.constants;

import ca.bc.gov.educ.eas.api.BaseEasAPITest;
import ca.bc.gov.educ.eas.api.constants.v1.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EasAPIConstantsTest extends BaseEasAPITest {

    @Test
    void test_AssessmentStudentStatusCodes() {
        Optional<AssessmentStudentStatusCodes> assessmentStudentStatusCode = AssessmentStudentStatusCodes.findByValue("LOADED");
        assertThat(assessmentStudentStatusCode.get().name()).isEqualTo(AssessmentStudentStatusCodes.LOADED.name());
    }

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
