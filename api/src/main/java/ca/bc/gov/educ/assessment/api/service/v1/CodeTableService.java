package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static javax.management.timer.Timer.ONE_DAY;

@Service
@AllArgsConstructor
public class CodeTableService {

    private final AssessmentSessionRepository assessmentSessionRepository;
    private final AssessmentTypeCodeRepository assessmentTypeCodeRepository;
    private final ProvincialSpecialCaseCodeRepository provincialSpecialCaseCodeRepository;
    private final AdaptedAssessmentIndicatorCodeRepository adaptedAssessmentIndicatorCodeRepository;
    private final ClaimCodeRepository claimCodeRepository;
    private final CognitiveLevelCodeRepository cognitiveLevelCodeRepository;
    private final ConceptsCodeRepository conceptsCodeRepository;
    private final ContextCodeRepository contextCodeRepository;
    private final TaskCodeRepository taskCodeRepository;

    @Cacheable("assessmentTypeCodes")
    public List<AssessmentTypeCodeEntity> getAllAssessmentTypeCodes() {
        return assessmentTypeCodeRepository.findAll();
    }
    
    public Map<String, String> getAllAssessmentTypeCodesAsMap() {
        var allCodes = getAllAssessmentTypeCodes();
        return allCodes.stream().collect(Collectors.toMap(AssessmentTypeCodeEntity::getAssessmentTypeCode, AssessmentTypeCodeEntity::getDescription));
    }

    @Scheduled(fixedRate = ONE_DAY, initialDelayString = "${timing.initialDelay}")
    @Cacheable("assessmentSessions")
    public List<AssessmentSessionEntity> getAllAssessmentSessionCodes() {
        return assessmentSessionRepository.findAll();
    }

    @Cacheable("adaptedAssessmentIndicatorCodes")
    public List<AdaptedAssessmentIndicatorCodeEntity> getAdaptedAssessmentIndicatorCodes() {
        return adaptedAssessmentIndicatorCodeRepository.findAll();
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
