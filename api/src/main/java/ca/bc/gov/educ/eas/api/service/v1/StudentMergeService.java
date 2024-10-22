package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.rest.RestUtils;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMerge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class StudentMergeService {

    private final RestUtils restUtils;

    @Autowired
    public StudentMergeService(RestUtils restUtils){
        this.restUtils = restUtils;
    }

    public List<StudentMerge> getMergedStudentsForDateRange(String createDateStart, String createDateEnd){
        UUID correlationID = UUID.randomUUID();
        log.info("Fetching student merge records for correlation ID: {}", correlationID);
        return restUtils.getMergedStudentsForDateRange(correlationID, createDateStart, createDateEnd);
    }
}
