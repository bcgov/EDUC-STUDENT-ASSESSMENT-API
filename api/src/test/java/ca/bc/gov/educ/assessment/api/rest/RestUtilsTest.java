package ca.bc.gov.educ.assessment.api.rest;

import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.wildfly.common.Assert.assertFalse;
import static org.wildfly.common.Assert.assertTrue;

class RestUtilsTest {
    @Mock
    private WebClient webClient;

    @Mock
    private WebClient chesWebClient;

    @Mock
    private MessagePublisher messagePublisher;

    @InjectMocks
    private RestUtils restUtils;

    @Mock
    private ApplicationProperties props;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        restUtils = spy(new RestUtils(webClient, props, messagePublisher));
    }

    @Test
    void testPopulateSchoolMap_WhenApiCallSucceeds_ShouldPopulateMaps() {
        // Given
        val school1ID = String.valueOf(UUID.randomUUID());
        val school2ID = String.valueOf(UUID.randomUUID());
        val school3ID = String.valueOf(UUID.randomUUID());
        val school1 = SchoolTombstone.builder()
                .schoolId(school1ID)
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .build();
        val school2 = SchoolTombstone.builder()
                .schoolId(school2ID)
                .displayName("School 2")
                .independentAuthorityId("Authority 1")
                .build();
        val school3 = SchoolTombstone.builder()
                .schoolId(school3ID)
                .displayName("School 3")
                .independentAuthorityId("Authority 2")
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getSchools();

        // When
        restUtils.populateSchoolMap();

        // Then verify the maps are populated
        Map<String, SchoolTombstone> schoolMap = (Map<String, SchoolTombstone>) ReflectionTestUtils.getField(restUtils, "schoolMap");
        assertEquals(3, schoolMap.size());
        assertEquals(school1, schoolMap.get(school1ID));
        assertEquals(school2, schoolMap.get(school2ID));
        assertEquals(school3, schoolMap.get(school3ID));

        Map<String, List<UUID>> independentAuthorityToSchoolIDMap = (Map<String, List<UUID>>) ReflectionTestUtils.getField(restUtils, "independentAuthorityToSchoolIDMap");
        assertEquals(2, independentAuthorityToSchoolIDMap.size());
        assertTrue(independentAuthorityToSchoolIDMap.containsKey("Authority 1"));
        assertTrue(independentAuthorityToSchoolIDMap.containsKey("Authority 2"));
        assertEquals(2, independentAuthorityToSchoolIDMap.get("Authority 1").size());
        assertEquals(1, independentAuthorityToSchoolIDMap.get("Authority 2").size());
    }

    @Test
    void testPopulateDistrictMap_WhenApiCallSucceeds_ShouldPopulateMaps() {
        // Given
        val district1ID = String.valueOf(UUID.randomUUID());
        val district2ID = String.valueOf(UUID.randomUUID());
        val district3ID = String.valueOf(UUID.randomUUID());
        val district1 = District.builder()
                .districtId(district1ID)
                .displayName("District 1")
                .build();
        val district2 = District.builder()
                .districtId(district2ID)
                .displayName("district 2")
                .build();
        val district3 = District.builder()
                .districtId(district3ID)
                .displayName("District 3")
                .build();

        doReturn(List.of(district1, district2, district3)).when(restUtils).getDistricts();

        // When
        restUtils.populateDistrictMap();

        // Then verify the maps are populated
        Map<String, District> districtMap = (Map<String, District>) ReflectionTestUtils.getField(restUtils, "districtMap");
        assertEquals(3, districtMap.size());
        assertEquals(district1, districtMap.get(district1ID));
        assertEquals(district2, districtMap.get(district2ID));
        assertEquals(district3, districtMap.get(district3ID));
    }


    @Test
    void testPopulateSchoolMap_WhenNoIndependentAuthorityId_ShouldPopulateMapsCorrectly() {
        // Given
        val school1ID = String.valueOf(UUID.randomUUID());
        val school2ID = String.valueOf(UUID.randomUUID());
        val school3ID = String.valueOf(UUID.randomUUID());
        val school1 = SchoolTombstone.builder()
                .schoolId(school1ID)
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .build();
        val school2 = SchoolTombstone.builder()
                .schoolId(school2ID)
                .displayName("School 2")
                .build();
        val school3 = SchoolTombstone.builder()
                .schoolId(school3ID)
                .displayName("School 3")
                .independentAuthorityId("Authority 2")
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getSchools();

        // When
        restUtils.populateSchoolMap();

        // Then verify the maps are populated
        Map<String, SchoolTombstone> schoolMap = (Map<String, SchoolTombstone>) ReflectionTestUtils.getField(restUtils, "schoolMap");
        assertEquals(3, schoolMap.size());
        assertEquals(school1, schoolMap.get(school1ID));
        assertEquals(school2, schoolMap.get(school2ID));
        assertEquals(school3, schoolMap.get(school3ID));

        Map<String, List<UUID>> independentAuthorityToSchoolIDMap = (Map<String, List<UUID>>) ReflectionTestUtils.getField(restUtils, "independentAuthorityToSchoolIDMap");
        assertEquals(2, independentAuthorityToSchoolIDMap.size());
        assertTrue(independentAuthorityToSchoolIDMap.containsKey("Authority 1"));
        assertTrue(independentAuthorityToSchoolIDMap.containsKey("Authority 2"));
        assertEquals(1, independentAuthorityToSchoolIDMap.get("Authority 1").size());
        assertEquals(1, independentAuthorityToSchoolIDMap.get("Authority 2").size());
    }


    @Test
    void testPopulateSchoolMap_WhenApiCallFails_ShouldHandleException() {
        // Given
        doThrow(new RuntimeException("API call failed")).when(restUtils).getSchools();

        // When
        assertDoesNotThrow(() -> restUtils.populateSchoolMap()); //checks exception is handled

        // Then Verify that the maps are not populated
        Map<String, SchoolTombstone> schoolMap = (Map<String, SchoolTombstone>) ReflectionTestUtils.getField(restUtils, "schoolMap");
        assertEquals(0, schoolMap.size());

        Map<String, List<UUID>> independentAuthorityToSchoolIDMap = (Map<String, List<UUID>>) ReflectionTestUtils.getField(restUtils, "independentAuthorityToSchoolIDMap");
        assertEquals(0, independentAuthorityToSchoolIDMap.size());
    }

    @Test
    void testGetSchoolIDsByIndependentAuthorityID_WhenAuthorityIDExists_ShouldReturnListOfSchoolIDs() {
        // Given
        String authorityId = "AUTH_ID";
        List<UUID> schoolIds = Collections.singletonList(UUID.randomUUID());
        ReflectionTestUtils.setField(restUtils, "independentAuthorityToSchoolIDMap", Collections.singletonMap(authorityId, schoolIds));

        // Then
        Optional<List<UUID>> result = restUtils.getSchoolIDsByIndependentAuthorityID(authorityId);

        // When
        assertTrue(result.isPresent());
        assertEquals(schoolIds, result.get());
        verify(restUtils, never()).populateSchoolMap();
    }

    @Test
    void testGetSchoolIDsByIndependentAuthorityID_WhenAuthorityIDDoesNotExist_ShouldReturnEmptyOptional() {
        // Given
        String authorityId = "AUTH_ID";
        ReflectionTestUtils.setField(restUtils, "independentAuthorityToSchoolIDMap", Collections.emptyMap());
        doNothing().when(restUtils).populateSchoolMap();

        // When
        Optional<List<UUID>> result = restUtils.getSchoolIDsByIndependentAuthorityID(authorityId);

        // Then
        assertFalse(result.isPresent());
        verify(restUtils).populateSchoolMap();
    }

    @Test
    void testPopulateSchoolMincodeMap_WhenApiCallSucceeds_ShouldPopulateMap() {
        // Given
        val school1Mincode = "97083";
        val school2Mincode = "97084";
        val school3Mincode = "97085";
        val school1 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .mincode(school1Mincode)
                .build();
        val school2 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 2")
                .mincode(school2Mincode)
                .build();
        val school3 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 3")
                .mincode(school3Mincode)
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getSchools();

        // When
        restUtils.populateSchoolMincodeMap();

        // Then verify the maps are populated
        Map<String, SchoolTombstone> schoolMincodeMap = (Map<String, SchoolTombstone>) ReflectionTestUtils.getField(restUtils, "schoolMincodeMap");
        assertEquals(3, schoolMincodeMap.size());
        assertEquals(school1, schoolMincodeMap.get(school1Mincode));
        assertEquals(school2, schoolMincodeMap.get(school2Mincode));
        assertEquals(school3, schoolMincodeMap.get(school3Mincode));

    }

    @Test
    void testGetSchoolFromMincodeMap_WhenApiCallSucceeds_ShouldReturnSchool() {
        // Given
        val school1Mincode = "97083";
        val school2Mincode = "97084";
        val school3Mincode = "97085";
        val school1 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 1")
                .independentAuthorityId("Authority 1")
                .mincode(school1Mincode)
                .build();
        val school2 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 2")
                .mincode(school2Mincode)
                .build();
        val school3 = SchoolTombstone.builder()
                .schoolId(String.valueOf(UUID.randomUUID()))
                .displayName("School 3")
                .mincode(school3Mincode)
                .build();

        doReturn(List.of(school1, school2, school3)).when(restUtils).getSchools();

        // When
        var result = restUtils.getSchoolByMincode(school1Mincode);
        assertEquals(school1, result.get());
    }

    @Test
    void testGetAllMergedStudentsInRange_WhenRequestTimesOut_ShouldThrowEASAPIRuntimeException() {
        UUID correlationID = UUID.randomUUID();
        String createStartDate = "2024-02-01T00:00:00";
        String createEndDate = "2024-09-01T00:00:00";

        when(messagePublisher.requestMessage(anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        StudentAssessmentAPIRuntimeException exception = assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> restUtils.getMergedStudentsForDateRange(correlationID, createStartDate, createEndDate)
        );

        assertEquals(RestUtils.NATS_TIMEOUT + correlationID, exception.getMessage());
    }

    @Test
    void testGetMergedStudentsInRange_WhenExceptionOccurs_ShouldThrowEASAPIRuntimeException() {
        UUID correlationID = UUID.randomUUID();
        String createStartDate = "2024-02-01T00:00:00";
        String createEndDate = "2024-09-01T00:00:00";
        Exception mockException = new Exception("exception");

        when(messagePublisher.requestMessage(anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.failedFuture(mockException));

        assertThrows(
                StudentAssessmentAPIRuntimeException.class,
                () -> restUtils.getMergedStudentsForDateRange(correlationID, createStartDate, createEndDate)
        );
    }
}
