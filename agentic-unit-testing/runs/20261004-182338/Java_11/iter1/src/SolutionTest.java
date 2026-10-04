import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class SolutionTest {
    @Test
    void testEqualLengthBothZeros() {
        Solution sol = new Solution();
        assertEquals("000", sol.stringXor("000", "000"));
    }

    @Test
    void testEqualLengthBothOnes() {
        Solution sol = new Solution();
        assertEquals("000", sol.stringXor("111", "111"));
    }

    @Test
    void testEqualLengthMixed() {
        Solution sol = new Solution();
        assertEquals("110", sol.stringXor("101", "011"));
    }

    @Test
    void testFirstLonger() {
        Solution sol = new Solution();
        assertEquals("1010", sol.stringXor("1100", "101"));
    }

    @Test
    void testSecondLonger() {
        Solution sol = new Solution();
        assertEquals("1010", sol.stringXor("101", "1100"));
    }

    @Test
    void testOneEmpty() {
        Solution sol = new Solution();
        assertEquals("101", sol.stringXor("", "101"));
    }

    @Test
    void testBothEmpty() {
        Solution sol = new Solution();
        assertEquals("", sol.stringXor("", ""));
    }

    @Test
    void testSingleCharDifferent() {
        Solution sol = new Solution();
        assertEquals("1", sol.stringXor("0", "1"));
    }

    @Test
    void testSingleCharSame() {
        Solution sol = new Solution();
        assertEquals("0", sol.stringXor("1", "1"));
    }
}
