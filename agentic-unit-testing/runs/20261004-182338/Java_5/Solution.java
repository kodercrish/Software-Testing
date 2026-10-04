import java.util.*;
import java.lang.*;

class Solution {
    /**
    Insert a number 'delimeter' between every two consecutive elements of input list `numbers'
    >>> intersperse(List.of(), 4)
    []
    >>> intersperse(Arrays.asList(1, 2, 3), 4)
    [1, 4, 2, 4, 3]
     */
    public List<Integer> intersperse(List<Integer> numbers, int delimiter) {
        List<Integer> result = new ArrayList<>();
        if (numbers == null || numbers.isEmpty()) {
            return result;
        }
        Iterator<Integer> it = numbers.iterator();
        result.add(it.next());
        while (it.hasNext()) {
            result.add(delimiter);
            result.add(it.next());
        }
        return result;
    }
}
