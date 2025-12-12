package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.endpoint.v1.AssessmentStudentEndpoint;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentListItemMapper;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentShowItemMapper;
import ca.bc.gov.educ.assessment.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentSearchService;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.struct.v1.*;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import ca.bc.gov.educ.assessment.api.util.RequestUtil;
import ca.bc.gov.educ.assessment.api.util.ValidationUtil;
import ca.bc.gov.educ.assessment.api.validator.AssessmentStudentValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
public class AssessmentStudentController implements AssessmentStudentEndpoint {

  private final AssessmentStudentService studentService;
  private final AssessmentStudentValidator validator;
  private final AssessmentStudentSearchService searchService;
  private final Publisher publisher;

  private static final AssessmentStudentMapper mapper = AssessmentStudentMapper.mapper;
  private static final AssessmentStudentListItemMapper listItemMapper = AssessmentStudentListItemMapper.mapper;

    @Autowired
  public AssessmentStudentController(AssessmentStudentService assessmentStudentService, AssessmentStudentValidator validator, AssessmentStudentSearchService searchService, Publisher publisher) {
    this.studentService = assessmentStudentService;
    this.validator = validator;
    this.searchService = searchService;
    this.publisher = publisher;
  }

  @Override
  public AssessmentStudentListItem readStudent(UUID assessmentStudentID) {
    AssessmentStudentListItem assessmentStudentItem =  listItemMapper.toStructure(studentService.getStudentByID(assessmentStudentID));
    assessmentStudentItem.setNumberOfAttempts(studentService.getNumberOfAttempts(assessmentStudentItem.getAssessmentID(), UUID.fromString(assessmentStudentItem.getStudentID())));
    return assessmentStudentItem;
  }

    @Override
    public AssessmentStudentShowItem readStudentResults(UUID assessmentStudentID, UUID assessmentID) {
        return studentService.getStudentWithAssessmentDetailsById(assessmentStudentID, assessmentID);
    }

  @Override
  public AssessmentStudentListItem updateStudent(AssessmentStudent assessmentStudent, UUID assessmentStudentID, boolean allowRuleOverride, String source) {
    ValidationUtil.validatePayload(() -> validator.validatePayload(assessmentStudent, false));
    RequestUtil.setAuditColumnsForUpdate(assessmentStudent);
    var pair = studentService.updateStudent(mapper.toModel(assessmentStudent), allowRuleOverride, source);
    if(pair.getRight() != null) {
      publisher.dispatchChoreographyEvent(pair.getRight());  
    }
    return pair.getLeft();
  }

  @Override
  public AssessmentStudentListItem createStudent(AssessmentStudent assessmentStudent, boolean allowRuleOverride, String source) {
    log.debug("Allow override set to: {}", allowRuleOverride);
    ValidationUtil.validatePayload(() -> validator.validatePayload(assessmentStudent, true));
    RequestUtil.setAuditColumnsForCreate(assessmentStudent);
    AssessmentStudentEntity assessmentStudentEntity = mapper.toModel(assessmentStudent);
    var pair = studentService.createStudent(assessmentStudentEntity, allowRuleOverride, source);
    if(pair.getRight() != null) {
      publisher.dispatchChoreographyEvent(pair.getRight());
    }
    return pair.getLeft();
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
  public ResponseEntity<Void> deleteStudents(List<UUID> assessmentStudentIDs, boolean allowRuleOverride) {
    List<AssessmentEventEntity> events = studentService.deleteStudents(assessmentStudentIDs, allowRuleOverride);
    events.forEach(publisher::dispatchChoreographyEvent);
    return ResponseEntity.noContent().build();
  }

  @Override
  public List<AssessmentStudentValidationIssue> transferStudents(AssessmentStudentMoveRequest assessmentStudentTransfer) {
    log.debug("Transfer student assessments request received: sourceStudentID={}, targetStudentID={}, count={}, updateUser={}",
        assessmentStudentTransfer.getSourceStudentID(), assessmentStudentTransfer.getTargetStudentID(), assessmentStudentTransfer.getStudentAssessmentIDsToMove().size(), assessmentStudentTransfer.getUpdateUser());
    
    var pair = studentService.transferStudentAssessments(assessmentStudentTransfer);
    if(pair.getRight() != null) {
      pair.getRight().forEach(publisher::dispatchChoreographyEvent);
    }
    return pair.getLeft();
  }

  @Override
  public List<AssessmentStudentListItem> mergeStudents(AssessmentStudentMoveRequest assessmentStudentMerge) {
    log.debug("Merge student assessments request received: sourceStudentID={}, targetStudentID={}, count={}, updateUser={}",
        assessmentStudentMerge.getSourceStudentID(), assessmentStudentMerge.getTargetStudentID(), assessmentStudentMerge.getStudentAssessmentIDsToMove().size(), assessmentStudentMerge.getUpdateUser());

    var pair = studentService.mergeStudentAssessments(assessmentStudentMerge);
    if(pair.getRight() != null) {
      publisher.dispatchChoreographyEvent(pair.getRight());
    }
    return pair.getLeft();
  }
}
