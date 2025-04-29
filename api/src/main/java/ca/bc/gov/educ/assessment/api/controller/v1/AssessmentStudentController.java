package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentStudentStatusCodes;
import ca.bc.gov.educ.assessment.api.endpoint.v1.AssessmentStudentEndpoint;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentListItemMapper;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentSearchService;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentListItem;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import ca.bc.gov.educ.assessment.api.util.RequestUtil;
import ca.bc.gov.educ.assessment.api.util.ValidationUtil;
import ca.bc.gov.educ.assessment.api.validator.AssessmentStudentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
public class AssessmentStudentController implements AssessmentStudentEndpoint {

  private final AssessmentStudentService studentService;
  private final AssessmentStudentValidator validator;
  private final AssessmentStudentSearchService searchService;

  private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;
  private static final AssessmentStudentListItemMapper listItemMapper = AssessmentStudentListItemMapper.mapper;

  @Autowired
  public AssessmentStudentController(AssessmentStudentService assessmentStudentService, AssessmentStudentValidator validator, AssessmentStudentSearchService searchService) {
    this.studentService = assessmentStudentService;
    this.validator = validator;
    this.searchService = searchService;
  }

  @Override
  public AssessmentStudentListItem readStudent(UUID assessmentStudentID) {
    AssessmentStudentListItem assessmentStudentItem =  listItemMapper.toStructure(studentService.getStudentByID(assessmentStudentID));
    assessmentStudentItem.setNumberOfAttempts(studentService.getNumberOfAttempts(assessmentStudentItem.getAssessmentID(), UUID.fromString(assessmentStudentItem.getStudentID())));
    return assessmentStudentItem;
  }

  @Override
  public AssessmentStudent updateStudent(AssessmentStudent assessmentStudent, UUID assessmentStudentID) {
    ValidationUtil.validatePayload(() -> validator.validatePayload(assessmentStudent, false));
    RequestUtil.setAuditColumnsForUpdate(assessmentStudent);
    return studentService.updateStudent(mapper.toModel(assessmentStudent));
  }

    @Override
    public AssessmentStudent createStudent(AssessmentStudent assessmentStudent) {
        ValidationUtil.validatePayload(() -> validator.validatePayload(assessmentStudent, true));
        RequestUtil.setAuditColumnsForCreate(assessmentStudent);
        AssessmentStudentEntity assessmentStudentEntity = mapper.toModel(assessmentStudent);
        assessmentStudentEntity.setAssessmentStudentStatusCode(AssessmentStudentStatusCodes.LOADED.getCode());
        return studentService.createStudent(assessmentStudentEntity);
    }

  @Override
  public CompletableFuture<Page<AssessmentStudentListItem>> findAll(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<AssessmentStudentEntity> specs = searchService
            .setSpecificationAndSortCriteria(
                    sortCriteriaJson,
                    searchCriteriaListJson,
                    JsonUtil.mapper,
                    sorts
            );
    return this.searchService
            .findAll(specs, pageNumber, pageSize, sorts)
            .thenApplyAsync(assessmentStudentEntities -> assessmentStudentEntities.map(listItemMapper::toStructure));
  }

  @Override
  public ResponseEntity<Void> deleteStudent(UUID assessmentStudentID) {
    studentService.deleteStudent(assessmentStudentID);
    return ResponseEntity.noContent().build();
  }
}
