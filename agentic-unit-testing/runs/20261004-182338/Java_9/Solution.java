import java.util.*;
import java.lang.*;

class Solution {
    public List<Integer> rollingMax(List<Integer> numbers) {
        List<Integer> result = new ArrayList<>();
        if (numbers == null || numbers.isEmpty()) {
            return result;
        }
        int max = numbers.get(0);
        result.add(max);
        for (int i = 1; i < numbers.size(); i++) {
            int current = numbers.get(i);
            if (current > max) {
                max = current;
            }
            result.add(max);
        }
        return result;
    }
}
