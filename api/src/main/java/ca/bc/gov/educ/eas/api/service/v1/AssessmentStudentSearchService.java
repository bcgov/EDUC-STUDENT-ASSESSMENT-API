package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.exception.EasAPIRuntimeException;
import ca.bc.gov.educ.eas.api.filter.AssessmentStudentFilterSpecs;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.eas.api.struct.v1.Search;
import ca.bc.gov.educ.eas.api.util.RequestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * The type Student search service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentStudentSearchService extends BaseSearchService {

  @Getter
  private final AssessmentStudentFilterSpecs studentFilterSpecs;
  private final AssessmentStudentRepository repository;

  @Transactional(propagation = Propagation.SUPPORTS)
  public CompletableFuture<Page<AssessmentStudentEntity>> findAll(Specification<AssessmentStudentEntity> specs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    log.trace("In find all query: {}", specs);
    return CompletableFuture.supplyAsync(() -> {
      Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        log.trace("Running paginated query: {}", specs);
        var results = this.repository.findAll(specs, paging);
        log.trace("Paginated query returned with results: {}", results);
        return results;
      } catch (final Throwable ex) {
        log.error("Failure querying for paginated collections: {}", ex.getMessage());
        throw new CompletionException(ex);
      }
    });
  }

  public Specification<AssessmentStudentEntity> setSpecificationAndSortCriteria(String sortCriteriaJson, String searchCriteriaListJson, ObjectMapper objectMapper, List<Sort.Order> sorts) {
    Specification<AssessmentStudentEntity> studentSpecs = null;
    try {
      RequestUtil.getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        List<Search> searches = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        int i = 0;
        for (var search : searches) {
          studentSpecs = getSpecifications(studentSpecs, i, search, this.getStudentFilterSpecs(), AssessmentStudentEntity.class);
          i++;
        }
      }
    } catch (JsonProcessingException e) {
      throw new EasAPIRuntimeException(e.getMessage());
    }
    return studentSpecs;
  }
}
