package ca.bc.gov.educ.assessment.api.struct.v1.reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SimpleHeadcountResultsTable {
  List<String> headers;
  List<Map<String, String>> rows;
}
