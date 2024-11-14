package com.example.slotwordgame;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import com.example.slotwordgame.TurkishUtils;

public class DatabaseHelper {

    private final Context context;
    private final String letterDbPath;
    private final String wordDbPath;

    public DatabaseHelper(Context context) {
        this.context = context;
        this.letterDbPath = context.getDatabasePath("LetterPoint.db").getPath();
        this.wordDbPath = context.getDatabasePath("turkce_kelime_veritabani.db").getPath();

        copyDatabaseIfNotExists("LetterPoint.db", letterDbPath);
        copyDatabaseIfNotExists("turkce_kelime_veritabani.db", wordDbPath);
    }

    private void copyDatabaseIfNotExists(String dbName, String dbPath) {
        File dbFile = new File(dbPath);
        if (!dbFile.exists()) {
            try (InputStream input = context.getAssets().open(dbName);
                 FileOutputStream output = new FileOutputStream(dbFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
                output.flush();
                Log.d("DatabaseHelper", dbName + " veritabanı başarıyla kopyalandı.");
            } catch (IOException e) {
                Log.e("DatabaseHelper", dbName + " veritabanını kopyalama hatası", e);
            }
        } else {
            Log.d("DatabaseHelper", dbName + " veritabanı zaten mevcut.");
        }
    }

    private SQLiteDatabase getLetterDatabase() {
        return SQLiteDatabase.openDatabase(letterDbPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    private SQLiteDatabase getWordDatabase() {
        return SQLiteDatabase.openDatabase(wordDbPath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    public List<String> getAllLetters() {
        List<String> letters = new ArrayList<>();
        try (SQLiteDatabase db = getLetterDatabase();
             Cursor cursor = db.rawQuery("SELECT letter FROM LetterScores", null)) {
            while (cursor.moveToNext()) {
                letters.add(cursor.getString(0));
            }
        } catch (SQLiteException e) {
            Log.e("DatabaseHelper", "Harfleri alırken hata oluştu", e);
        }
        return letters;
    }

    public int getLetterScore(String letter) {
        int score = 3;
        try (SQLiteDatabase db = getLetterDatabase();
             Cursor cursor = db.rawQuery("SELECT score FROM LetterScores WHERE letter = ?", new String[]{letter})) {
            if (cursor.moveToFirst()) {
                score = cursor.getInt(0);
            }
        } catch (SQLiteException e) {
            Log.e("DatabaseHelper", "Harf puanı alırken hata oluştu", e);
        }
        return score;
    }

    public boolean isWordExists(String word) {
        boolean exists = false;
        String normalizedWord = TurkishUtils.turkishToLower(word);

        try (SQLiteDatabase db = getWordDatabase();
             Cursor cursor = db.rawQuery("SELECT 1 FROM kelimeler WHERE LOWER(kelime) = ?", new String[]{normalizedWord})) {
            exists = cursor.moveToFirst();
            Log.d("exist:", exists + " \t" + word + "\t " + normalizedWord);
        } catch (SQLiteException e) {
            Log.e("DatabaseHelper", "Kelime kontrolü yapılırken hata oluştu", e);
        }
        return exists;
    }
}
