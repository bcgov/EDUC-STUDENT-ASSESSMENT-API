package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.rest.RestUtils;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMerge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class PENRecordService {

    private final RestUtils restUtils;

    @Autowired
    public PENRecordService(RestUtils restUtils){
        this.restUtils = restUtils;
    }

    public List<StudentMerge> getMergedStudentsForDateRange(String createDateStart, String createDateEnd){
        return restUtils.getMergedStudentsForDateRange(UUID.randomUUID(), createDateStart, createDateEnd);
    }
}
