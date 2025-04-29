package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.endpoint.v1.AssessmentEndpoint;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentService;
import ca.bc.gov.educ.assessment.api.struct.v1.Assessment;
import ca.bc.gov.educ.assessment.api.util.RequestUtil;
import ca.bc.gov.educ.assessment.api.util.ValidationUtil;
import ca.bc.gov.educ.assessment.api.validator.AssessmentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AssessmentController implements AssessmentEndpoint {

    private final AssessmentService assessmentService;
    private final AssessmentValidator validator;
    private static final AssessmentMapper mapper = AssessmentMapper.mapper;

    @Autowired
    public AssessmentController(AssessmentService assessmentService, AssessmentValidator assessmentValidator) {
        this.assessmentService = assessmentService;
        this.validator = assessmentValidator;
    }

    @Override
    public Assessment getAssessment(UUID assessmentID){
        return mapper.toStructure(assessmentService.getAssessment(assessmentID));
    }

    @Override
    public Assessment createAssessment(Assessment assessment) {
        ValidationUtil.validatePayload(() -> validator.validatePayload(assessment, true));
        RequestUtil.setAuditColumnsForCreate(assessment);
        AssessmentEntity assessmentEntity = mapper.toEntity(assessment);
        return mapper.toStructure(assessmentService.createAssessment(assessmentEntity));
    }

    @Override
    public Assessment updateAssessment(UUID assessmentID, Assessment assessment) {
        ValidationUtil.validatePayload(() -> validator.validatePayload(assessment, false));
        RequestUtil.setAuditColumnsForUpdate(assessment);
        return mapper.toStructure(assessmentService.updateAssessment(mapper.toEntity(assessment)));
    }

    @Override
    public ResponseEntity<Void> deleteAssessment(UUID assessmentID){
        this.assessmentService.deleteAssessment(assessmentID);
        return ResponseEntity.noContent().build();
    }

}
