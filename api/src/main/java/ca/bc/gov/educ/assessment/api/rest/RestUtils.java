package ca.bc.gov.educ.assessment.api.rest;

import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.external.PaginatedResponse;
import ca.bc.gov.educ.assessment.api.struct.external.grad.v1.GradStudentRecord;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.*;
import ca.bc.gov.educ.assessment.api.struct.external.sdc.v1.Collection;
import ca.bc.gov.educ.assessment.api.struct.external.sdc.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.assessment.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.assessment.api.struct.v1.CHESEmail;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentMerge;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import ca.bc.gov.educ.assessment.api.util.SearchCriteriaBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * This class is used for REST calls
 *
 */
@Component
@Slf4j
public class RestUtils {
  public static final String NATS_TIMEOUT = "Either NATS timed out or the response is null , correlationID :: ";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String EXCEPTION = "exception";
  public static final String NO_RESPONSE_RECEIVED_WITHIN_TIMEOUT_FOR_CORRELATION_ID = "No response received within timeout for correlation ID ";
  private final Map<String, IndependentAuthority> authorityMap = new ConcurrentHashMap<>();
  private final Map<String, SchoolTombstone> schoolMap = new ConcurrentHashMap<>();
  private final Map<String, SchoolTombstone> schoolMincodeMap = new ConcurrentHashMap<>();
  private final Map<String, District> districtMap = new ConcurrentHashMap<>();
  private final Map<String, FacilityTypeCode> facilityTypeCodesMap = new ConcurrentHashMap<>();
  private final Map<String, SchoolCategoryCode> schoolCategoryCodesMap = new ConcurrentHashMap<>();
  public static final String PAGE_SIZE = "pageSize";
  public static final String CREATE_DATE_START = "createDateStart";
  public static final String CREATE_DATE_END = "createDateEnd";
  private final WebClient webClient;
  private final WebClient chesWebClient;
  private final MessagePublisher messagePublisher;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ReadWriteLock facilityTypesLock = new ReentrantReadWriteLock();
  private final ReadWriteLock schoolCategoriesLock = new ReentrantReadWriteLock();
  private final ReadWriteLock authorityLock = new ReentrantReadWriteLock();
  private final ReadWriteLock schoolLock = new ReentrantReadWriteLock();
  private final ReadWriteLock districtLock = new ReentrantReadWriteLock();
  @Getter
  private final ApplicationProperties props;

  @Value("${initialization.background.enabled}")
  private Boolean isBackgroundInitializationEnabled;

  private final Map<String, List<UUID>> independentAuthorityToSchoolIDMap = new ConcurrentHashMap<>();

  @Autowired
  public RestUtils(WebClient webClient, @Qualifier("chesWebClient")WebClient chesWebClient, final ApplicationProperties props, final MessagePublisher messagePublisher) {
    this.webClient = webClient;
    this.chesWebClient = chesWebClient;
    this.props = props;
    this.messagePublisher = messagePublisher;
  }

  @PostConstruct
  public void init() {
    if (this.isBackgroundInitializationEnabled != null && this.isBackgroundInitializationEnabled) {
      ApplicationProperties.bgTask.execute(this::initialize);
    }
  }

  private void initialize() {
    this.populateSchoolCategoryCodesMap();
    this.populateFacilityTypeCodesMap();
    this.populateSchoolMap();
    this.populateSchoolMincodeMap();
    this.populateDistrictMap();
    this.populateAuthorityMap();
  }

  @Scheduled(cron = "${schedule.jobs.load.school.cron}")
  public void scheduled() {
    this.init();
  }

