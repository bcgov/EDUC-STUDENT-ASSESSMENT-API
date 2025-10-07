package ca.bc.gov.educ.assessment.api.service.v1.events.schedulers;

import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.helpers.LogHelper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.StagedAssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.TransferStudentProcessingOrchestrator;
import ca.bc.gov.educ.assessment.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.StagedStudentResultRepository;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.service.v1.SessionService;
import ca.bc.gov.educ.assessment.api.service.v1.StudentAssessmentResultService;
import ca.bc.gov.educ.assessment.api.service.v1.TransferStudentOrchestrationService;
import ca.bc.gov.educ.assessment.api.struct.v1.TransferOnApprovalSagaData;
import ca.bc.gov.educ.assessment.api.util.SchoolYearUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
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
    private final TransferStudentOrchestrationService transferStudentOrchestrationService;

    @Value("${number.students.process.saga}")
    private String numberOfStudentsToProcess;
    private final Map<String, Orchestrator> sagaOrchestrators = new HashMap<>();

    @Setter
    private List<String> statusFilters;

    public EventTaskSchedulerAsyncService(final List<Orchestrator> orchestrators, SagaRepository sagaRepository, AssessmentStudentRepository assessmentStudentRepository, AssessmentSessionRepository assessmentSessionRepository, AssessmentStudentService assessmentStudentService, SessionService sessionService, StagedStudentResultRepository stagedStudentResultRepository, StudentAssessmentResultService studentAssessmentResultService, TransferStudentOrchestrationService transferStudentOrchestrationService) {
        this.sagaRepository = sagaRepository;
        this.assessmentStudentRepository = assessmentStudentRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentStudentService = assessmentStudentService;
        this.sessionService = sessionService;
        this.stagedStudentResultRepository = stagedStudentResultRepository;
        this.studentAssessmentResultService = studentAssessmentResultService;
        this.transferStudentOrchestrationService = transferStudentOrchestrationService;
        orchestrators.forEach(orchestrator -> this.sagaOrchestrators.put(orchestrator.getSagaName(), orchestrator));
    }

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
            studentAssessmentResultService.prepareAndSendStudentsForFurtherProcessing(entities);
        } else {
            int batchSize = Integer.parseInt(numberOfStudentsToProcess);
            final var transferStudents = assessmentStudentService.findBatchOfTransferStudentIds(batchSize);
            log.debug("Found :: {} students marked for transfer in this batch", transferStudents.size());
            if(!transferStudents.isEmpty()) {
                transferStudentOrchestrationService.prepareAndSendStudentsForFurtherProcessing(transferStudents);
            }
        }
    }

    @Async("processUncompletedSagasTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void findAndProcessUncompletedSagas() {
        log.debug("Processing uncompleted sagas");
        final var sagas = this.sagaRepository.findTop500ByStatusInOrderByCreateDate(this.getStatusFilters());
        log.debug("Found {} sagas to be retried", sagas.size());
        if (!sagas.isEmpty()) {
            this.processUncompletedSagas(sagas);
        }
    }

    private void processUncompletedSagas(final List<AssessmentSagaEntity> sagas) {
        for (val saga : sagas) {
            if (saga.getUpdateDate().isBefore(LocalDateTime.now().minusMinutes(2))
                    && this.sagaOrchestrators.containsKey(saga.getSagaName())) {
                try {
                    this.setRetryCountAndLog(saga);
                    this.sagaOrchestrators.get(saga.getSagaName()).replaySaga(saga);
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.error("InterruptedException while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, ex);
                } catch (final IOException | TimeoutException e) {
                    log.error("Exception while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, e);
                }
            }
        }
    }

    private void setRetryCountAndLog(final AssessmentSagaEntity saga) {
        Integer retryCount = saga.getRetryCount();
        if (retryCount == null || retryCount == 0) {
            retryCount = 1;
        } else {
            retryCount += 1;
        }
        saga.setRetryCount(retryCount);
        this.sagaRepository.save(saga);
        LogHelper.logSagaRetry(saga);
    }

    public void purgeCompletedResultsFromStaging() {
        this.stagedStudentResultRepository.deleteResultWithStatusCompleted();
        log.debug("Finished purging completed records from Staged Student Result.");
    }
}
