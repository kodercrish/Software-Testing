import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {

    @Test
    void testEmptyString() {
        Solution sol = new Solution();
        List<String> result = sol.separateParenGroups("");
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void testOnlySpaces() {
        Solution sol = new Solution();
        List<String> result = sol.separateParenGroups("   ");
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void testSingleGroupNoSpaces() {
        Solution sol = new Solution();
        List<String> result = sol.separateParenGroups("()");
        assertEquals(List.of("()"), result);
    }

    @Test
    void testSingleGroupWithSpaces() {
        Solution sol = new Solution();
        List<String> result = sol.separateParenGroups("( )");
        assertEquals(List.of("()"), result);
    }

    @Test
    void testMultipleGroupsSeparatedBySpaces() {
        Solution sol = new Solution();
        List<String> result = sol.separateParenGroups("( ) (( )) (( )( ))");
        assertEquals(List.of("()", "(())", "(()())"), result);
    }

    @Test
    void testMultipleGroupsNoSpaces() {
        Solution sol = new Solution();
        List<String> result = sol.separateParenGroups("()(()())(())");
        assertEquals(List.of("()", "(()())", "(())"), result);
    }

    @Test
    void testNestedGroups() {
        Solution sol = new Solution();
        List<String> result = sol.separateParenGroups("((()))");
        assertEquals(List.of("((()))"), result);
    }

    @Test
    void testMixedSpacesAndNested() {
        Solution sol = new Solution();
        List<String> result = sol.separateParenGroups(" ( ( ( ) ) ) ");
        assertEquals(List.of("((()))"), result);
    }
}
