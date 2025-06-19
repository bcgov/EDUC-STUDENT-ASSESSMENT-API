package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.batch.processor.AssessmentKeysProcessor;
import ca.bc.gov.educ.assessment.api.endpoint.v1.FileUploadEndpoint;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class FileUploadController implements FileUploadEndpoint {
    private final AssessmentKeysProcessor assessmentKeysProcessor;

    public FileUploadController(AssessmentKeysProcessor assessmentKeysProcessor) {
        this.assessmentKeysProcessor = assessmentKeysProcessor;
    }

    @Override
    public ResponseEntity<Void> processAssessmentKeysFile(AssessmentKeyFileUpload fileUpload, String session) {
        assessmentKeysProcessor.processAssessmentKeys(fileUpload, session);
        return ResponseEntity.noContent().build();
    }
}
