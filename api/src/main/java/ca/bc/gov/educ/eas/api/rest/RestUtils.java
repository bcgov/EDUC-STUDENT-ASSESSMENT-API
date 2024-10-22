package ca.bc.gov.educ.eas.api.rest;

import ca.bc.gov.educ.eas.api.constants.EventType;
import ca.bc.gov.educ.eas.api.constants.TopicsEnum;
import ca.bc.gov.educ.eas.api.exception.EasAPIRuntimeException;
import ca.bc.gov.educ.eas.api.messaging.MessagePublisher;
import ca.bc.gov.educ.eas.api.properties.ApplicationProperties;
import ca.bc.gov.educ.eas.api.struct.Event;
import ca.bc.gov.educ.eas.api.struct.external.institute.v1.*;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMerge;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class is used for REST calls
 *
 */
@Component
@Slf4j
public class RestUtils {
  public static final String NATS_TIMEOUT = "Either NATS timed out or the response is null , correlationID :: ";
  private static final String CONTENT_TYPE = "Content-Type";
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
  public RestUtils(WebClient webClient, final ApplicationProperties props, final MessagePublisher messagePublisher) {
    this.webClient = webClient;
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

  public Optional<District> getDistrictByDistrictID(final String districtID) {
    if (this.districtMap.isEmpty()) {
      log.info("District map is empty reloading schools");
      this.populateDistrictMap();
    }
    return Optional.ofNullable(this.districtMap.get(districtID));
  }

  public Optional<List<UUID>> getSchoolIDsByIndependentAuthorityID(final String independentAuthorityID) {
    if (this.independentAuthorityToSchoolIDMap.isEmpty()) {
      log.info("The map is empty reloading schools");
      this.populateSchoolMap();
    }
    return Optional.ofNullable(this.independentAuthorityToSchoolIDMap.get(independentAuthorityID));
  }

  @Retryable(retryFor = {Exception.class}, noRetryFor = {EasAPIRuntimeException.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  public List<School> getAllSchoolList(UUID correlationID) {
    try {
      log.info("Calling Institute api to load all schools to memory");
      final TypeReference<List<School>> ref = new TypeReference<>() {
      };
      val event = Event.builder().sagaId(correlationID).eventType(EventType.GET_PAGINATED_SCHOOLS).eventPayload(PAGE_SIZE.concat("=").concat("100000")).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.INSTITUTE_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 60, TimeUnit.SECONDS).get();
      if (null != responseMessage) {
        return objectMapper.readValue(responseMessage.getData(), ref);
      } else {
        throw new EasAPIRuntimeException(NATS_TIMEOUT + correlationID);
      }
    } catch (final Exception ex) {
      Thread.currentThread().interrupt();
      throw new EasAPIRuntimeException(NATS_TIMEOUT + correlationID + ex.getMessage());
    }
  }
  
  public List<StudentMerge> getMergedStudentsForDateRange(UUID correlationID, String createDateStart, String createDateEnd) {
    try {
      final TypeReference<Event> refEventResponse = new TypeReference<>() {};
      final TypeReference<List<StudentMerge>> refMergedStudentResponse = new TypeReference<>() {};
      Object event = Event.builder().sagaId(correlationID).eventType(EventType.GET_MERGED_STUDENT_IDS).eventPayload(CREATE_DATE_START.concat("=").concat(createDateStart).concat("&").concat(CREATE_DATE_END).concat("=").concat(createDateEnd)).build();
      val responseMessage = this.messagePublisher.requestMessage(TopicsEnum.PEN_SERVICES_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event)).completeOnTimeout(null, 120, TimeUnit.SECONDS).get();
      if (responseMessage == null) {
        log.error("Received null response from PEN SERVICES API for correlationID: {}", correlationID);
        throw new EasAPIRuntimeException(NATS_TIMEOUT + correlationID);
      } else {
        val eventResponse = objectMapper.readValue(responseMessage.getData(), refEventResponse);
        return objectMapper.readValue(eventResponse.getEventPayload(), refMergedStudentResponse);
      }

    } catch (final Exception ex) {
      log.error("Error occurred calling PEN SERVICES API service :: " + ex.getMessage());
      Thread.currentThread().interrupt();
      throw new EasAPIRuntimeException(ex.getMessage());
    }
  }
}
