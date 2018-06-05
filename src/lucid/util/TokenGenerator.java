package lucid.util;

import java.util.Random;

public class TokenGenerator {
    private static String[] chars = {"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
    "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
    "1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};

    public static String getToken() {
        Random r = new Random();
        String token = "";

        for(int i = 0; i < 32; i++) {
            String c = chars[r.nextInt(chars.length)];
            token += c;
        }
        return token;
    }
}
