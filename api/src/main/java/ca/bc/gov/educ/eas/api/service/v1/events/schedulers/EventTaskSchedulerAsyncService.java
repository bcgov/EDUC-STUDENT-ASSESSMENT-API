package ca.bc.gov.educ.eas.api.service.v1.events.schedulers;

import ca.bc.gov.educ.eas.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.eas.api.service.v1.AssessmentStudentService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventTaskSchedulerAsyncService {

    @Getter(PRIVATE)
    private final SagaRepository sagaRepository;

    @Getter(PRIVATE)
    private final AssessmentStudentRepository assessmentStudentRepository;

    @Getter(PRIVATE)
    private final AssessmentStudentService assessmentStudentService;

    @Value("${number.students.publish.saga}")
    private String numberOfStudentsToPublish;

    @Setter
    private List<String> statusFilters;

    @Async("processLoadedStudentsTaskExecutor")
    public void findAndPublishLoadedStudentRegistrationsForProcessing() {
        log.debug("Querying for loaded students to publish");
        if (this.getSagaRepository().countAllByStatusIn(this.getStatusFilters()) > 100) { // at max there will be 100 parallel sagas.
            log.debug("Saga count is greater than 100, so not processing student records");
            return;
        }
        final var studentEntities = this.assessmentStudentRepository.findTopLoadedStudentForPublishing(numberOfStudentsToPublish);
        log.debug("Found :: {}  records in loaded status", studentEntities.size());
        if (!studentEntities.isEmpty()) {
            this.assessmentStudentService.prepareAndPublishStudentRegistration(studentEntities);
        }
    }

    public List<String> getStatusFilters() {
        if (this.statusFilters != null && !this.statusFilters.isEmpty()) {
            return this.statusFilters;
        } else {
            final var statuses = new ArrayList<String>();
            statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
            statuses.add(SagaStatusEnum.STARTED.toString());
            return statuses;
        }
    }
}
