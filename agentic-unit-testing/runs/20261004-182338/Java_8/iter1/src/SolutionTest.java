import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    @Test
    void testEmptyList() {
        Solution solution = new Solution();
        List<Integer> result = solution.sumProduct(List.of());
        assertEquals(0, result.get(0));
        assertEquals(1, result.get(1));
    }

    @Test
    void testSingleElement() {
        Solution solution = new Solution();
        List<Integer> result = solution.sumProduct(List.of(5));
        assertEquals(5, result.get(0));
        assertEquals(5, result.get(1));
    }

    @Test
    void testMultipleElements() {
        Solution solution = new Solution();
        List<Integer> result = solution.sumProduct(Arrays.asList(1, 2, 3, 4));
        assertEquals(10, result.get(0));
        assertEquals(24, result.get(1));
    }

    @Test
    void testWithZero() {
        Solution solution = new Solution();
        List<Integer> result = solution.sumProduct(Arrays.asList(0, 1, 2));
        assertEquals(3, result.get(0));
        assertEquals(0, result.get(1));
    }

    @Test
    void testWithNegativeNumbers() {
        Solution solution = new Solution();
        List<Integer> result = solution.sumProduct(Arrays.asList(-1, 2, -3));
        assertEquals(-2, result.get(0));
        assertEquals(6, result.get(1));
    }
}
