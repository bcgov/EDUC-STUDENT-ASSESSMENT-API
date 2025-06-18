package ca.bc.gov.educ.assessment.api.schedulers;

import ca.bc.gov.educ.assessment.api.service.v1.events.schedulers.EventTaskSchedulerAsyncService;
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

}
