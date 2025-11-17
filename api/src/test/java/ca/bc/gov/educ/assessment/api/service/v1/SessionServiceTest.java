package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.SessionApprovalOrchestrator;
import ca.bc.gov.educ.assessment.api.repository.v1.*;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentApproval;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class SessionServiceTest extends BaseAssessmentAPITest {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private AssessmentSessionRepository assessmentSessionRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private AssessmentStudentRepository assessmentStudentRepository;

    @Autowired
    private AssessmentStudentHistoryRepository assessmentStudentHistoryRepository;

    @Autowired
    private StagedAssessmentStudentRepository stagedAssessmentStudentRepository;

    @Autowired
    private SagaRepository sagaRepository;

    @Autowired
    private SagaEventRepository sagaEventRepository;

    @SpyBean
    private SessionApprovalOrchestrator sessionApprovalOrchestrator;

    private AssessmentSessionEntity testSession;

    @BeforeEach
    public void setUp() {
        Mockito.reset(sessionApprovalOrchestrator);
        testSession = assessmentSessionRepository.save(createMockSessionEntity());
        assessmentRepository.save(createMockAssessmentEntity(testSession, AssessmentTypeCodes.LTF12.getCode()));
    }

    @AfterEach
    public void after() {
        sagaEventRepository.deleteAll();
        sagaRepository.deleteAll();
        stagedAssessmentStudentRepository.deleteAll();
        assessmentStudentRepository.deleteAll();
        assessmentStudentHistoryRepository.deleteAll();
        assessmentRepository.deleteAll();
        assessmentSessionRepository.deleteAll();
    }

    @Test
    @SneakyThrows
    void testApproveAssessment_withThirdSignature_savesAndReturnsQuickly_whileSagaRunsAsync() {
        // Arrange: Set up first two signatures already present
        testSession.setApprovalStudentCertUserID("USER1");
        testSession.setApprovalStudentCertSignDate(LocalDateTime.now());
        testSession.setApprovalAssessmentDesignUserID("USER2");
        testSession.setApprovalAssessmentDesignSignDate(LocalDateTime.now());
        testSession = assessmentSessionRepository.save(testSession);

        // Create a latch to track when the async saga method is called
        CountDownLatch sagaStartedLatch = new CountDownLatch(1);
        AtomicBoolean sagaWasCalledAsync = new AtomicBoolean(false);

        // Spy on the orchestrator to track async execution
        doAnswer(invocation -> {
            // This runs in the async thread
            sagaWasCalledAsync.set(true);
            sagaStartedLatch.countDown();
            // Call the real method
            invocation.callRealMethod();
            return null;
        }).when(sessionApprovalOrchestrator).startXamFileGenerationSaga(any(UUID.class));

        // Prepare third signature approval
        AssessmentApproval thirdApproval = AssessmentApproval.builder()
                .sessionID(testSession.getSessionID().toString())
                .approvalAssessmentAnalysisUserID("USER3")
                .build();

        // Act: Record start time and call approval
        long startTime = System.currentTimeMillis();
        AssessmentSessionEntity result = sessionService.approveAssessment(thirdApproval);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Assert: Verify the method returned quickly
        assertThat(responseTime).isLessThan(1000L);

        // Assert: Verify the approval was saved immediately
        assertNotNull(result);
        assertEquals("USER3", result.getApprovalAssessmentAnalysisUserID());
        assertNotNull(result.getApprovalAssessmentAnalysisSignDate());
        assertNotNull(result.getActiveUntilDate());

        // Assert: Verify all three signatures are present in the returned entity
        assertEquals("USER1", result.getApprovalStudentCertUserID());
        assertEquals("USER2", result.getApprovalAssessmentDesignUserID());
        assertEquals("USER3", result.getApprovalAssessmentAnalysisUserID());

        // Assert: Verify the approval is actually persisted in the database
        AssessmentSessionEntity savedInDb = assessmentSessionRepository.findById(testSession.getSessionID()).orElseThrow();
        assertEquals("USER3", savedInDb.getApprovalAssessmentAnalysisUserID());
        assertNotNull(savedInDb.getApprovalAssessmentAnalysisSignDate());

        // Assert: Verify saga was triggered asynchronously
        verify(sessionApprovalOrchestrator, times(1)).startXamFileGenerationSaga(testSession.getSessionID());

        // Wait for the async saga to start (with timeout)
        boolean sagaStarted = sagaStartedLatch.await(5, TimeUnit.SECONDS);
        assertTrue(sagaStarted, "Saga should have been triggered asynchronously");
        assertTrue(sagaWasCalledAsync.get(), "Saga method should have been called");

        Thread.sleep(500);

        // Assert: Verify saga record was created in the background
        List<AssessmentSagaEntity> sagas = sagaRepository.findAll();
        assertThat(sagas).isNotEmpty();
        AssessmentSagaEntity createdSaga = sagas.stream()
                .filter(s -> s.getSagaName().equals(SagaEnum.GENERATE_XAM_FILES.toString()))
                .filter(s -> s.getAssessmentStudentID().equals(testSession.getSessionID()))
                .findFirst()
                .orElse(null);
        assertNotNull(createdSaga, "Saga should have been created asynchronously");
    }

    @Test
    @SneakyThrows
    void testApproveAssessment_withSecondSignature_savesAndReturnsQuickly_withoutTriggeringSaga() {
        // Arrange: Set up first signature already present
        testSession.setApprovalStudentCertUserID("USER1");
        testSession.setApprovalStudentCertSignDate(LocalDateTime.now());
        testSession = assessmentSessionRepository.save(testSession);

        // Prepare second signature approval
        AssessmentApproval secondApproval = AssessmentApproval.builder()
                .sessionID(testSession.getSessionID().toString())
                .approvalAssessmentDesignUserID("USER2")
                .build();

        // Act
        long startTime = System.currentTimeMillis();
        AssessmentSessionEntity result = sessionService.approveAssessment(secondApproval);
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;

        // Assert: Verify quick response
        assertThat(responseTime).isLessThan(1000L);

        // Assert: Verify the approval was saved
        assertNotNull(result);
        assertEquals("USER2", result.getApprovalAssessmentDesignUserID());

        // Assert: Third signature is not present yet
        assertNull(result.getApprovalAssessmentAnalysisUserID());

        // Assert: Verify saga was NOT triggered (only 2 signatures present)
        verify(sessionApprovalOrchestrator, never()).startXamFileGenerationSaga(any(UUID.class));

        Thread.sleep(500);

        // Assert: No saga should exist
        List<AssessmentSagaEntity> sagas = sagaRepository.findAll();
        assertThat(sagas).isEmpty();
    }

    @Test
    @SneakyThrows
    void testApproveAssessment_sagaFailure_doesNotAffectApprovalSave() {
        // Arrange: Set up first two signatures
        testSession.setApprovalStudentCertUserID("USER1");
        testSession.setApprovalStudentCertSignDate(LocalDateTime.now());
        testSession.setApprovalAssessmentDesignUserID("USER2");
        testSession.setApprovalAssessmentDesignSignDate(LocalDateTime.now());
        testSession = assessmentSessionRepository.save(testSession);

        // Mock the orchestrator to throw an exception
        doThrow(new JsonProcessingException("Simulated saga failure") {})
                .when(sessionApprovalOrchestrator).startXamFileGenerationSaga(any(UUID.class));

        // Prepare third signature approval
        AssessmentApproval thirdApproval = AssessmentApproval.builder()
                .sessionID(testSession.getSessionID().toString())
                .approvalAssessmentAnalysisUserID("USER3")
                .build();

        // Act: Should not throw exception even though saga fails
        AssessmentSessionEntity result = sessionService.approveAssessment(thirdApproval);

        // Assert: Approval should still be saved despite saga failure
        assertNotNull(result);
        assertEquals("USER3", result.getApprovalAssessmentAnalysisUserID());

        // Assert: Verify the approval is persisted in database
        AssessmentSessionEntity savedInDb = assessmentSessionRepository.findById(testSession.getSessionID()).orElseThrow();
        assertEquals("USER3", savedInDb.getApprovalAssessmentAnalysisUserID());
        assertNotNull(savedInDb.getApprovalAssessmentAnalysisSignDate());

        // Assert: Saga was attempted
        verify(sessionApprovalOrchestrator, times(1)).startXamFileGenerationSaga(testSession.getSessionID());
    }
}

