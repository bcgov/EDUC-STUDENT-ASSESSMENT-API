package ca.bc.gov.educ.assessment.api.constants.v1.reports;

import java.util.Arrays;

public enum DOARColumnLookup {

    ENTRY1("BOTH", "P"),
    ENTRY2("BOTH", "R"),
    ENTRY3("BOTH", "F"),
    ENTRY4("BOTH", "M"),
    ENTRY5("MUL_CHOICE", "I"),
    ENTRY6("MUL_CHOICE", "P"),
    ENTRY7("MUL_CHOICE", "S"),
    ENTRY8("MUL_CHOICE", "N"),
    ENTRY9("BOTH", "7"),
    ENTRY10("BOTH", "7"),
    ENTRY11("BOTH", "9")
    ;

    private final String componentType;
    private final String code;
    DOARColumnLookup(String componentType, String code) {
        this.componentType = componentType;
        this.code= code;
    }
    public static DOARColumnLookup getDOARColumn(String componentType, String code) {
        return Arrays.stream(values())
                .filter(e -> e.code.equalsIgnoreCase(code) && e.componentType.equalsIgnoreCase(componentType))
                .findFirst().orElse(null);
    }

}
