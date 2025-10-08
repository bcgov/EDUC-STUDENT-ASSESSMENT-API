package ca.bc.gov.educ.assessment.api.rest;

import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.external.PaginatedResponse;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.struct.external.sdc.v1.Collection;
import ca.bc.gov.educ.assessment.api.struct.external.sdc.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.assessment.api.struct.external.servicesapi.v1.StudentMerge;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
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
        restUtils = spy(new RestUtils(webClient, webClient, props, messagePublisher));
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

    @Test
    void testGetLastFourCollections_WhenApiCallSucceeds_ShouldReturnCollections() throws JsonProcessingException {
        // Given
        Collection collection1 = new Collection();
        collection1.setCollectionID(String.valueOf(UUID.randomUUID()));
        collection1.setSnapshotDate(String.valueOf(LocalDateTime.now()));

        Collection collection2 = new Collection();
        collection2.setCollectionID(String.valueOf(UUID.randomUUID()));
        collection2.setSnapshotDate(String.valueOf(LocalDateTime.now().minusDays(30)));

        PaginatedResponse<Collection> expectedResponse = new PaginatedResponse<>(
                Arrays.asList(collection1, collection2),
                PageRequest.of(0, 4),
                2L
        );

        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(expectedResponse));

        when(props.getSdcApiURL()).thenReturn("http://localhost:8080/api/v1/sdc");

        // When
        PaginatedResponse<Collection> result = restUtils.getLastFourCollections();

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(collection1.getCollectionID(), result.getContent().get(0).getCollectionID());
        assertEquals(collection2.getCollectionID(), result.getContent().get(1).getCollectionID());
        assertEquals(2L, result.getTotalElements());
        assertEquals(0, result.getNumber());
        assertEquals(1, result.getTotalPages());
        verify(webClient).get();
    }

    @Test
    void testGetLastFourCollections_WhenApiCallFails_ShouldReturnNull() throws JsonProcessingException {
        // Given
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenThrow(new RuntimeException("API Error"));

        when(props.getSdcApiURL()).thenReturn("http://localhost:8080/api/v1/sdc");

        // When
        PaginatedResponse<Collection> result = restUtils.getLastFourCollections();

        // Then
        assertNull(result);
        verify(webClient).get();
    }

    @Test
    void testGet1701DataForStudents_WhenApiCallSucceeds_ShouldReturnStudents() {
        // Given
        String collectionID = UUID.randomUUID().toString();
        List<String> assignedStudentIds = Arrays.asList("123456789", "987654321", "555444333");

        SdcSchoolCollectionStudent student1 = new SdcSchoolCollectionStudent();
        student1.setSdcSchoolCollectionStudentID(String.valueOf(UUID.randomUUID()));
        student1.setAssignedStudentId("123456789");

        SdcSchoolCollectionStudent student2 = new SdcSchoolCollectionStudent();
        student2.setSdcSchoolCollectionStudentID(String.valueOf(UUID.randomUUID()));
        student2.setAssignedStudentId("987654321");

        PaginatedResponse<SdcSchoolCollectionStudent> mockResponse = new PaginatedResponse<>(
                Arrays.asList(student1, student2),
                PageRequest.of(0, 1500),
                2L
        );

        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(mockResponse));

        when(props.getSdcApiURL()).thenReturn("http://localhost:8080/api/v1/sdc");

        // When
        List<SdcSchoolCollectionStudent> result = restUtils.get1701DataForStudents(collectionID, assignedStudentIds);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("123456789", result.get(0).getAssignedStudentId());
        assertEquals("987654321", result.get(1).getAssignedStudentId());

        // Verify that the WebClient was called
        verify(webClient, atLeastOnce()).get();
    }

    @Test
    void testGet1701DataForStudents_WithLargeStudentList_ShouldBatchRequests() {
        // Given
        String collectionID = UUID.randomUUID().toString();

        List<String> assignedStudentIds = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            assignedStudentIds.add("PEN" + String.format("%06d", i));
        }

        SdcSchoolCollectionStudent student1 = new SdcSchoolCollectionStudent();
        student1.setSdcSchoolCollectionStudentID(String.valueOf(UUID.randomUUID()));
        student1.setAssignedStudentId("PEN000001");

        PaginatedResponse<SdcSchoolCollectionStudent> mockResponse = new PaginatedResponse<>(
                List.of(student1),
                PageRequest.of(0, 1500),
                1L
        );

        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(mockResponse));

        when(props.getSdcApiURL()).thenReturn("http://localhost:8080/api/v1/sdc");

        // When
        List<SdcSchoolCollectionStudent> result = restUtils.get1701DataForStudents(collectionID, assignedStudentIds);

        // Then
        assertNotNull(result);

        // Verify that WebClient was called multiple times for batching (2000 students / 1500 batch size = 2 batches)
        verify(webClient, atLeast(2)).get();
    }

    @Test
    void testGet1701DataForStudents_WhenBatchFails_ShouldReturnPartialResults() {
        // Given
        String collectionID = UUID.randomUUID().toString();
        List<String> assignedStudentIds = Arrays.asList("123456789", "987654321");

        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenThrow(new RuntimeException("API Error"));

        when(props.getSdcApiURL()).thenReturn("http://localhost:8080/api/v1/sdc");

        // When
        List<SdcSchoolCollectionStudent> result = restUtils.get1701DataForStudents(collectionID, assignedStudentIds);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testFetchStudentsForBatch_WhenApiCallSucceeds_ShouldReturnStudents() throws Exception {
        // Given
        int pageSize = 100;
        List<Map<String, Object>> searchCriteriaList = List.of(
                Map.of("key", "collectionID", "operation", "eq", "value", "test-collection")
        );

        SdcSchoolCollectionStudent student1 = new SdcSchoolCollectionStudent();
        student1.setSdcSchoolCollectionStudentID(String.valueOf(UUID.randomUUID()));
        student1.setAssignedStudentId("123456789");

        SdcSchoolCollectionStudent student2 = new SdcSchoolCollectionStudent();
        student2.setSdcSchoolCollectionStudentID(String.valueOf(UUID.randomUUID()));
        student2.setAssignedStudentId("987654321");

        PaginatedResponse<SdcSchoolCollectionStudent> mockResponse = new PaginatedResponse<>(
                Arrays.asList(student1, student2),
                PageRequest.of(0, pageSize),
                2L
        );

        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenReturn(Mono.just(mockResponse));

        when(props.getSdcApiURL()).thenReturn("http://localhost:8080/api/v1/sdc");

        // When
        List<SdcSchoolCollectionStudent> result = (List<SdcSchoolCollectionStudent>) ReflectionTestUtils.invokeMethod(restUtils, "fetchStudentsForBatch", pageSize, searchCriteriaList);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("123456789", result.get(0).getAssignedStudentId());
        assertEquals("987654321", result.get(1).getAssignedStudentId());
    }

    @Test
    void testFetchStudentsForBatch_WithMultiplePages_ShouldReturnAllStudents() throws Exception {
        // Given
        int pageSize = 1;
        List<Map<String, Object>> searchCriteriaList = Arrays.asList(
                Map.of("key", "collectionID", "operation", "eq", "value", "test-collection")
        );

        SdcSchoolCollectionStudent student1 = new SdcSchoolCollectionStudent();
        student1.setSdcSchoolCollectionStudentID(String.valueOf(UUID.randomUUID()));
        student1.setAssignedStudentId("123456789");

        SdcSchoolCollectionStudent student2 = new SdcSchoolCollectionStudent();
        student2.setSdcSchoolCollectionStudentID(String.valueOf(UUID.randomUUID()));
        student2.setAssignedStudentId("987654321");

        // First page response
        PaginatedResponse<SdcSchoolCollectionStudent> page1Response = new PaginatedResponse<>(
                Arrays.asList(student1),
                PageRequest.of(0, pageSize),
                2L
        );

        // Second page response
        PaginatedResponse<SdcSchoolCollectionStudent> page2Response = new PaginatedResponse<>(
                Arrays.asList(student2),
                PageRequest.of(1, pageSize),
                2L
        );

        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(page1Response))
                .thenReturn(Mono.just(page2Response));

        when(props.getSdcApiURL()).thenReturn("http://localhost:8080/api/v1/sdc");

        // When
        List<SdcSchoolCollectionStudent> result = (List<SdcSchoolCollectionStudent>)
                ReflectionTestUtils.invokeMethod(restUtils, "fetchStudentsForBatch", pageSize, searchCriteriaList);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("123456789", result.get(0).getAssignedStudentId());
        assertEquals("987654321", result.get(1).getAssignedStudentId());

        // Verify that the API was called twice (for both pages)
        verify(responseSpec, times(2)).bodyToMono(any(ParameterizedTypeReference.class));
    }

    @Test
    void testFetchStudentsForBatch_WhenApiCallFails_ShouldReturnEmptyList() throws Exception {
        // Given
        int pageSize = 100;
        List<Map<String, Object>> searchCriteriaList = Arrays.asList(
                Map.of("key", "collectionID", "operation", "eq", "value", "test-collection")
        );

        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.header(anyString(), anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class))).thenThrow(new RuntimeException("API Error"));

        when(props.getSdcApiURL()).thenReturn("http://localhost:8080/api/v1/sdc");

        // When
        List<SdcSchoolCollectionStudent> result = (List<SdcSchoolCollectionStudent>)
                ReflectionTestUtils.invokeMethod(restUtils, "fetchStudentsForBatch", pageSize, searchCriteriaList);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetMergedStudentsForDateRange_WhenValidResponse_ShouldReturnStudentMerges() throws Exception {
        // Given
        UUID correlationID = UUID.randomUUID();
        String createDateStart = "2024-09-08T00:00:00";
        String createDateEnd = "2025-10-08T00:00:00";

        StudentMerge merge1 = StudentMerge.builder()
                .studentMergeID(UUID.randomUUID().toString())
                .studentID(UUID.randomUUID().toString())
                .mergeStudentID(UUID.randomUUID().toString())
                .build();

        StudentMerge merge2 = StudentMerge.builder()
                .studentMergeID(UUID.randomUUID().toString())
                .studentID(UUID.randomUUID().toString())
                .mergeStudentID(UUID.randomUUID().toString())
                .build();

        List<StudentMerge> expectedMerges = Arrays.asList(merge1, merge2);
        ObjectMapper mapper = new ObjectMapper();
        String mergesJson = mapper.writeValueAsString(expectedMerges);

        Event responseEvent = Event.builder()
                .eventOutcome(EventOutcome.MERGE_FOUND)
                .eventPayload(mergesJson)
                .build();

        Message natsMessage = mock(Message.class);
        when(natsMessage.getData()).thenReturn(mapper.writeValueAsBytes(responseEvent));

        when(messagePublisher.requestMessage(anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(natsMessage));

        // When
        List<ca.bc.gov.educ.assessment.api.struct.v1.StudentMerge> result = restUtils.getMergedStudentsForDateRange(correlationID, createDateStart, createDateEnd);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(merge1.getStudentMergeID(), result.get(0).getStudentMergeID());
        assertEquals(merge2.getStudentMergeID(), result.get(1).getStudentMergeID());
    }

    @Test
    void testGetMergedStudentsForDateRange_WhenEmptyList_ShouldReturnEmptyList() throws Exception {
        // Given
        UUID correlationID = UUID.randomUUID();
        String createDateStart = "2024-09-08T00:00:00";
        String createDateEnd = "2025-10-08T00:00:00";

        ObjectMapper mapper = new ObjectMapper();
        String emptyListJson = "[]";

        Event responseEvent = Event.builder()
                .eventOutcome(EventOutcome.MERGE_FOUND)
                .eventPayload(emptyListJson)
                .build();

        Message natsMessage = mock(Message.class);
        when(natsMessage.getData()).thenReturn(mapper.writeValueAsBytes(responseEvent));

        when(messagePublisher.requestMessage(anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(natsMessage));

        // When
        List<ca.bc.gov.educ.assessment.api.struct.v1.StudentMerge> result = restUtils.getMergedStudentsForDateRange(correlationID, createDateStart, createDateEnd);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetStudents_WhenStudentsNotFoundOutcome_ShouldReturnEmptyList() throws Exception {
        // Given
        UUID correlationID = UUID.randomUUID();
        Set<String> studentIDs = new HashSet<>(Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        ));

        ObjectMapper mapper = new ObjectMapper();

        Event responseEvent = Event.builder()
                .eventOutcome(EventOutcome.STUDENTS_NOT_FOUND)
                .eventPayload(mapper.writeValueAsString(studentIDs)) // Payload still contains IDs
                .build();

        Message natsMessage = mock(Message.class);
        when(natsMessage.getData()).thenReturn(mapper.writeValueAsBytes(responseEvent));

        when(messagePublisher.requestMessage(anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(natsMessage));

        // When
        List<Student> result = restUtils.getStudents(correlationID, studentIDs);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetStudents_WhenStudentsFound_ShouldReturnStudents() throws Exception {
        // Given
        UUID correlationID = UUID.randomUUID();
        Set<String> studentIDs = new HashSet<>(Arrays.asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        ));

        Student student1 = Student.builder()
                .studentID(studentIDs.iterator().next())
                .legalFirstName("John")
                .legalLastName("Doe")
                .build();

        Student student2 = Student.builder()
                .studentID(studentIDs.iterator().next())
                .legalFirstName("Jane")
                .legalLastName("Smith")
                .build();

        List<Student> expectedStudents = Arrays.asList(student1, student2);
        ObjectMapper mapper = new ObjectMapper();
        String studentsJson = mapper.writeValueAsString(expectedStudents);

        Event responseEvent = Event.builder()
                .eventOutcome(EventOutcome.STUDENTS_FOUND)
                .eventPayload(studentsJson)
                .build();

        Message natsMessage = mock(Message.class);
        when(natsMessage.getData()).thenReturn(mapper.writeValueAsBytes(responseEvent));

        when(messagePublisher.requestMessage(anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(natsMessage));

        // When
        List<Student> result = restUtils.getStudents(correlationID, studentIDs);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).getLegalFirstName());
        assertEquals("Jane", result.get(1).getLegalFirstName());
    }

    @Test
    void testGetStudents_WhenEmptyResponseData_ShouldReturnEmptyList() {
        // Given
        UUID correlationID = UUID.randomUUID();
        Set<String> studentIDs = new HashSet<>(Collections.singletonList(UUID.randomUUID().toString()));

        Message natsMessage = mock(Message.class);
        when(natsMessage.getData()).thenReturn(new byte[0]); // Empty response data

        when(messagePublisher.requestMessage(anyString(), any(byte[].class)))
                .thenReturn(CompletableFuture.completedFuture(natsMessage));

        // When
        List<Student> result = restUtils.getStudents(correlationID, studentIDs);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
