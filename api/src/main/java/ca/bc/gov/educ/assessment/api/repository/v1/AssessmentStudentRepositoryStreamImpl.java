package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

@Repository
public class AssessmentStudentRepositoryStreamImpl implements AssessmentStudentRepositoryStream {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * @param spec The specification to filter results (can be null for all results)
     * @return Stream of AssessmentStudentEntity
     */
    @Override
    @Transactional(readOnly = true)
    public Stream<AssessmentStudentEntity> streamAll(Specification<AssessmentStudentEntity> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AssessmentStudentEntity> cq = cb.createQuery(AssessmentStudentEntity.class);
        Root<AssessmentStudentEntity> root = cq.from(AssessmentStudentEntity.class);

        if (spec != null) {
            cq.where(spec.toPredicate(root, cq, cb));
        }

        TypedQuery<AssessmentStudentEntity> query = entityManager.createQuery(cq);

        query.setHint("org.hibernate.fetchSize", 5000);
        query.setHint("org.hibernate.readOnly", true);

        return query.getResultStream();
    }
}

