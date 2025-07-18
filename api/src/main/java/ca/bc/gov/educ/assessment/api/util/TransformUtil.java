package ca.bc.gov.educ.assessment.api.util;

import ca.bc.gov.educ.assessment.api.exception.StudentAssessmentAPIRuntimeException;
import lombok.extern.slf4j.Slf4j;

import java.beans.Expression;
import java.beans.Statement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.springframework.util.StringUtils.capitalize;

/**
 * The type Transform util.
 */

@Slf4j
public class TransformUtil {
  private TransformUtil() {
  }

  /**
   * Uppercase fields t.
   *
   * @param <T>    the type parameter
   * @param claz the claz
   * @return the t
   */
  public static <T> T uppercaseFields(T claz) {
    var clazz = claz.getClass();
    List<Field> fields = new ArrayList<>();
    var superClazz = clazz;
    while (!superClazz.equals(Object.class)) {
      fields.addAll(Arrays.asList(superClazz.getDeclaredFields()));
      superClazz = superClazz.getSuperclass();
    }
    fields.forEach(field -> TransformUtil.transformFieldToUppercase(field, claz));
    return claz;
  }

  /**
   * Is uppercase field boolean.
   *
   * @param clazz     the clazz
   * @param fieldName the field name
   * @return the boolean
   */
  public static boolean isUppercaseField(Class<?> clazz, String fieldName) {
    var superClazz = clazz;
    while (!superClazz.equals(Object.class)) {
      try {
        Field field = superClazz.getDeclaredField(fieldName);
        return field.getAnnotation(UpperCase.class) != null;
      } catch (NoSuchFieldException e) {
        superClazz = superClazz.getSuperclass();
      }
    }
    return false;
  }

  private static <T> void transformFieldToUppercase(Field field, T claz) {
    if (!field.getType().equals(String.class)) {
      return;
    }

    if (field.getAnnotation(UpperCase.class) != null) {
      try {
        var fieldName = capitalize(field.getName());
        var expr = new Expression(claz, "get" + fieldName, new Object[0]);
        var entityFieldValue = (String) expr.getValue();
        if (entityFieldValue != null) {
          var stmt = new Statement(claz, "set" + fieldName, new Object[]{entityFieldValue.toUpperCase()});
          stmt.execute();
        }
      } catch (Exception ex) {
        throw new StudentAssessmentAPIRuntimeException(ex.getMessage());
      }
    }
  }

  public static List<String> splitStringEveryNChars(String text, int n) {
    List<String> result = new ArrayList<>();
    int length = text.length();

    for (int i = 0; i < length; i += n) {
      // Calculate the end index for the substring, ensuring it doesn't exceed string length
      int endIndex = Math.min(length, i + n);
      result.add(text.substring(i, endIndex));
    }
    return result;
  }
}
