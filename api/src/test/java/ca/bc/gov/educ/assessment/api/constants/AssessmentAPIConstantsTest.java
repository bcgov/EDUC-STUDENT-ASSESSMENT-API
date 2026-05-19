package ca.bc.gov.educ.assessment.api.constants;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.constants.v1.CourseStatusCodes;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentAPIConstantsTest extends BaseAssessmentAPITest {

    @Test
    void test_AssessmentTypeCodes() {
        Optional<AssessmentTypeCodes> assessmentTypeCode = AssessmentTypeCodes.findByValue("LTE10");
        assertThat(assessmentTypeCode.get().name()).isEqualTo(AssessmentTypeCodes.LTE10.name());
    }

    @Test
    void test_AssessmentTypeCodesSortOrderFor_KnownCodes_UsesEnumOrder() {
        List<String> reportOrder = List.of(
                AssessmentTypeCodes.LTE10.getCode(),
                AssessmentTypeCodes.LTP10.getCode(),
                AssessmentTypeCodes.NME10.getCode(),
                AssessmentTypeCodes.NMF10.getCode(),
                AssessmentTypeCodes.LTE12.getCode(),
                AssessmentTypeCodes.LTP12.getCode(),
                AssessmentTypeCodes.LTF12.getCode()
        );

        assertThat(reportOrder.stream()
                .sorted(Comparator.comparingInt(AssessmentTypeCodes::sortOrderFor))
                .toList()).containsExactlyElementsOf(reportOrder);
        assertThat(AssessmentTypeCodes.sortOrderFor(AssessmentTypeCodes.NME.getCode()))
                .isGreaterThan(AssessmentTypeCodes.sortOrderFor(AssessmentTypeCodes.NME10.getCode()))
                .isLessThan(AssessmentTypeCodes.sortOrderFor(AssessmentTypeCodes.NMF10.getCode()));
        assertThat(AssessmentTypeCodes.sortOrderFor(AssessmentTypeCodes.NMF.getCode()))
                .isGreaterThan(AssessmentTypeCodes.sortOrderFor(AssessmentTypeCodes.NMF10.getCode()))
                .isLessThan(AssessmentTypeCodes.sortOrderFor(AssessmentTypeCodes.LTE12.getCode()));
    }

    @Test
    void test_AssessmentTypeCodesSortOrderFor_UnknownCode_UsesMaxValue() {
        assertThat(AssessmentTypeCodes.sortOrderFor("UNKNOWN")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void test_CourseStatusCodes() {
        Optional<CourseStatusCodes> courseStatusCode = CourseStatusCodes.findByValue("A");
        assertThat(courseStatusCode.get().name()).isEqualTo(CourseStatusCodes.ACTIVE.name());
    }

}
