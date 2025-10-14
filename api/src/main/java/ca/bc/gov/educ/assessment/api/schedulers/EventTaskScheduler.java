package ca.bc.gov.educ.assessment.api.schedulers;

import ca.bc.gov.educ.assessment.api.service.v1.events.schedulers.EventTaskSchedulerAsyncService;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static lombok.AccessLevel.PRIVATE;

@Component
@Slf4j
public class EventTaskScheduler {

    @Getter(PRIVATE)
    private final EventTaskSchedulerAsyncService taskSchedulerAsyncService;

    @Autowired
    public EventTaskScheduler(final EventTaskSchedulerAsyncService taskSchedulerAsyncService) {
        this.taskSchedulerAsyncService = taskSchedulerAsyncService;
    }

    @Scheduled(cron = "${scheduled.jobs.setup.sessions.cron}")
    @SchedulerLock(name = "SETUP_SESSIONS", lockAtLeastFor = "${scheduled.jobs.setup.sessions.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.setup.sessions.cron.lockAtMostFor}")
    public void setupSessionsForUpcomingSchoolYear() {
        LockAssert.assertLocked();
        this.getTaskSchedulerAsyncService().createSessionsForSchoolYear();
    }

    @Scheduled(cron = "${scheduled.jobs.extract.uncompleted.sagas.cron}") // 1 * * * * *
    @SchedulerLock(name = "EXTRACT_UNCOMPLETED_SAGAS",
            lockAtLeastFor = "${scheduled.jobs.extract.uncompleted.sagas.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.extract.uncompleted.sagas.cron.lockAtMostFor}")
    public void findAndProcessPendingSagaEvents() {
        LockAssert.assertLocked();
        log.debug("Started findAndProcessPendingSagaEvents scheduler");
        this.getTaskSchedulerAsyncService().findAndProcessUncompletedSagas();
        log.debug("Scheduler findAndProcessPendingSagaEvents complete");
    }

    @Scheduled(cron = "${scheduled.jobs.publish.loaded.assessment.students.cron}")
    @SchedulerLock(name = "PROCESS_LOADED_STUDENTS", lockAtLeastFor = "${scheduled.jobs.publish.loaded.assessment.students.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.publish.loaded.assessment.students.cron.lockAtMostFor}")
    public void processLoadedStudents() {
        LockAssert.assertLocked();
        log.debug("Started processLoadedStudents scheduler");
        this.getTaskSchedulerAsyncService().findAndPublishLoadedStudentRecordsForProcessing();
        log.debug("Scheduler processLoadedStudents complete");
    }

    @Scheduled(cron = "${scheduled.jobs.purge.completed.results.cron}")
    @SchedulerLock(name = "PURGE_COMPLETED_RESULTS_FROM_STAGING", lockAtLeastFor = "${scheduled.jobs.purge.completed.results.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.purge.completed.results.cron.lockAtMostFor}")
    @Transactional
    public void purgeCompletedResultsFromStaging() {
        LockAssert.assertLocked();
        this.getTaskSchedulerAsyncService().purgeCompletedResultsFromStaging();
    }
}
