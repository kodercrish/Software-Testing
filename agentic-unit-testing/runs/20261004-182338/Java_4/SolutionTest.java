import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class SolutionTest {

    @Test
    void testNullInput() {
        Solution sol = new Solution();
        assertEquals(0.0, sol.meanAbsoluteDeviation(null), 1e-6);
    }

    @Test
    void testEmptyList() {
        Solution sol = new Solution();
        assertEquals(0.0, sol.meanAbsoluteDeviation(Collections.emptyList()), 1e-6);
    }

    @Test
    void testSingleElement() {
        Solution sol = new Solution();
        assertEquals(0.0, sol.meanAbsoluteDeviation(Collections.singletonList(5.0)), 1e-6);
    }

    @Test
    void testExampleFromJavadoc() {
        Solution sol = new Solution();
        List<Double> numbers = Arrays.asList(1.0, 2.0, 3.0, 4.0);
        assertEquals(1.0, sol.meanAbsoluteDeviation(numbers), 1e-6);
    }

    @Test
    void testAllEqualElements() {
        Solution sol = new Solution();
        List<Double> numbers = Arrays.asList(7.0, 7.0, 7.0, 7.0);
        assertEquals(0.0, sol.meanAbsoluteDeviation(numbers), 1e-6);
    }

    @Test
    void testNegativeNumbers() {
        Solution sol = new Solution();
        List<Double> numbers = Arrays.asList(-2.0, 0.0, 2.0);
        assertEquals(4.0 / 3.0, sol.meanAbsoluteDeviation(numbers), 1e-6);
    }

    @Test
    void testMixedPositiveAndNegative() {
        Solution sol = new Solution();
        List<Double> numbers = Arrays.asList(-1.0, 1.0);
        assertEquals(1.0, sol.meanAbsoluteDeviation(numbers), 1e-6);
    }
}
