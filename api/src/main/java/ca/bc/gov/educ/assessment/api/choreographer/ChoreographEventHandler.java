package ca.bc.gov.educ.assessment.api.choreographer;

import ca.bc.gov.educ.assessment.api.constants.EventStatus;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentStudentRepository;
import ca.bc.gov.educ.assessment.api.service.v1.AssessmentStudentService;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.StudentForAssessmentUpdate;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class ChoreographEventHandler {
    private final AssessmentEventRepository assessmentEventRepository;
    private final AssessmentStudentRepository assessmentStudentRepository;
    private final AssessmentStudentService assessmentStudentService;

    public ChoreographEventHandler(AssessmentEventRepository assessmentEventRepository, AssessmentStudentRepository assessmentStudentRepository, AssessmentStudentService assessmentStudentService) {
        this.assessmentEventRepository = assessmentEventRepository;
        this.assessmentStudentRepository = assessmentStudentRepository;
        this.assessmentStudentService = assessmentStudentService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEvent(AssessmentEventEntity event) throws JsonProcessingException {
        val eventFromDBOptional = this.assessmentEventRepository.findById(event.getEventId());
        if (eventFromDBOptional.isPresent()) {
            val eventFromDB = eventFromDBOptional.get();
            if (eventFromDB.getEventStatus().equals(EventStatus.DB_COMMITTED.toString())) {
                log.info("Processing event with event ID :: {}", event.getEventId());
                try {
                    switch (event.getEventType()) {
                        case "UPDATE_SCHOOL_OF_RECORD":
                            final StudentForAssessmentUpdate update = JsonUtil.getJsonObjectFromString(StudentForAssessmentUpdate.class, event.getEventPayload());
                            List<AssessmentStudentEntity> students = assessmentStudentRepository.findByStudentID(UUID.fromString(update.getStudentID()));
                            if(!students.isEmpty()) {
                                assessmentStudentService.updateSchoolOfRecord(students, update.getSchoolOfRecordID(), event);
                            } else {
                                log.info("Student does not exist in assessment-api :: {}", event.getEventId());
                            }
                            break;
                        default:
                            log.warn("Silently ignoring event: {}", event);
                            break;
                    }
                    log.info("Event was processed, ID :: {}", event.getEventId());
                } catch (final Exception exception) {
                    log.error("Exception while processing event :: {}", event, exception);
                }
            }
        }
    }
}
