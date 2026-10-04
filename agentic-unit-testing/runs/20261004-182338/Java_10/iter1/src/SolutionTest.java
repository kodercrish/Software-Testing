import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

class SolutionTest {

    @Test
    void testMakePalindromeEmptyString() {
        Solution solution = new Solution();
        assertEquals("", solution.makePalindrome(""));
    }

    @Test
    void testMakePalindromeSingleCharacter() {
        Solution solution = new Solution();
        assertEquals("a", solution.makePalindrome("a"));
    }

    @Test
    void testMakePalindromeAlreadyPalindrome() {
        Solution solution = new Solution();
        assertEquals("aba", solution.makePalindrome("aba"));
    }

    @Test
    void testMakePalindromeNoPalindromicSuffixExceptLastChar() {
        Solution solution = new Solution();
        assertEquals("catac", solution.makePalindrome("cat"));
    }

    @Test
    void testMakePalindromePalindromicSuffixOfLengthTwo() {
        Solution solution = new Solution();
        assertEquals("catac", solution.makePalindrome("cata"));
    }

    @Test
    void testMakePalindromePalindromicSuffixInMiddle() {
        Solution solution = new Solution();
        assertEquals("abacaba", solution.makePalindrome("abac"));
    }

    @Test
    void testMakePalindromeNullInput() {
        Solution solution = new Solution();
        assertNull(solution.makePalindrome(null));
    }
}
