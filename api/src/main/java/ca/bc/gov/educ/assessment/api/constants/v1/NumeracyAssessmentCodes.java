package ca.bc.gov.educ.assessment.api.constants.v1;

import lombok.Getter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum NumeracyAssessmentCodes {
    NME10("NME10"),
    NMF10("NMF10"),
    NME("NME"),
    NMF("NMF");

    @Getter
    private final String code;
    NumeracyAssessmentCodes(String code) {
        this.code = code;
    }

    public static List<String> getAllCodes() {
        return Arrays.stream(NumeracyAssessmentCodes.values())
                .map(NumeracyAssessmentCodes::getCode)
                .collect(Collectors.toList());
    }
}
