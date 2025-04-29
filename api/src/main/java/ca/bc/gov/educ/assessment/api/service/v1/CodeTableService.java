package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentTypeCodeEntity;
import ca.bc.gov.educ.assessment.api.model.v1.ProvincialSpecialCaseCodeEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentTypeCodeRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.ProvincialSpecialCaseCodeRepository;
import org.springframework.cache.annotation.Cacheable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CodeTableService {

    private final AssessmentTypeCodeRepository assessmentTypeCodeRepository;
    private final ProvincialSpecialCaseCodeRepository provincialSpecialCaseCodeRepository;

    @Cacheable("assessmentTypeCodes")
    public List<AssessmentTypeCodeEntity> getAllAssessmentTypeCodes() {
        return assessmentTypeCodeRepository.findAll();
    }

    @Cacheable("provincialSpecialCaseCodes")
    public List<ProvincialSpecialCaseCodeEntity> getAllProvincialSpecialCaseCodes() {
        return provincialSpecialCaseCodeRepository.findAll();
    }

}
