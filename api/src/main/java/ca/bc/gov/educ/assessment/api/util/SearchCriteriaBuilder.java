package ca.bc.gov.educ.assessment.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SearchCriteriaBuilder {
    // Method to create the search criteria for SEPTEMBER collection
    public static List<Map<String, Object>> septemberCollections() {
        List<Map<String, Object>> searchCriteriaList = new ArrayList<>();

        Map<String, Object> collectionTypeCodeCriteria = new HashMap<>();
        collectionTypeCodeCriteria.put("key", "collectionTypeCode");
        collectionTypeCodeCriteria.put("value", "SEPTEMBER");
        collectionTypeCodeCriteria.put("operation", "eq");
        collectionTypeCodeCriteria.put("valueType", "STRING");

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("condition", "AND");
        wrapper.put("searchCriteriaList", List.of(collectionTypeCodeCriteria));
        searchCriteriaList.add(wrapper);

        return searchCriteriaList;
    }

    public static List<Map<String, Object>> getSDCStudentsByCollectionIdAndAssignedPENs(String collectionID, List<String> assignedPens) {
        List<Map<String, Object>> searchCriteriaList = new ArrayList<>();

        // First block: collection ID
        Map<String, Object> collectionIdCriteria = new HashMap<>();
        collectionIdCriteria.put("key", "sdcSchoolCollection.collectionEntity.collectionID");
        collectionIdCriteria.put("value", collectionID);
        collectionIdCriteria.put("operation", "eq");
        collectionIdCriteria.put("valueType", "UUID");

        // Second block: assigned PENs (IN)
        Map<String, Object> pensCriteria = new HashMap<>();
        pensCriteria.put("key", "assignedPen");
        pensCriteria.put("operation", "in");
        pensCriteria.put("value", String.join(",", assignedPens));
        pensCriteria.put("valueType", "STRING");
        pensCriteria.put("condition", "AND"); // inside the group condition

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("condition", "AND"); // outer group condition
        wrapper.put("searchCriteriaList", List.of(collectionIdCriteria, pensCriteria));
        searchCriteriaList.add(wrapper);

        return searchCriteriaList;
    }
}
