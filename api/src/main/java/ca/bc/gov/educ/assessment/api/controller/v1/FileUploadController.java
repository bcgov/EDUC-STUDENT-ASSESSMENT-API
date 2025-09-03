package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.batch.processor.AssessmentKeysProcessor;
import ca.bc.gov.educ.assessment.api.batch.processor.AssessmentResultsProcessor;
import ca.bc.gov.educ.assessment.api.endpoint.v1.FileUploadEndpoint;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultFileUpload;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultsSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
public class FileUploadController implements FileUploadEndpoint {
    private final AssessmentKeysProcessor assessmentKeysProcessor;
    private final AssessmentResultsProcessor assessmentResultsProcessor;
    private final AssessmentStudentService studentService;

    public FileUploadController(AssessmentKeysProcessor assessmentKeysProcessor, AssessmentResultsProcessor assessmentResultsProcessor, AssessmentStudentService studentService) {
        this.assessmentKeysProcessor = assessmentKeysProcessor;
        this.assessmentResultsProcessor = assessmentResultsProcessor;
        this.studentService = studentService;
    }

    @Override
    public ResponseEntity<Void> processAssessmentKeysFile(AssessmentKeyFileUpload fileUpload, UUID sessionID) {
        assessmentKeysProcessor.processAssessmentKeys(fileUpload, sessionID);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> processAssessmentResultsFile(AssessmentResultFileUpload fileUpload, String session) {
        assessmentResultsProcessor.processAssessmentResults(fileUpload, UUID.fromString(session), fileUpload.getIsSingleUpload().equalsIgnoreCase("Y"));
        return ResponseEntity.noContent().build();
    }

    @Override
    public List<AssessmentResultsSummary> getAssessmentResultsUploadSummary(UUID sessionID) {
        return studentService.getResultsUploadSummary(sessionID);
    }
}
