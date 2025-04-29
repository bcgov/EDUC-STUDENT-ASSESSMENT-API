package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.endpoint.v1.CodeTableAPIEndpoint;
import ca.bc.gov.educ.assessment.api.mappers.v1.CodeTableMapper;
import ca.bc.gov.educ.assessment.api.service.v1.CodeTableService;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentTypeCode;
import ca.bc.gov.educ.assessment.api.struct.v1.ProvincialSpecialCaseCode;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

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
}
