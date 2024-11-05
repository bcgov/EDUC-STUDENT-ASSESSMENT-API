package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.endpoint.v1.AssessmentEndpoint;
import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentMapper;
import ca.bc.gov.educ.eas.api.service.v1.AssessmentService;
import ca.bc.gov.educ.eas.api.struct.v1.Assessment;
import ca.bc.gov.educ.eas.api.util.RequestUtil;
import ca.bc.gov.educ.eas.api.util.ValidationUtil;
import ca.bc.gov.educ.eas.api.validator.AssessmentValidator;
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
