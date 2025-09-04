package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistorySearchEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentHistory;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentShowItem;
import org.apache.commons.lang3.StringUtils;

public class AssessmentStudentMapperUtils {
    public static void setWroteFlag(AssessmentStudentEntity entity, AssessmentStudent assessmentStudent) {
        boolean hasProficiencyScore = entity.getProficiencyScore() != null;
        boolean hasSpecialCaseCode = StringUtils.isNotBlank(entity.getProvincialSpecialCaseCode()) &&
            (entity.getProvincialSpecialCaseCode().equals(ProvincialSpecialCaseCodes.NOTCOMPLETED.getCode())
                || entity.getProvincialSpecialCaseCode().equals(ProvincialSpecialCaseCodes.DISQUALIFIED.getCode()));

        assessmentStudent.setWroteFlag(hasProficiencyScore || hasSpecialCaseCode);
    }

    public static void setWroteFlag(AssessmentStudentHistorySearchEntity entity, AssessmentStudentHistory assessmentStudent) {
        boolean hasProficiencyScore = entity.getProficiencyScore() != null;
        boolean hasSpecialCaseCode = StringUtils.isNotBlank(entity.getProvincialSpecialCaseCode()) &&
                (entity.getProvincialSpecialCaseCode().equals(ProvincialSpecialCaseCodes.NOTCOMPLETED.getCode())
                        || entity.getProvincialSpecialCaseCode().equals(ProvincialSpecialCaseCodes.DISQUALIFIED.getCode()));

        assessmentStudent.setWroteFlag(hasProficiencyScore || hasSpecialCaseCode);
    }

    public static void setWroteFlag(AssessmentStudentEntity entity, AssessmentStudentShowItem assessmentStudentShowItem) {
        boolean hasProficiencyScore = entity.getProficiencyScore() != null;
        boolean hasSpecialCaseCode = StringUtils.isNotBlank(entity.getProvincialSpecialCaseCode()) &&
                (entity.getProvincialSpecialCaseCode().equals(ProvincialSpecialCaseCodes.NOTCOMPLETED.getCode())
                        || entity.getProvincialSpecialCaseCode().equals(ProvincialSpecialCaseCodes.DISQUALIFIED.getCode()));

        assessmentStudentShowItem.setWroteFlag(hasProficiencyScore || hasSpecialCaseCode);
    }
}