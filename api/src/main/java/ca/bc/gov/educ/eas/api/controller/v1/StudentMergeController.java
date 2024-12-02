package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.endpoint.v1.StudentMergeEndpoint;
import ca.bc.gov.educ.eas.api.service.v1.StudentMergeService;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMerge;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMergeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class StudentMergeController implements StudentMergeEndpoint {
    private final StudentMergeService studentMergeService;

    public StudentMergeController(StudentMergeService studentMergeService){
        this.studentMergeService = studentMergeService;
    }

    @Override
    public List<StudentMergeResult> getMergedStudentsForDateRange(String createDateStart, String createDateEnd){
        return studentMergeService.getMergedStudentsForDateRange(createDateStart, createDateEnd);
    }
}
