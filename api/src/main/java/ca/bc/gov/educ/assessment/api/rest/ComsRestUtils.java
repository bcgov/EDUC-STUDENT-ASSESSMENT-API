package ca.bc.gov.educ.assessment.api.rest;

import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.struct.external.coms.v1.Bucket;
import ca.bc.gov.educ.assessment.api.struct.external.coms.v1.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * This class is used for REST calls to COMS (Common Object Management Service)
 */
@Component
@Slf4j
public class ComsRestUtils {
    private static final String OBJECT_PATH = "/object";
    private static final String BUCKET_PATH = "/bucket";
    private static final String OBJECT_SYNC_PATH = "/object/sync";

    private final WebClient comsWebClient;
    private final ApplicationProperties applicationProperties;

    public ComsRestUtils(@Qualifier("comsWebClient") WebClient comsWebClient, ApplicationProperties applicationProperties) {
        this.comsWebClient = comsWebClient;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Builds a complete COMS API URI from the configured endpoint and path
     * @param path the API path (e.g., "/bucket", "/object/")
     * @return complete URI
     */
    private String buildComsUri(String path) {
        return applicationProperties.getComsEndpointUrl() + path;
    }

    /**
     * Create a bucket in COMS
     */
    public Bucket createBucket(Bucket bucket) {
        try {
            log.info("Creating bucket in COMS: {}", bucket.getBucket());
            return comsWebClient.post()
                    .uri(buildComsUri(BUCKET_PATH))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(bucket)
                    .retrieve()
                    .bodyToMono(Bucket.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to create bucket in COMS: {}", bucket.getBucket(), e);
            throw new StudentAssessmentAPIRuntimeException("Failed to create bucket in COMS: " + e.getMessage());
        }
    }

    /**
     * Get all buckets from COMS
     */
    public List<Bucket> getBuckets() {
        try {
            log.debug("Retrieving buckets from COMS");
            return comsWebClient.get()
                    .uri(buildComsUri(BUCKET_PATH))
                    .retrieve()
                    .bodyToFlux(Bucket.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("Failed to retrieve buckets from COMS", e);
            throw new StudentAssessmentAPIRuntimeException("Failed to retrieve buckets from COMS: " + e.getMessage());
        }
    }

    /**
     * Get bucket by name
     */
    public Bucket getBucketByName(String bucketName) {
        try {
            log.debug("Retrieving bucket by name from COMS: {}", bucketName);
            List<Bucket> buckets = getBuckets();
            return buckets.stream()
                    .filter(b -> bucketName.equals(b.getBucket()))
                    .findFirst()
                    .orElseThrow(() -> new StudentAssessmentAPIRuntimeException("Bucket not found: " + bucketName));
        } catch (Exception e) {
            log.error("Failed to retrieve bucket by name from COMS: {}", bucketName, e);
            throw new StudentAssessmentAPIRuntimeException("Failed to retrieve bucket from COMS: " + e.getMessage());
        }
    }

    /**
     * Upload a file to COMS
     *
     * @param content the file content as byte array
     * @param path the path/key in the bucket (e.g., "xam-files-202501/12345678-202501-Results.xam")
     * @return the uploaded object metadata
     */
    public ObjectMetadata uploadObject(byte[] content, String path) {
        try {
            String bucketName = applicationProperties.getS3BucketName();

            // Get the bucket ID from the bucket name
            Bucket bucket = getBucketByName(bucketName);
            String bucketId = bucket.getBucketId();

            log.info("Uploading object to COMS - Bucket: {} (ID: {}), Path: {}, Size: {} bytes",
                    bucketName, bucketId, path, content.length);

            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    // Extract filename from path
                    int lastSlash = path.lastIndexOf('/');
                    return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
                }
            });

            builder.part("bucketId", bucketId);
            builder.part("path", path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "");

            ObjectMetadata response = comsWebClient.post()
                    .uri(buildComsUri(OBJECT_PATH))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToMono(ObjectMetadata.class)
                    .block();

            log.info("Successfully uploaded object to COMS - ID: {}, Path: {}",
                    response.getId(), response.getPath());

            return response;
        } catch (Exception e) {
            log.error("Failed to upload object to COMS - Path: {}", path, e);
            throw new StudentAssessmentAPIRuntimeException("Failed to upload file to COMS: " + e.getMessage());
        }
    }

