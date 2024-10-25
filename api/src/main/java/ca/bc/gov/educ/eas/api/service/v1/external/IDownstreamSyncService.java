package ca.bc.gov.educ.eas.api.service.v1.external;

import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;

public interface IDownstreamSyncService {
    void publishRegistration(AssessmentStudent assessmentStudent);
}
