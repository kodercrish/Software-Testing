import java.util.*;
import java.lang.*;

class Solution {
    public List<String> separateParenGroups(String paren_string) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int balance = 0;

        for (char c : paren_string.toCharArray()) {
            if (c == ' ') {
                continue;
            }
            if (c == '(') {
                balance++;
                current.append(c);
            } else if (c == ')') {
                balance--;
                current.append(c);
            }
            if (balance == 0 && current.length() > 0) {
                result.add(current.toString());
                current.setLength(0);
            }
        }

        return result;
    }
}
