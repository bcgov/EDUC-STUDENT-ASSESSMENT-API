package ca.bc.gov.educ.assessment.api.util;

import ca.bc.gov.educ.assessment.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Utility methods for filtering school tombstones.
 */
public class SchoolTombstoneUtil {

  private SchoolTombstoneUtil() {
  }

  /**
   * Returns the public (non-independent, non-offshore) schools belonging to the given district.
   */
  public static List<SchoolTombstone> filterDistrictPublicSchools(List<SchoolTombstone> schoolTombstones, UUID districtID) {
    return schoolTombstones.stream()
            .filter(school -> StringUtils.isNotBlank(school.getSchoolId()))
            .filter(school -> districtID.toString().equalsIgnoreCase(school.getDistrictId()))
            .filter(school -> StringUtils.isBlank(school.getIndependentAuthorityId()))
            .filter(school -> !SchoolCategoryCodes.INDEPENDENTS_AND_OFFSHORE.contains(school.getSchoolCategoryCode()))
            .toList();
  }
}
