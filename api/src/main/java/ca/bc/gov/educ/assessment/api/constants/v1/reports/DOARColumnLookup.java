package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import java.util.Arrays;

public enum DOARColumnLookup {

    ENTRY1("BOTH", new String[] {"P", "R", "F", "M"}, new String[] {"NME"}),//task code
    ENTRY2("MUL_CHOICE", new String[] {"I","P", "S", "N", "C"}, new String[] {"NME", "LTF12", "LTP10", "LTP12", "LTE"}), // claim code
    ENTRY3("BOTH", new String[] {"7", "8", "9"}, new String[] {"NME", "LTE", "LTP12", "LTF12", "LTP10"}), // cogn. code
    ENTRY4("OPEN_ENDED", new String[] {"W", "O"}, new String[] {"LTF12", "LTP10", "LTP12", "LTE"}),// claim code
    ENTRY5("MUL_TASK_CHOICE", new String[] {"A", "I", "E"}, new String[] {"LTF12"}), //task code
    ENTRY6("OPEN_ENDED", new String[] {"WRS", "WRD", "WRF", "O1D", "O1F", "O1E", "O2D", "O2F", "O2E", "GO", "WRA", "WRB", "O1", "O2", "O3"}, new String[] {"LTF12", "LTP10", "LTP12", "LTE"}), //concepts code
    ENTRY7("MUL_CHOICE", new String[] {"A", "B"}, new String[] {"LTE", "LTP12", "LTP10"}) //assmt. section
    ;

    private final String componentType;
    private final String[] code;
    private final String[] assessmentTypeCode;
    DOARColumnLookup(String componentType, String[] code, String[] assessmentTypeCode) {
        this.componentType = componentType;
        this.code= code;
        this.assessmentTypeCode = assessmentTypeCode;
    }
    public static DOARColumnLookup getDOARColumn(String componentType, String code, String assessmentTypeCode) {
        return Arrays.stream(values())
                .filter(e -> Arrays.asList(e.code).contains(code) && e.componentType.equalsIgnoreCase(componentType) && Arrays.asList(e.assessmentTypeCode).contains(assessmentTypeCode))
                .findFirst().orElse(null);
    }

}
