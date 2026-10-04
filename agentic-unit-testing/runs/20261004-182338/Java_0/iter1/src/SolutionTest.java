import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class SolutionTest {

    @Test
    void testNullList() {
        Solution sol = new Solution();
        assertFalse(sol.hasCloseElements(null, 0.5));
    }

    @Test
    void testEmptyList() {
        Solution sol = new Solution();
        assertFalse(sol.hasCloseElements(Collections.emptyList(), 0.5));
    }

    @Test
    void testSingleElementList() {
        Solution sol = new Solution();
        assertFalse(sol.hasCloseElements(Collections.singletonList(1.0), 0.5));
    }

    @Test
    void testNoCloseElements() {
        Solution sol = new Solution();
        assertFalse(sol.hasCloseElements(Arrays.asList(1.0, 2.0, 3.0), 0.5));
    }

    @Test
    void testCloseElementsFound() {
        Solution sol = new Solution();
        assertTrue(sol.hasCloseElements(Arrays.asList(1.0, 2.8, 3.0, 4.0, 5.0, 2.0), 0.3));
    }

    @Test
    void testThresholdZeroNoClose() {
        Solution sol = new Solution();
        assertFalse(sol.hasCloseElements(Arrays.asList(1.0, 2.0, 3.0), 0.0));
    }

    @Test
    void testThresholdZeroWithClose() {
        Solution sol = new Solution();
        assertTrue(sol.hasCloseElements(Arrays.asList(1.0, 1.0, 2.0), 0.0));
    }

    @Test
    void testNegativeThreshold() {
        Solution sol = new Solution();
        assertFalse(sol.hasCloseElements(Arrays.asList(1.0, 2.0, 3.0), -0.1));
    }

    @Test
    void testUnsortedInputWithClose() {
        Solution sol = new Solution();
        assertTrue(sol.hasCloseElements(Arrays.asList(3.0, 1.0, 1.1), 0.2));
    }

    @Test
    void testExactThresholdNotLess() {
        Solution sol = new Solution();
        assertFalse(sol.hasCloseElements(Arrays.asList(1.0, 1.5, 2.0), 0.5));
    }
}
