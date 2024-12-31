package ca.bc.gov.educ.eas.api.batch.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public enum FileType {
    ASSESSMENT_KEY("tab", ".tab", "assessmentKeyMapper.xml");

    private final String code;
    private final String allowedExtension;
    private final String mapperFileName;

    FileType(String code, String extension, String mapperFileName) {
        this.code = code;
        this.allowedExtension = extension;
        this.mapperFileName = mapperFileName;
    }

    public static Optional<FileType> findByCode(String filetypeCode) {
        return Arrays.stream(values()).filter(filetype -> filetype.code.equalsIgnoreCase(filetypeCode)).findFirst();
    }
}
