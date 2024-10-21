package ca.bc.gov.educ.eas.api.endpoint.v1;

import ca.bc.gov.educ.eas.api.constants.v1.URL;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMerge;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequestMapping(URL.BASE_URL + "/pen-records/createDateInRange")
public interface PENRecordEndpoint {

    @GetMapping()
    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_STUDENT')")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Tag(name = "PEN Records", description = "Endpoints to retrieve PEN records.")
    List<StudentMerge> getMergedStudentsForDateRange(@RequestParam(name = "createDateStart") String createDateStart, @RequestParam(name = "createDateEnd") String createDateEnd) throws JsonProcessingException;

}