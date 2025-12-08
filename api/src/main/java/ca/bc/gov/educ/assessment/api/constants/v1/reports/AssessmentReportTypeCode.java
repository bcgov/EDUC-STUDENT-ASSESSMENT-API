package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum AssessmentReportTypeCode {
    ALL_SESSION_REGISTRATIONS("ALL_SESSION_REGISTRATIONS"),
    SCHOOL_STUDENTS_IN_SESSION("SCHOOL_STUDENTS_IN_SESSION"),
    SCHOOL_STUDENTS_BY_ASSESSMENT("SCHOOL_STUDENTS_BY_ASSESSMENT"),
    ATTEMPTS("ATTEMPTS"),
    PEN_MERGES("PEN_MERGES"),
    XAM_FILE("XAM_FILE"),
    SESSION_RESULTS("SESSION_RESULTS"),
    REGISTRATION_SUMMARY("registration-summary"),
    REGISTRATION_SUMMARY_BY_SCHOOL("registration-summary-by-school"),
    REGISTRATION_DETAIL_CSV("registration-detail-csv"),
    ALL_DETAILED_STUDENTS_IN_SESSION_CSV("all-detailed-students-in-session-csv"),
    SUMMARY_BY_GRADE_FOR_SESSION("summary-by-grade-for-session"),
    SUMMARY_BY_FORM_FOR_SESSION("summary-by-form-for-session"),
    PEN_ISSUES_CSV("pen-issues-csv"),
    NME_DETAILED_DOAR("nme-detailed-doar"),
    NMF_DETAILED_DOAR("nmf-detailed-doar"),
    LTE10_DETAILED_DOAR("lte10-detailed-doar"),
    LTE12_DETAILED_DOAR("lte12-detailed-doar"),
    LTP10_DETAILED_DOAR("ltp10-detailed-doar"),
    LTP12_DETAILED_DOAR("ltp12-detailed-doar"),
    LTF12_DETAILED_DOAR("ltf12-detailed-doar"),
    NME_KEY_SUMMARY("nme-key-summary"),
    NMF_KEY_SUMMARY("nmf-key-summary"),
    LTE10_KEY_SUMMARY("lte10-key-summary"),
    LTE12_KEY_SUMMARY("lte12-key-summary"),
    LTP10_KEY_SUMMARY("ltp10-key-summary"),
    LTP12_KEY_SUMMARY("ltp12-key-summary"),
    LTF12_KEY_SUMMARY("ltf12-key-summary"),
    NME_ITEM_ANALYSIS("NME10"),
    NMF_ITEM_ANALYSIS("NMF10"),
    LTE10_ITEM_ANALYSIS("LTE10"),
    LTE12_ITEM_ANALYSIS("LTE12"),
    LTP10_ITEM_ANALYSIS("LTP10"),
    LTP12_ITEM_ANALYSIS("LTP12"),
    LTF12_ITEM_ANALYSIS("LTF12"),
    DOAR_SUMMARY("doar-summary"),
    DOAR_PROVINCIAL_SUMMARY("doar-prov-summary"),
    ASSESSMENT_STUDENT_SEARCH_CSV("assessment-student-search-csv"),
    ;
    private final String code;
    AssessmentReportTypeCode(String code) { this.code = code; }

    public static Optional<AssessmentReportTypeCode> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.code).contains(value)).findFirst();
    }
}
