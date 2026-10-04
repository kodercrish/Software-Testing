import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class SolutionTest {

    @Test
    void testNeverBelowZero() {
        Solution sol = new Solution();
        List<Integer> operations = Arrays.asList(1, 2, 3);
        assertFalse(sol.belowZero(operations));
    }

    @Test
    void testBelowZeroAtThirdOperation() {
        Solution sol = new Solution();
        List<Integer> operations = Arrays.asList(1, 2, -4, 5);
        assertTrue(sol.belowZero(operations));
    }

    @Test
    void testBelowZeroAtFirstOperation() {
        Solution sol = new Solution();
        List<Integer> operations = Arrays.asList(-1);
        assertTrue(sol.belowZero(operations));
    }

    @Test
    void testEmptyOperations() {
        Solution sol = new Solution();
        List<Integer> operations = Collections.emptyList();
        assertFalse(sol.belowZero(operations));
    }

    @Test
    void testBalanceExactlyZeroThenNegative() {
        Solution sol = new Solution();
        List<Integer> operations = Arrays.asList(5, -5, -1);
        assertTrue(sol.belowZero(operations));
    }

    @Test
    void testMultipleDepositsAndWithdrawalsNeverBelowZero() {
        Solution sol = new Solution();
        List<Integer> operations = Arrays.asList(10, -5, 3, -1);
        assertFalse(sol.belowZero(operations));
    }
}
