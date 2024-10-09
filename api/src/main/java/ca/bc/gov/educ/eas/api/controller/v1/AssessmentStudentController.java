package ca.bc.gov.educ.eas.api.controller.v1;

import ca.bc.gov.educ.eas.api.endpoint.v1.AssessmentStudentEndpoint;
import ca.bc.gov.educ.eas.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.service.v1.AssessmentStudentSearchService;
import ca.bc.gov.educ.eas.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import ca.bc.gov.educ.eas.api.util.RequestUtil;
import ca.bc.gov.educ.eas.api.util.ValidationUtil;
import ca.bc.gov.educ.eas.api.validator.AssessmentStudentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
  @Autowired
  public AssessmentStudentController(AssessmentStudentService assessmentStudentService, AssessmentStudentValidator validator, AssessmentStudentSearchService searchService) {
    this.studentService = assessmentStudentService;
    this.validator = validator;
    this.searchService = searchService;
  }

  @Override
  public AssessmentStudent readStudent(UUID assessmentStudentID) {
    return mapper.toStructure(studentService.getStudentByID(assessmentStudentID));
  }

  @Override
  public AssessmentStudent updateStudent(AssessmentStudent assessmentStudent, UUID assessmentStudentID) {
    ValidationUtil.validatePayload(() -> validator.validatePayload(assessmentStudent, false));
    RequestUtil.setAuditColumnsForUpdate(assessmentStudent);
    return mapper.toStructure(studentService.updateStudent(mapper.toModel(assessmentStudent)));
  }

  @Override
  public AssessmentStudent createStudent(AssessmentStudent assessmentStudent) {
    ValidationUtil.validatePayload(() -> validator.validatePayload(assessmentStudent, true));
    RequestUtil.setAuditColumnsForCreate(assessmentStudent);
    return mapper.toStructure(studentService.createStudent(mapper.toModel(assessmentStudent)));
  }

  @Override
  public CompletableFuture<Page<AssessmentStudent>> findAll(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
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
            .thenApplyAsync(assessmentStudentEntities -> assessmentStudentEntities.map(mapper::toStructure));
  }
}
