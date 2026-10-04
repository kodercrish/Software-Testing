import java.util.*;
import java.lang.*;

class Solution {
    public List<Integer> parseNestedParens(String paren_string) {
        List<Integer> result = new ArrayList<>();
        if (paren_string == null || paren_string.isEmpty()) {
            return result;
        }
        String[] groups = paren_string.split("\\s+");
        for (String group : groups) {
            if (group.isEmpty()) {
                continue;
            }
            int currentDepth = 0;
            int maxDepth = 0;
            for (char c : group.toCharArray()) {
                if (c == '(') {
                    currentDepth++;
                    if (currentDepth > maxDepth) {
                        maxDepth = currentDepth;
                    }
                } else if (c == ')') {
                    currentDepth--;
                }
            }
            result.add(maxDepth);
        }
        return result;
    }
}
