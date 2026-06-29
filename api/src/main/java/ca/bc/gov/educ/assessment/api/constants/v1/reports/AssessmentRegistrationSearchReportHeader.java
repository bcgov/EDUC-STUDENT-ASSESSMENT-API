package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

@Getter
public enum AssessmentRegistrationSearchReportHeader {

    SESSION("Session"),
    ASSESSMENT_CODE("Assessment Code"),
    PEN("PEN"),
    LOCAL_ID("Local ID"),
    SURNAME("Surname"),
    GIVEN_NAME("Given Name"),
    SCHOOL_OF_RECORD("School of Record"),
    ASSESSMENT_CENTRE("Assessment Centre");

    private final String code;

    AssessmentRegistrationSearchReportHeader(String code) {
        this.code = code;
    }
}
