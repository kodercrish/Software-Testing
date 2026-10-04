import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    @Test
    void filterBySubstring_emptyList_returnsEmptyList() {
        Solution sol = new Solution();
        List<String> input = List.of();
        List<String> expected = List.of();
        assertEquals(expected, sol.filterBySubstring(input, "a"));
    }

    @Test
    void filterBySubstring_noMatches_returnsEmptyList() {
        Solution sol = new Solution();
        List<String> input = List.of("bcd", "efg", "hij");
        List<String> expected = List.of();
        assertEquals(expected, sol.filterBySubstring(input, "a"));
    }

    @Test
    void filterBySubstring_allMatch_returnsAll() {
        Solution sol = new Solution();
        List<String> input = List.of("abc", "bacd", "array");
        List<String> expected = List.of("abc", "bacd", "array");
        assertEquals(expected, sol.filterBySubstring(input, "a"));
    }

    @Test
    void filterBySubstring_someMatch_returnsFiltered() {
        Solution sol = new Solution();
        List<String> input = List.of("abc", "bacd", "cde", "array");
        List<String> expected = List.of("abc", "bacd", "array");
        assertEquals(expected, sol.filterBySubstring(input, "a"));
    }

    @Test
    void filterBySubstring_nullSubstring_throwsNPE() {
        Solution sol = new Solution();
        List<String> input = List.of("abc", "def");
        assertThrows(NullPointerException.class, () -> sol.filterBySubstring(input, null));
    }
}
