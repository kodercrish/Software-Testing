import java.util.*;
import java.lang.*;

class Solution {
    /**
    Input are two strings a and b consisting only of 1s and 0s.
    Perform binary XOR on these inputs and return result also as a string.
    >>> stringXor("010", "110")
    "100"
     */
    public String stringXor(String a, String b) {
        StringBuilder result = new StringBuilder();
        int length = Math.max(a.length(), b.length());
        for (int i = 0; i < length; i++) {
            char bitA = i < a.length() ? a.charAt(a.length() - 1 - i) : '0';
            char bitB = i < b.length() ? b.charAt(b.length() - 1 - i) : '0';
            char xorBit = (bitA == bitB) ? '0' : '1';
            result.insert(0, xorBit);
        }
        return result.toString();
    }
}