    /**
     * Delete an object from COMS
     */
    public void deleteObject(String objectId) {
        try {
            log.info("Deleting object from COMS - ID: {}", objectId);
            comsWebClient.delete()
                    .uri(buildComsUri(OBJECT_PATH + "/" + objectId))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            log.info("Successfully deleted object from COMS - ID: {}", objectId);
        } catch (Exception e) {
            log.error("Failed to delete object from COMS - ID: {}", objectId, e);
            throw new StudentAssessmentAPIRuntimeException("Failed to delete object from COMS: " + e.getMessage());
        }
    }

    /**
     * Get object metadata from COMS
     */
    public ObjectMetadata getObjectMetadata(String objectId) {
        try {
            log.debug("Retrieving object metadata from COMS - ID: {}", objectId);
            return comsWebClient.get()
                    .uri(buildComsUri(OBJECT_PATH + "/" + objectId))
                    .retrieve()
                    .bodyToMono(ObjectMetadata.class)
                    .block();
        } catch (Exception e) {
            log.error("Failed to retrieve object metadata from COMS - ID: {}", objectId, e);
            throw new StudentAssessmentAPIRuntimeException("Failed to retrieve object metadata from COMS: " + e.getMessage());
        }
    }

    /**
     * Make an object public by updating its permissions
     * This is needed because COMS makes objects private by default
     *
     * @param objectId the COMS object ID
     */
    public void makeObjectPublic(String objectId) {
        try {
            log.info("Making object public in COMS - ID: {}", objectId);

            // Toggle object to public
            comsWebClient.patch()
                    .uri(buildComsUri(OBJECT_PATH + "/" + objectId + "/public"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"public\": true}")
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Successfully made object public - ID: {}", objectId);
        } catch (Exception e) {
            log.error("Failed to make object public in COMS - ID: {}", objectId, e);
            throw new StudentAssessmentAPIRuntimeException("Failed to make object public in COMS: " + e.getMessage());
        }
    }

    /**
     * Add permissions to an object for specific users/roles
     *
     * @param objectId the COMS object ID
     * @param permissionType the permission type (e.g., "READ", "WRITE", "DELETE")
     * @param userId the user ID to grant permission to (optional)
     */
    public void addObjectPermission(String objectId, String permissionType, String userId) {
        try {
            log.info("Adding {} permission to object {} for user {}", permissionType, objectId, userId);

            String requestBody = userId != null
                ? String.format("{\"permCodes\": [\"%s\"], \"userId\": \"%s\"}", permissionType, userId)
                : String.format("{\"permCodes\": [\"%s\"]}", permissionType);

            comsWebClient.put()
                    .uri(buildComsUri(OBJECT_PATH + "/" + objectId + "/permission"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Successfully added permission to object - ID: {}", objectId);
        } catch (Exception e) {
            log.error("Failed to add permission to object in COMS - ID: {}", objectId, e);
            throw new StudentAssessmentAPIRuntimeException("Failed to add permission to object in COMS: " + e.getMessage());
        }
    }

    /**
     * Sync/register a path in COMS to make S3 objects visible in BCBox
     * Use this if uploading directly to S3 and need to register with COMS
     *
     * @param path the path to sync (e.g., "xam-files-202309/")
     */
    public void syncPath(String path) {
        try {
            log.info("Syncing path in COMS - Path: {}", path);

            comsWebClient.post()
                    .uri(buildComsUri(OBJECT_SYNC_PATH))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(String.format("{\"path\": \"%s\"}", path))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Successfully synced path - Path: {}", path);
        } catch (Exception e) {
            log.error("Failed to sync path in COMS - Path: {}", path, e);
            throw new StudentAssessmentAPIRuntimeException("Failed to sync path in COMS: " + e.getMessage());
        }
    }
}

