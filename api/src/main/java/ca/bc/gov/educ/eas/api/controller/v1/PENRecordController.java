package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.endpoint.v1.PENRecordEndpoint;
import ca.bc.gov.educ.eas.api.service.v1.PENRecordService;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMerge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class PENRecordController implements PENRecordEndpoint {
    private final PENRecordService penRecordService;

    public PENRecordController(PENRecordService penRecordService){
        this.penRecordService = penRecordService;
    }

    @Override
    public List<StudentMerge> getMergedStudentsForDateRange(String createDateStart, String createDateEnd){
        return penRecordService.getMergedStudentsForDateRange(createDateStart, createDateEnd);
    }
}