  public void populateAuthorityMap() {
    val writeLock = this.authorityLock.writeLock();
    try {
      writeLock.lock();
      for (val authority : this.getAuthorities()) {
        this.authorityMap.put(authority.getIndependentAuthorityId(), authority);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache authorities ", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} authorities to memory", this.authorityMap.values().size());
  }

  public void populateSchoolCategoryCodesMap() {
    val writeLock = this.schoolCategoriesLock.writeLock();
    try {
      writeLock.lock();
      for (val categoryCode : this.getSchoolCategoryCodes()) {
        this.schoolCategoryCodesMap.put(categoryCode.getSchoolCategoryCode(), categoryCode);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache school categories ", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} school categories to memory", this.schoolCategoryCodesMap.values().size());
  }

  public void populateFacilityTypeCodesMap() {
    val writeLock = this.facilityTypesLock.writeLock();
    try {
      writeLock.lock();
      for (val categoryCode : this.getFacilityTypeCodes()) {
        this.facilityTypeCodesMap.put(categoryCode.getFacilityTypeCode(), categoryCode);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache facility types ", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} facility types to memory", this.facilityTypeCodesMap.values().size());
  }

  public void populateSchoolMap() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      for (val school : this.getSchools()) {
        this.schoolMap.put(school.getSchoolId(), school);
        if (StringUtils.isNotBlank(school.getIndependentAuthorityId())) {
          this.independentAuthorityToSchoolIDMap.computeIfAbsent(school.getIndependentAuthorityId(), k -> new ArrayList<>()).add(UUID.fromString(school.getSchoolId()));
        }
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache school ", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} schools to memory", this.schoolMap.values().size());
  }

  public void populateSchoolMincodeMap() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      for (val school : this.getSchools()) {
        this.schoolMincodeMap.put(school.getMincode(), school);
        if (StringUtils.isNotBlank(school.getIndependentAuthorityId())) {
          this.independentAuthorityToSchoolIDMap.computeIfAbsent(school.getIndependentAuthorityId(), k -> new ArrayList<>()).add(UUID.fromString(school.getSchoolId()));
        }
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache school mincodes ", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} school mincodes to memory", this.schoolMincodeMap.values().size());
  }

  public List<SchoolTombstone> getSchools() {
    log.info("Calling Institute api to load schools to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/school")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(SchoolTombstone.class)
            .collectList()
            .block();
  }

  public List<IndependentAuthority> getAuthorities() {
    log.info("Calling Institute api to load authority to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/authority")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(IndependentAuthority.class)
            .collectList()
            .block();
  }

  public List<SchoolCategoryCode> getSchoolCategoryCodes() {
    log.info("Calling Institute api to load school categories to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/category-codes")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(SchoolCategoryCode.class)
            .collectList()
            .block();
  }

  public List<FacilityTypeCode> getFacilityTypeCodes() {
    log.info("Calling Institute api to load facility type codes to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/facility-codes")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(FacilityTypeCode.class)
            .collectList()
            .block();
  }

  public School getSchoolDetails(UUID schoolID) {
    log.debug("Retrieving school by ID: {}", schoolID);
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/school/" + schoolID)
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(School.class)
            .blockFirst();
  }

  public void populateDistrictMap() {
    val writeLock = this.districtLock.writeLock();
    try {
      writeLock.lock();
      for (val district : this.getDistricts()) {
        this.districtMap.put(district.getDistrictId(), district);
      }
    } catch (Exception ex) {
      log.error("Unable to load map cache district ", ex);
    } finally {
      writeLock.unlock();
    }
    log.info("Loaded  {} districts to memory", this.districtMap.values().size());
  }

  public List<District> getDistricts() {
    log.info("Calling Institute api to load districts to memory");
    return this.webClient.get()
            .uri(this.props.getInstituteApiURL() + "/district")
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .retrieve()
            .bodyToFlux(District.class)
            .collectList()
            .block();
  }

  public Optional<SchoolCategoryCode> getSchoolCategoryCode(final String schoolCategoryCode) {
    if (this.schoolCategoryCodesMap.isEmpty()) {
      log.info("School categories map is empty reloading them");
      this.populateSchoolCategoryCodesMap();
    }
    return Optional.ofNullable(this.schoolCategoryCodesMap.get(schoolCategoryCode));
  }

  public Optional<FacilityTypeCode> getFacilityTypeCode(final String facilityTypeCode) {
    if (this.facilityTypeCodesMap.isEmpty()) {
      log.info("Facility types map is empty reloading them");
      this.populateFacilityTypeCodesMap();
    }
    return Optional.ofNullable(this.facilityTypeCodesMap.get(facilityTypeCode));
  }

  public Optional<SchoolTombstone> getSchoolBySchoolID(final String schoolID) {
    if (this.schoolMap.isEmpty()) {
      log.info("School map is empty reloading schools");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.schoolMap.get(schoolID));
  }

  public Optional<IndependentAuthority> getAuthorityByAuthorityID(final String authorityID) {
    if (this.authorityMap.isEmpty()) {
      log.info("Authority map is empty reloading authorities");
      this.populateAuthorityMap();
    }
    return Optional.ofNullable(this.authorityMap.get(authorityID));
  }

  public Optional<SchoolTombstone> getSchoolByMincode(final String mincode) {
    if (this.schoolMincodeMap.isEmpty()) {
      log.info("School mincode map is empty reloading schools");
      this.populateSchoolMincodeMap();
    }
    return Optional.ofNullable(this.schoolMincodeMap.get(mincode));
  }

  public List<SchoolTombstone> getAllSchoolTombstones() {
    if (this.schoolMincodeMap.isEmpty()) {
      log.info("School mincode map is empty reloading schools");
      this.populateSchoolMincodeMap();
    }
    return new ArrayList<>(this.schoolMap.values());
  }

  public Optional<District> getDistrictByDistrictID(final String districtID) {
    if (this.districtMap.isEmpty()) {
      log.info("District map is empty reloading schools");
      this.populateDistrictMap();
    }
    return Optional.ofNullable(this.districtMap.get(districtID));
  }


  public Optional<District> getYukonDistrict() {
    if (this.districtMap.isEmpty()) {
      log.info("District map is empty reloading schools");
      this.populateDistrictMap();
    }
    
    return this.districtMap.values().stream().filter(dist -> dist.getDistrictRegionCode().equalsIgnoreCase("YUKON")).findFirst();
  }

  public Optional<List<UUID>> getSchoolIDsByIndependentAuthorityID(final String independentAuthorityID) {
    if (this.independentAuthorityToSchoolIDMap.isEmpty()) {
      log.info("The map is empty reloading school");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.independentAuthorityToSchoolIDMap.get(independentAuthorityID));
  }
  
  public List<StudentMerge> getMergedStudentsForDateRange(UUID correlationID, String createDateStart, String createDateEnd) {
    try {
      final TypeReference<Event> refEventResponse = new TypeReference<>() {};
      final TypeReference<List<StudentMerge>> refMergedStudentResponse = new TypeReference<>() {};
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.GET_MERGES_IN_DATE_RANGE).eventPayload(CREATE_DATE_START.concat("=").concat(createDateStart).concat("&").concat(CREATE_DATE_END).concat("=").concat(createDateEnd)).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.PEN_SERVICES_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 120, TimeUnit.SECONDS).get();
      if (responseMessage == null) {
        log.error("Received null response from PEN SERVICES API for correlationID: {}", correlationID);
        throw new StudentAssessmentAPIRuntimeException(NATS_TIMEOUT + correlationID);
      } else {
        val eventResponse = objectMapper.readValue(responseMessage.getData(), refEventResponse);
        return objectMapper.readValue(eventResponse.getEventPayload(), refMergedStudentResponse);
      }

    } catch (final Exception ex) {
      log.error("Error occurred calling PEN SERVICES API service :: " + ex.getMessage());
      Thread.currentThread().interrupt();
      throw new StudentAssessmentAPIRuntimeException(ex.getMessage());
    }
  }

  @Retryable(retryFor = {Exception.class}, noRetryFor = {SagaRuntimeException.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Optional<Student> getStudentByPEN(UUID correlationID, String assignedPEN) {
    try {
      final TypeReference<Event> refEvent = new TypeReference<>() {};
      final TypeReference<Student> refPenMatchResult = new TypeReference<>() {
      };
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.GET_STUDENT).eventPayload(assignedPEN).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.STUDENT_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 120, TimeUnit.SECONDS).get();
      if (responseMessage != null) {
        byte[] data = responseMessage.getData();
        if (data == null || data.length == 0) {
          log.debug("Empty response data for getStudentByPEN; treating as student not found for PEN: {}", assignedPEN);
          return Optional.empty();
        }
        Event responseEvent = objectMapper.readValue(responseMessage.getData(), refEvent);

        if (EventOutcome.STUDENT_NOT_FOUND.equals(responseEvent.getEventOutcome())) {
          log.info("Student not found for PEN: {}", assignedPEN);
          return Optional.empty();
        }

        return Optional.ofNullable(objectMapper.readValue(responseMessage.getData(), refPenMatchResult));
      } else {
        throw new StudentAssessmentAPIRuntimeException(NATS_TIMEOUT + correlationID);
      }

    } catch (final Exception ex) {
      log.error("Error occurred calling GET STUDENT service :: " + ex.getMessage());
      Thread.currentThread().interrupt();
      throw new StudentAssessmentAPIRuntimeException(NATS_TIMEOUT + correlationID + ex.getMessage());
    }
  }

  public void sendEmail(final String fromEmail, final List<String> toEmail, final String body, final String subject) {
    this.sendEmail(this.getChesEmail(fromEmail, toEmail, body, subject));
  }
  
  public void sendEmail(final CHESEmail chesEmail) {
    this.chesWebClient
            .post()
            .uri(this.props.getChesEndpointURL())
            .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(Mono.just(chesEmail), CHESEmail.class)
            .retrieve()
            .bodyToMono(String.class)
            .doOnError(error -> this.logError(error, chesEmail))
            .doOnSuccess(success -> this.onSendEmailSuccess(success, chesEmail))
            .block();
  }

  private void logError(final Throwable throwable, final CHESEmail chesEmailEntity) {
    log.error("Error from CHES API call :: {} ", chesEmailEntity, throwable);
  }

  private void onSendEmailSuccess(final String s, final CHESEmail chesEmailEntity) {
    log.info("Email sent success :: {} :: {}", chesEmailEntity, s);
  }

  public CHESEmail getChesEmail(final String fromEmail, final List<String> toEmail, final String body, final String subject) {
    final CHESEmail chesEmail = new CHESEmail();
    chesEmail.setBody(body);
    chesEmail.setBodyType("html");
    chesEmail.setDelayTS(0);
    chesEmail.setEncoding("utf-8");
    chesEmail.setFrom(fromEmail);
    chesEmail.setPriority("normal");
    chesEmail.setSubject(subject);
    chesEmail.setTag("tag");
    chesEmail.getTo().addAll(toEmail);
    return chesEmail;
  }
  
  public List<Student> getStudents(UUID correlationID, Set<String> studentIDs) {
    try {
      final TypeReference<Event> refEventResponse = new TypeReference<>() {};
      final TypeReference<List<Student>> refStudentResponse = new TypeReference<>() {};
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.GET_STUDENTS).eventPayload(objectMapper.writeValueAsString(studentIDs)).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.STUDENT_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 120, TimeUnit.SECONDS).get();
      if (responseMessage == null) {
        log.error("Received null response from GET STUDENTS for correlationID: {}", correlationID);
        throw new StudentAssessmentAPIRuntimeException(NATS_TIMEOUT + correlationID);
      } else {
        byte[] data = responseMessage.getData();
        if (data == null || data.length == 0) {
          log.debug("Empty response data for getStudents; returning empty list for correlationID: {}", correlationID);
          return new ArrayList<>();
        }
        val eventResponse = objectMapper.readValue(data, refEventResponse);
        
        if (EventOutcome.STUDENTS_NOT_FOUND.equals(eventResponse.getEventOutcome())) {
          log.debug("Students not found for correlationID: {}; returning empty list", correlationID);
          return new ArrayList<>();
        }
        return objectMapper.readValue(eventResponse.getEventPayload(), refStudentResponse);
      }

    } catch (final Exception ex) {
      log.error("Error occurred calling GET STUDENTS service :: " + ex.getMessage());
      Thread.currentThread().interrupt();
      throw new StudentAssessmentAPIRuntimeException(ex.getMessage());
    }
  }

  @Retryable(retryFor = {Exception.class}, noRetryFor = {SagaRuntimeException.class, EntityNotFoundException.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public Optional<GradStudentRecord> getGradStudentRecordByStudentID(UUID correlationID, UUID studentID) {
    try {
      final TypeReference<GradStudentRecord> refGradStudentRecordResult = new TypeReference<>() {
      };
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.GET_GRAD_STUDENT_RECORD).eventPayload(studentID.toString()).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.GRAD_STUDENT_API_FETCH_GRAD_STUDENT_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 120, TimeUnit.SECONDS).get();
      if (responseMessage != null) {
        String responseData = new String(responseMessage.getData(), StandardCharsets.UTF_8);

        Map<String, Object> response = objectMapper.readValue(responseData, new TypeReference<>() {});

        log.debug("getGradStudentRecordByStudentID response{}", response.toString());

        if ("not found".equals(response.get(EXCEPTION))) {
          log.debug("Grad student was not found while fetching GradStudentRecord for Student ID {}", studentID);
          return Optional.empty();
        } else if ("error".equals(response.get(EXCEPTION))) {
          log.error("An exception error occurred while fetching GradStudentRecord for Student ID {}", studentID);
          throw new StudentAssessmentAPIRuntimeException("Error occurred while processing the request for correlation ID " + correlationID);
        }

        log.debug("Success fetching GradStudentRecord for Student ID {}", studentID);
        return Optional.of(objectMapper.readValue(responseData, refGradStudentRecordResult));
      } else {
        throw new StudentAssessmentAPIRuntimeException(NO_RESPONSE_RECEIVED_WITHIN_TIMEOUT_FOR_CORRELATION_ID + correlationID);
      }

    } catch (EntityNotFoundException ex) {
      log.debug("Entity Not Found occurred calling GET GRAD STUDENT RECORD service :: {}", ex.getMessage());
      throw ex;
    } catch (final Exception ex) {
      log.error("Error occurred calling GET GRAD STUDENT RECORD service :: {}", ex.getMessage());
      Thread.currentThread().interrupt();
      throw new StudentAssessmentAPIRuntimeException(NATS_TIMEOUT + correlationID);
    }
  }

  public void setStudentFlagsInGRAD(UUID correlationID, List<UUID> studentIDs) {
    try {
      final TypeReference<Event> refSetStudentFlagReturn = new TypeReference<>() {
      };
      String commaSeparated = studentIDs.stream()
              .map(UUID::toString)
              .collect(Collectors.joining(","));
      
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.SET_STUDENT_FLAGS).eventPayload(commaSeparated).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.GRAD_STUDENT_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 220, TimeUnit.SECONDS).get();
      if (responseMessage != null) {
        String responseData = new String(responseMessage.getData(), StandardCharsets.UTF_8);

        Map<String, Object> response = objectMapper.readValue(responseData, new TypeReference<>() {});

        log.debug("setStudentFlagsInGRAD response{}", response.toString());

        var eventReturn = Optional.of(objectMapper.readValue(responseData, refSetStudentFlagReturn));

        if (!eventReturn.get().getEventOutcome().toString().equalsIgnoreCase("STUDENT_FLAGS_UPDATED")) {
          log.error("An exception error occurred while writing student flags in GRAD, please inspect GRAD Student API logs");
          throw new StudentAssessmentAPIRuntimeException("An exception error occurred while writing student flags in GRAD, please inspect GRAD Student API logs");
        }

        log.info("Success setting flags for students in GRAD for IDs {}", commaSeparated);
      } else {
        throw new StudentAssessmentAPIRuntimeException(NO_RESPONSE_RECEIVED_WITHIN_TIMEOUT_FOR_CORRELATION_ID + correlationID);
      }

    } catch (EntityNotFoundException ex) {
      log.debug("Entity Not Found occurred calling GET GRAD STUDENT RECORD service :: {}", ex.getMessage());
      throw ex;
    } catch (final Exception ex) {
      log.error("Error occurred calling GET GRAD STUDENT RECORD service :: {}", ex.getMessage());
      Thread.currentThread().interrupt();
      throw new StudentAssessmentAPIRuntimeException(NATS_TIMEOUT + correlationID);
    }
  }

  public void adoptStudentInGRAD(UUID correlationID, UUID studentID) {
    try {
      final TypeReference<Event> refAdoptStudentReturn = new TypeReference<>() {
      };

      Object event = Event.builder().sagaId(correlationID).eventType(EventType.ADOPT_STUDENT).eventPayload(String.valueOf(studentID)).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.GRAD_STUDENT_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 220, TimeUnit.SECONDS).get();
      if (responseMessage != null) {
        String responseData = new String(responseMessage.getData(), StandardCharsets.UTF_8);

        Map<String, Object> response = objectMapper.readValue(responseData, new TypeReference<>() {});

        log.debug("adoptStudentInGRAD response{}", response.toString());

        var eventReturn = Optional.of(objectMapper.readValue(responseData, refAdoptStudentReturn));

        if (!eventReturn.get().getEventOutcome().toString().equalsIgnoreCase("GRAD_STUDENT_ADOPTED")) {
          log.error("An exception error occurred while adopting student in GRAD, please inspect GRAD Student API logs");
          throw new StudentAssessmentAPIRuntimeException("An exception error occurred while  adopting student in GRAD, please inspect GRAD Student API logs");
        }

        log.info("Success  adopting student in GRAD for studentID {}", studentID);
      } else {
        throw new StudentAssessmentAPIRuntimeException(NO_RESPONSE_RECEIVED_WITHIN_TIMEOUT_FOR_CORRELATION_ID + correlationID);
      }

    } catch (EntityNotFoundException ex) {
      log.debug("Entity Not Found occurred calling GET GRAD STUDENT RECORD service :: {}", ex.getMessage());
      throw ex;
    } catch (final Exception ex) {
      log.error("Error occurred calling GET GRAD STUDENT RECORD service :: {}", ex.getMessage());
      Thread.currentThread().interrupt();
      throw new StudentAssessmentAPIRuntimeException(NATS_TIMEOUT + correlationID);
    }
  }

    public PaginatedResponse<Collection> getLastFourCollections(AssessmentSessionEntity assessmentSession) throws JsonProcessingException {
        int pageNumber = 0;
        int pageSize = 4;

        try {
            Map<String, String> sortMap = new HashMap<>();
            sortMap.put("snapshotDate", "DESC");

            String sortJson = objectMapper.writeValueAsString(sortMap);
            String encodedSortJson = URLEncoder.encode(sortJson, StandardCharsets.UTF_8);

            String activeFromDate = String.valueOf(assessmentSession.getActiveFromDate());

            Map<String, Object> criteria = new HashMap<>();
            criteria.put("key", "openDate");
            criteria.put("operation", "lt");
            criteria.put("value", activeFromDate);
            criteria.put("valueType", "DATE_TIME");

            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("condition", "AND");
            wrapper.put("searchCriteriaList", List.of(criteria));

            List<Map<String, Object>> searchCriteriaList = new ArrayList<>();
            searchCriteriaList.add(wrapper);

            String searchJson = objectMapper.writeValueAsString(searchCriteriaList);
            String encodedSearchJson = URLEncoder.encode(searchJson, StandardCharsets.UTF_8);

            String fullUrl = this.props.getSdcApiURL()
                    + "/collection/paginated"
                    + "?pageNumber=" + pageNumber
                    + "&pageSize=" + pageSize
                    + "&sort=" + encodedSortJson
                    + "&searchCriteriaList=" + encodedSearchJson;

            log.debug("Calling SDC API to get last 4 collections: {}", fullUrl);

            return webClient.get()
                    .uri(fullUrl)
                    .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<PaginatedResponse<Collection>>() {
                    })
                    .block();
        } catch (Exception ex) {
            log.error("Error fetching collections on page {}", pageNumber, ex);
            return null;
        }
    }

    public List<SdcSchoolCollectionStudent> get1701DataForStudents(String collectionID, List<String> assignedStudentIds) {
        int maxPensPerBatch = 1500;
        int pageSize = 1500;

        ExecutorService executor = Executors.newFixedThreadPool(8); // Adjust thread pool size as needed
        List<CompletableFuture<List<SdcSchoolCollectionStudent>>> futures = new ArrayList<>();

        for (int i = 0; i < assignedStudentIds.size(); i += maxPensPerBatch) {
            int start = i;
            int end = Math.min(i + maxPensPerBatch, assignedStudentIds.size());
            List<String> assigendStudentIdsSubList = new ArrayList<>(assignedStudentIds.subList(start, end));

            CompletableFuture<List<SdcSchoolCollectionStudent>> future = CompletableFuture.supplyAsync(() -> {
                try {
                    List<Map<String, Object>> searchCriteriaList = SearchCriteriaBuilder.getSDCStudentsByCollectionIdAndAssignedPENs(collectionID, assigendStudentIdsSubList);
                    return fetchStudentsForBatch(pageSize, searchCriteriaList);
                } catch (Exception e) {
                    log.error("Batch fetch failed", e);
                    return Collections.emptyList();
                }
            }, executor);

            futures.add(future);
        }

        List<SdcSchoolCollectionStudent> allStudents = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        executor.shutdown();
        return allStudents;
    }

    private List<SdcSchoolCollectionStudent> fetchStudentsForBatch(int pageSize, List<Map<String, Object>> searchCriteriaList) throws JsonProcessingException {
        List<SdcSchoolCollectionStudent> students = new ArrayList<>();
        String searchJson = objectMapper.writeValueAsString(searchCriteriaList);
        String encodedSearchJson = URLEncoder.encode(searchJson, StandardCharsets.UTF_8);

        int pageNumber = 0;
        boolean hasNextPage = true;

        while (hasNextPage) {
            try {
                String fullUrl = this.props.getSdcApiURL()
                        + "/sdcSchoolCollectionStudent/paginated-shallow"
                        + "?pageNumber=" + pageNumber
                        + "&pageSize=" + pageSize
                        + "&sort=" // optional: add sort json or keep empty
                        + "&searchCriteriaList=" + encodedSearchJson;

                PaginatedResponse<SdcSchoolCollectionStudent> response = webClient.get()
                        .uri(fullUrl)
                        .header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<PaginatedResponse<SdcSchoolCollectionStudent>>() {
                        })
                        .block();

                if (response != null && response.getContent() != null) {
                    students.addAll(response.getContent());
                    hasNextPage = response.getNumber() < response.getTotalPages() - 1;
                    pageNumber++;
                } else {
                    hasNextPage = false;
                }
            } catch (Exception ex) {
                log.error("Error fetching 1701 data for page {} of batch starting at PEN {}", pageNumber, ex);
                break;
            }
        }

        return students;
    }
}
