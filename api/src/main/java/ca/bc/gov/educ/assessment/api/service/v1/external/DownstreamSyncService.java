package ca.bc.gov.educ.assessment.api.service.v1.external;

import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DownstreamSyncService implements IDownstreamSyncService {

    @Override
    public void publishRegistration(AssessmentStudent assessmentStudent) {
        //Implementation
    }
}
