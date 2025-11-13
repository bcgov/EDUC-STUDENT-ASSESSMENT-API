package ca.bc.gov.educ.assessment.api.exception;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

@NoArgsConstructor
public class PreconditionRequiredException extends RuntimeException {

  /**
   * Instantiates a new Entity not found exception.
   *
   * @param clazz           the clazz
   * @param searchParamsMap the search params map
   */
  public PreconditionRequiredException(Class clazz, String... searchParamsMap) {
    super(PreconditionRequiredException.generateMessage(clazz.getSimpleName(), toMap(String.class, String.class, searchParamsMap)));
  }

  /**
   * Generate message string.
   *
   * @param entity       the entity
   * @param searchParams the search params
   * @return the string
   */
  private static String generateMessage(String entity, Map<String, String> searchParams) {
    return StringUtils.capitalize(entity) +
        " precondition required " +
        searchParams;
  }

  /**
   * To map map.
   *
   * @param <K>       the type parameter
   * @param <V>       the type parameter
   * @param keyType   the key type
   * @param valueType the value type
   * @param entries   the entries
   * @return the map
   */
  private static <K, V> Map<K, V> toMap(
      Class<K> keyType, Class<V> valueType, Object... entries) {
    if (entries.length % 2 == 1)
      throw new IllegalArgumentException("Invalid entries");
    return IntStream.range(0, entries.length / 2).map(i -> i * 2)
        .collect(HashMap::new,
            (m, i) -> m.put(keyType.cast(entries[i]), valueType.cast(entries[i + 1])),
            Map::putAll);
  }

}
