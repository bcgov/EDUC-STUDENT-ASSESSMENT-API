package ca.bc.gov.educ.assessment.api.controller.v1;

import ca.bc.gov.educ.assessment.api.endpoint.v1.AssessmentStudentEndpoint;
import ca.bc.gov.educ.assessment.api.endpoint.v1.AssessmentStudentHistoryEndpoint;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentListItemMapper;
import ca.bc.gov.educ.assessment.api.mappers.v1.AssessmentStudentMapper;
import ca.bc.gov.educ.assessment.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistoryEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistorySearchEntity;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentHistorySearchService;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentSearchService;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentHistoryListItem;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentListItem;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import ca.bc.gov.educ.assessment.api.util.RequestUtil;
import ca.bc.gov.educ.assessment.api.util.ValidationUtil;
import ca.bc.gov.educ.assessment.api.validator.AssessmentStudentValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
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
public class AssessmentStudentHistoryController implements AssessmentStudentHistoryEndpoint {

  private final AssessmentStudentHistorySearchService searchService;
  private static final AssessmentStudentListItemMapper listItemMapper = AssessmentStudentListItemMapper.mapper;

  @Autowired
  public AssessmentStudentHistoryController(AssessmentStudentHistorySearchService searchService) {
      this.searchService = searchService;
  }


    @Override
  public CompletableFuture<Page<AssessmentStudentHistoryListItem>> findAll(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<AssessmentStudentHistorySearchEntity> specs = searchService
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

}
