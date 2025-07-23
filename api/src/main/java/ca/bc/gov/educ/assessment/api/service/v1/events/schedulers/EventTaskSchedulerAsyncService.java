package ca.bc.gov.educ.assessment.api.service.v1.events.schedulers;

import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.StagedStudentResultRepository;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.service.v1.SessionService;
import ca.bc.gov.educ.assessment.api.service.v1.StudentAssessmentResultService;
import ca.bc.gov.educ.assessment.api.util.SchoolYearUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final AssessmentSessionRepository assessmentSessionRepository;

    @Getter(PRIVATE)
    private final AssessmentStudentService assessmentStudentService;

    @Getter(PRIVATE)
    private final SessionService sessionService;

    private final StagedStudentResultRepository stagedStudentResultRepository;
    private final StudentAssessmentResultService studentAssessmentResultService;

    @Value("${number.students.process.saga}")
    private String numberOfStudentsToProcess;

    @Setter
    private List<String> statusFilters;

    @Transactional
    public void createSessionsForSchoolYear(){
        int schoolYearStart = LocalDate.now().getYear();
        String schoolYear = SchoolYearUtil.generateSchoolYearString(schoolYearStart);
        try {
            if (!this.getAssessmentSessionRepository().upcomingSchoolYearSessionsExist(schoolYear)) {
                log.debug("Creating sessions for {}/{} school year", schoolYearStart, schoolYearStart + 1);
                this.sessionService.createAllAssessmentSessionsForSchoolYear(schoolYearStart);
            }
        } catch (Exception e) {
            log.error("Error creating sessions for {}/{} school year: ", schoolYearStart, schoolYearStart + 1, e);
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

    @Async("processLoadedStudentsTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void findAndPublishLoadedStudentRecordsForProcessing() {
        log.debug("Querying for loaded students to process");
        if (this.getSagaRepository().findByStatusIn(this.getStatusFilters(), 101).size() > 100) { // at max there will be 100 parallel sagas.
            log.debug("Saga count is greater than 100, so not processing student records");
            return;
        }
        final var entities = stagedStudentResultRepository.findTopLoadedStudentForProcessing(numberOfStudentsToProcess);
        log.debug("Found :: {}  records in loaded status", entities.size());
        if (!entities.isEmpty()) {
            studentAssessmentResultService.prepareAndSendSdcStudentsForFurtherProcessing(entities);
        }
    }
}
