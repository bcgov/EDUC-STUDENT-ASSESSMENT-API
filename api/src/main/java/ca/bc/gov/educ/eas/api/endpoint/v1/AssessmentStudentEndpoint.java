package ca.bc.gov.educ.eas.api.endpoint.v1;

import ca.bc.gov.educ.eas.api.constants.v1.URL;
import ca.bc.gov.educ.eas.api.struct.OnCreate;
import ca.bc.gov.educ.eas.api.struct.OnUpdate;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudentListItem;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequestMapping(URL.BASE_URL_STUDENT)
public interface AssessmentStudentEndpoint {

    @GetMapping("/{assessmentStudentID}")
    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_STUDENT')")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    AssessmentStudent readStudent(@PathVariable UUID assessmentStudentID);

    @PutMapping("/{assessmentStudentID}")
    @PreAuthorize("hasAuthority('SCOPE_WRITE_EAS_STUDENT')")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    AssessmentStudent updateStudent(@Validated({Default.class, OnUpdate.class}) @RequestBody AssessmentStudent assessmentStudent, @PathVariable UUID assessmentStudentID);

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_WRITE_EAS_STUDENT')")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "NOT FOUND"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    AssessmentStudent createStudent(@Validated({Default.class, OnCreate.class}) @RequestBody AssessmentStudent assessmentStudent);

    @GetMapping(URL.PAGINATED)
    @PreAuthorize("hasAuthority('SCOPE_READ_EAS_STUDENT')")
    @Transactional(readOnly = true)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "400", description = "BAD REQUEST"), @ApiResponse(responseCode = "500", description = "INTERNAL SERVER ERROR.")})
    CompletableFuture<Page<AssessmentStudentListItem>> findAll(@RequestParam(name = "pageNumber", defaultValue = "0") Integer pageNumber,
                                                       @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                       @RequestParam(name = "sort", defaultValue = "") String sortCriteriaJson,
                                                       @RequestParam(name = "searchCriteriaList", required = false) String searchCriteriaListJson);

}
