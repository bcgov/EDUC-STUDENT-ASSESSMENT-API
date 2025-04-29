package ca.bc.gov.educ.assessment.api.service.v1.external;

import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;

public interface IDownstreamSyncService {
    void publishRegistration(AssessmentStudent assessmentStudent);
}
