package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.batch.processor.AssessmentKeyFileProcessor;
import ca.bc.gov.educ.eas.api.endpoint.v1.AssessmentFileUploadEndpoint;
import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentKeyFileMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentKeyFileEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentKeyFile;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentKeyFileUpload;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@Slf4j
public class AssessmentFileUploadController implements AssessmentFileUploadEndpoint {

    private static final AssessmentKeyFileMapper mapper = AssessmentKeyFileMapper.mapper;
    private final AssessmentKeyFileProcessor assessmentKeyFileProcessor;

    @Override
    public ResponseEntity<AssessmentKeyFile> processAssessmentKeyBatchFile(AssessmentKeyFileUpload fileUpload, String sessionID) {
        log.info("Running assessment key load for file: " + fileUpload.getFileName());
        AssessmentKeyFileEntity incomingFilesetEntity = assessmentKeyFileProcessor.processBatchFile(fileUpload, sessionID);
        log.info("Assessment key file data committed for file: " + fileUpload.getFileName());
        return ResponseEntity.ok(mapper.toStructure(incomingFilesetEntity));
    }
}
