import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    @Test
    void testNullInput() {
        Solution solution = new Solution();
        List<Integer> result = solution.parseNestedParens(null);
        assertEquals(new ArrayList<>(), result);
    }

    @Test
    void testEmptyStringInput() {
        Solution solution = new Solution();
        List<Integer> result = solution.parseNestedParens("");
        assertEquals(new ArrayList<>(), result);
    }

    @Test
    void testSingleGroupWithNoNesting() {
        Solution solution = new Solution();
        List<Integer> result = solution.parseNestedParens("()");
        assertEquals(List.of(1), result);
    }

    @Test
    void testSingleGroupWithNesting() {
        Solution solution = new Solution();
        List<Integer> result = solution.parseNestedParens("(())");
        assertEquals(List.of(2), result);
    }

    @Test
    void testMultipleGroupsWithSpaces() {
        Solution solution = new Solution();
        List<Integer> result = solution.parseNestedParens("(()()) ((())) () ((())()())");
        assertEquals(List.of(2, 3, 1, 3), result);
    }

    @Test
    void testGroupsWithExtraWhitespace() {
        Solution solution = new Solution();
        List<Integer> result = solution.parseNestedParens("  (())   ()  ((()))  ");
        assertEquals(List.of(2, 1, 3), result);
    }

    @Test
    void testGroupWithEmptyStringAfterSplit() {
        Solution solution = new Solution();
        List<Integer> result = solution.parseNestedParens("()  ()");
        assertEquals(List.of(1, 1), result);
    }

    @Test
    void testGroupWithOnlyClosingParenthesis() {
        Solution solution = new Solution();
        List<Integer> result = solution.parseNestedParens(")");
        assertEquals(List.of(0), result);
    }

}
