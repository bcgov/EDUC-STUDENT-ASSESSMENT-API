package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.endpoint.v1.CodeTableAPIEndpoint;
import ca.bc.gov.educ.eas.api.mappers.v1.CodeTableMapper;
import ca.bc.gov.educ.eas.api.rules.assessment.AssessmentStudentValidationIssueTypeCode;
import ca.bc.gov.educ.eas.api.service.v1.CodeTableService;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentTypeCode;
import ca.bc.gov.educ.eas.api.struct.v1.ProvincialSpecialCaseCode;
import ca.bc.gov.educ.eas.api.struct.v1.ValidationIssueTypeCode;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@AllArgsConstructor
public class CodeTableAPIController  implements CodeTableAPIEndpoint {

    private final CodeTableService codeTableService;
    private static final CodeTableMapper mapper = CodeTableMapper.mapper;

    @Override
    public List<AssessmentTypeCode> getAssessTypeCodes() {
        return codeTableService.getAllAssessmentTypeCodes().stream().map(mapper::toStructure).toList();
    }

    @Override
    public List<ProvincialSpecialCaseCode> getProvincialSpecialCaseCodes() {
        return codeTableService.getAllProvincialSpecialCaseCodes().stream().map(mapper::toStructure).toList();
    }

    @Override
    public List<ValidationIssueTypeCode> getAssessmentStudentValidationIssueTypeCodes() {
        List<ValidationIssueTypeCode> validationIssues = new ArrayList<>();

        for (var code : AssessmentStudentValidationIssueTypeCode.values()) {
            ValidationIssueTypeCode issue = new ValidationIssueTypeCode();
            issue.setValidationIssueTypeCode(code.getCode());
            issue.setMessage(code.getMessage());

            validationIssues.add(issue);
        }

        return  validationIssues;
    }
}
