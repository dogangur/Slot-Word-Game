package com.example.slotwordgame;
import com.example.slotwordgame.TurkishUtils;

public class TurkishUtils {
    public static String turkishToLower(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace('İ', 'i')
                .replace('I', 'ı')
                .replace('Ğ', 'ğ')
                .replace('Ü', 'ü')
                .replace('Ş', 'ş')
                .replace('Ö', 'ö')
                .replace('Ç', 'ç')
                .replace('A', 'a')
                .replace('E', 'e')
                .replace('O', 'o')
                .replace('U', 'u')
                .toLowerCase();
    }
}
