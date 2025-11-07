package ca.bc.gov.educ.assessment.api.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TransformUtilTest {

    @Test
    void testSplitStringEveryNChars_withValidString() {
        String input = "12345678";
        List<String> result = TransformUtil.splitStringEveryNChars(input, 4);

        assertEquals(2, result.size());
        assertEquals("1234", result.get(0));
        assertEquals("5678", result.get(1));
    }

    @Test
    void testSplitStringEveryNChars_withStringNotDivisibleByN() {
        String input = "123456789";
        List<String> result = TransformUtil.splitStringEveryNChars(input, 4);

        assertEquals(3, result.size());
        assertEquals("1234", result.get(0));
        assertEquals("5678", result.get(1));
        assertEquals("9", result.get(2));
    }

    @Test
    void testSplitStringEveryNChars_withNullString() {
        List<String> result = TransformUtil.splitStringEveryNChars(null, 4);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSplitStringEveryNChars_withEmptyString() {
        List<String> result = TransformUtil.splitStringEveryNChars("", 4);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSplitStringEveryNChars_withSingleChar() {
        String input = "A";
        List<String> result = TransformUtil.splitStringEveryNChars(input, 4);

        assertEquals(1, result.size());
        assertEquals("A", result.getFirst());
    }

    @Test
    void testSplitStringEveryNChars_withNEqualsOne() {
        String input = "ABC";
        List<String> result = TransformUtil.splitStringEveryNChars(input, 1);

        assertEquals(3, result.size());
        assertEquals("A", result.get(0));
        assertEquals("B", result.get(1));
        assertEquals("C", result.get(2));
    }
}
