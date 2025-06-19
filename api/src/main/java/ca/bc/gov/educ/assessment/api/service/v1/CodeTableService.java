package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CodeTableService {

    private final AssessmentTypeCodeRepository assessmentTypeCodeRepository;
    private final ProvincialSpecialCaseCodeRepository provincialSpecialCaseCodeRepository;

    private final ClaimCodeRepository claimCodeRepository;
    private final CognitiveLevelCodeRepository cognitiveLevelCodeRepository;
    private final ConceptsCodeRepository conceptsCodeRepository;
    private final ContextCodeRepository contextCodeRepository;
    private final TaskCodeRepository taskCodeRepository;

    @Cacheable("assessmentTypeCodes")
    public List<AssessmentTypeCodeEntity> getAllAssessmentTypeCodes() {
        return assessmentTypeCodeRepository.findAll();
    }

    @Cacheable("provincialSpecialCaseCodes")
    public List<ProvincialSpecialCaseCodeEntity> getAllProvincialSpecialCaseCodes() {
        return provincialSpecialCaseCodeRepository.findAll();
    }

    @Cacheable("claimCodes")
    public List<ClaimCodeEntity> getAllClaimCodes() {
        return claimCodeRepository.findAll();
    }

    @Cacheable("cognitiveLevelCodes")
    public List<CognitiveLevelCodeEntity> getAllCognitiveLevelCodes() {
        return cognitiveLevelCodeRepository.findAll();
    }

    @Cacheable("conceptCodes")
    public List<ConceptCodeEntity> getAllConceptCodes() {
        return conceptsCodeRepository.findAll();
    }

    @Cacheable("contextCodes")
    public List<ContextCodeEntity> getAllContextCodes() {
        return contextCodeRepository.findAll();
    }

    @Cacheable("taskCodes")
    public List<TaskCodeEntity> getAllTaskCodes() {
        return taskCodeRepository.findAll();
    }

}
