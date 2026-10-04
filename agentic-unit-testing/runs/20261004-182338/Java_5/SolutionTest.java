import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class SolutionTest {

    @Test
    void testEmptyList() {
        Solution sol = new Solution();
        List<Integer> result = sol.intersperse(List.of(), 4);
        assertEquals(List.of(), result);
    }

    @Test
    void testSingleElementList() {
        Solution sol = new Solution();
        List<Integer> result = sol.intersperse(List.of(5), 3);
        assertEquals(List.of(5), result);
    }

    @Test
    void testTwoElementList() {
        Solution sol = new Solution();
        List<Integer> result = sol.intersperse(List.of(1, 2), 4);
        assertEquals(List.of(1, 4, 2), result);
    }

    @Test
    void testThreeElementList() {
        Solution sol = new Solution();
        List<Integer> result = sol.intersperse(List.of(1, 2, 3), 4);
        assertEquals(List.of(1, 4, 2, 4, 3), result);
    }

    @Test
    void testNullList() {
        Solution sol = new Solution();
        List<Integer> result = sol.intersperse(null, 4);
        assertEquals(List.of(), result);
    }
}
