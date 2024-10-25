package ca.bc.gov.educ.eas.api.schedulers;

import ca.bc.gov.educ.eas.api.service.v1.events.schedulers.EventTaskSchedulerAsyncService;
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

    @Scheduled(cron = "${scheduled.jobs.publish.loaded.eas.students.cron}")
    @SchedulerLock(name = "PUBLISH_LOADED_STUDENTS", lockAtLeastFor = "${scheduled.jobs.publish.loaded.eas.students.cron.lockAtLeastFor}", lockAtMostFor = "${scheduled.jobs.publish.loaded.eas.students.cron.lockAtMostFor}")
    public void publishLoadedStudents() {
        LockAssert.assertLocked();
        this.getTaskSchedulerAsyncService().findAndPublishLoadedStudentRegistrationsForProcessing();
    }

}
