package ca.bc.gov.educ.assessment.api.batch.constants;

import lombok.Getter;

@Getter
public enum AssessmentResultsBatchFile {

    TX_ID("txID"),
    EXAM_TYPE("examType"),
    COMPONENT_TYPE("componentType"),
    SEQUENCE_NUMBER("sequenceNumber"),
    CSID("csid"),
    ASSESSMENT_CODE("assessmentCode"),
    COURSE_LEVEL("courseLevel"),
    ASSESSMENT_SESSION("assessmentSession"),
    MINCODE("mincode"),
    PEN("pen"),
    DATE_OF_BIRTH("dateOfBirth"),
    NEW_STUDENT_FLAG("newStudentFlag"),
    PARTICIPATION("participation"),
    FORM_CODE("formCode"),
    FORM_CODE2("formCode2"),
    PAGE_LINK("pageLink"),
    OPEN_ENDED_MARKS("openEndedMarks"),
    MUL_CHOICE_MARKS("multiChoiceMarks"),
    CHOICE_PATH("choicePath"),
    SPECIAL_CASE_CODE("specialCaseCode"),
    PROFICIENCY_SCORE("proficiencyScore"),
    IRT_SCORE("irtScore"),
    ADAPTED_ASSESSMENT_INDICATOR("adaptedAssessmentIndicator"),
    MARKING_SESSION("markingSession");

    private final String name;

    AssessmentResultsBatchFile(String name) {
        this.name = name;
    }
}
