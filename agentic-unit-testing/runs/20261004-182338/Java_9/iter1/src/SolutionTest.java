import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    @Test
    void testNullInput() {
        Solution solution = new Solution();
        List<Integer> result = solution.rollingMax(null);
        assertEquals(new ArrayList<>(), result);
    }

    @Test
    void testEmptyList() {
        Solution solution = new Solution();
        List<Integer> result = solution.rollingMax(Collections.emptyList());
        assertEquals(new ArrayList<>(), result);
    }

    @Test
    void testSingleElement() {
        Solution solution = new Solution();
        List<Integer> input = List.of(5);
        List<Integer> expected = List.of(5);
        assertEquals(expected, solution.rollingMax(input));
    }

    @Test
    void testIncreasingSequence() {
        Solution solution = new Solution();
        List<Integer> input = List.of(1, 2, 3, 4, 5);
        List<Integer> expected = List.of(1, 2, 3, 4, 5);
        assertEquals(expected, solution.rollingMax(input));
    }

    @Test
    void testDecreasingSequence() {
        Solution solution = new Solution();
        List<Integer> input = List.of(5, 4, 3, 2, 1);
        List<Integer> expected = List.of(5, 5, 5, 5, 5);
        assertEquals(expected, solution.rollingMax(input));
    }

    @Test
    void testMixedSequence() {
        Solution solution = new Solution();
        List<Integer> input = List.of(1, 2, 3, 2, 3, 4, 2);
        List<Integer> expected = List.of(1, 2, 3, 3, 3, 4, 4);
        assertEquals(expected, solution.rollingMax(input));
    }

    @Test
    void testWithNegativeNumbers() {
        Solution solution = new Solution();
        List<Integer> input = List.of(-3, -1, -2, 0, -5);
        List<Integer> expected = List.of(-3, -1, -1, 0, 0);
        assertEquals(expected, solution.rollingMax(input));
    }

    @Test
    void testWithDuplicates() {
        Solution solution = new Solution();
        List<Integer> input = List.of(4, 4, 4, 4);
        List<Integer> expected = List.of(4, 4, 4, 4);
        assertEquals(expected, solution.rollingMax(input));
    }
}
