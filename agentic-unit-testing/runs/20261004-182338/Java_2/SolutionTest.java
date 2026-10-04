import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    @Test
    void testTruncateNumberWithIntegerPartAndDecimalPart() {
        Solution solution = new Solution();
        double result = solution.truncateNumber(3.5);
        assertEquals(0.5, result, 1e-6);
    }

    @Test
    void testTruncateNumberWithNoDecimalPart() {
        Solution solution = new Solution();
        double result = solution.truncateNumber(4.0);
        assertEquals(0.0, result, 1e-6);
    }

    @Test
    void testTruncateNumberWithSmallDecimalPart() {
        Solution solution = new Solution();
        double result = solution.truncateNumber(0.1);
        assertEquals(0.1, result, 1e-6);
    }

    @Test
    void testTruncateNumberWithLargeNumber() {
        Solution solution = new Solution();
        double result = solution.truncateNumber(123.456);
        assertEquals(0.456, result, 1e-6);
    }
}
