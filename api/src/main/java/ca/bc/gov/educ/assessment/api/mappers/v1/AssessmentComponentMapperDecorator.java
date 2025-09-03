package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentComponentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentComponentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentComponent;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Optional;

@Slf4j
public abstract class AssessmentComponentMapperDecorator implements AssessmentComponentMapper {
    private final AssessmentComponentMapper delegate;
    private static final AssessmentAnswerMapper assessmentAnswerMapper = AssessmentAnswerMapper.mapper;

    private static final ThreadLocal<AssessmentStudentEntity> currentStudentEntity = new ThreadLocal<>();

    protected AssessmentComponentMapperDecorator(AssessmentComponentMapper delegate) {
        this.delegate = delegate;
    }

    public static void setCurrentStudentEntity(AssessmentStudentEntity studentEntity) {
        currentStudentEntity.set(studentEntity);
    }

    public static void clearCurrentStudentEntity() {
        currentStudentEntity.remove();
    }

    @Override
    public AssessmentComponent toStructure(AssessmentComponentEntity entity) {
        final var assessmentComponent = this.delegate.toStructure(entity);

        AssessmentStudentEntity studentEntity = currentStudentEntity.get();
        if (studentEntity != null) {
            populateAssessmentAnswers(assessmentComponent, entity, studentEntity);
        }

        return assessmentComponent;
    }

    private void populateAssessmentAnswers(AssessmentComponent assessmentComponent,
                                         AssessmentComponentEntity componentEntity,
                                         AssessmentStudentEntity studentEntity) {

        Optional<AssessmentStudentComponentEntity> studentComponent = studentEntity.getAssessmentStudentComponentEntities()
            .stream()
            .filter(sc -> sc.getAssessmentComponentID().equals(componentEntity.getAssessmentComponentID()))
            .findFirst();

        if (studentComponent.isPresent()) {
            var answers = studentComponent.get().getAssessmentStudentAnswerEntities()
                .stream()
                .map(assessmentAnswerMapper::toStructure)
                .toList();

            assessmentComponent.setAssessmentAnswers(answers);
            log.debug("Populated {} assessment answers for component {}",
                     answers.size(), componentEntity.getAssessmentComponentID());
        } else {
            assessmentComponent.setAssessmentAnswers(Collections.emptyList());
            log.debug("No student component found for component {}",
                     componentEntity.getAssessmentComponentID());
        }
    }
}
