package ca.bc.gov.educ.assessment.api.endpoint.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.URL;
import ca.bc.gov.educ.assessment.api.struct.OnCreate;
import ca.bc.gov.educ.assessment.api.struct.OnUpdate;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentHistoryListItem;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentListItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.HttpStatus.NO_CONTENT;

@RequestMapping(URL.BASE_URL_STUDENT_HISTORY)
public interface AssessmentStudentHistoryEndpoint {

    @GetMapping(URL.PAGINATED)
    @PreAuthorize("hasAuthority('SCOPE_READ_ASSESSMENT_STUDENT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    CompletableFuture<Page<AssessmentStudentHistoryListItem>> findAll(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                                      @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                                      @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);

}
