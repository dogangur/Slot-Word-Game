package com.example.slotwordgame;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import com.example.slotwordgame.TurkishUtils;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private GridLayout letterGrid, selectedLettersGrid;
    private TextView turnCount, currentScore, balance, turnRight, targetScore, selectedWordScoreTextView, timerTextView;
    private Button confirmWordButton, replayButton, loadTokenButton;
    private ProgressBar progressBar;
    private Map<TextView, String> selectedLetterViews = new HashMap<>();
    private final int totalTurns = 10;
    private final int targetScoreValue = 100;
    private int currentTurnCount = 1, userScore = 0, userBalance = 0, remainingTurns = totalTurns, currentTurnScore = 0;
    private CountDownTimer countDownTimer;
    private DatabaseHelper databaseHelper;
    private StringBuilder selectedWord = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);

        setupUI();
        setupGame();
    }

    private void setupUI() {
        letterGrid = findViewById(R.id.letterGrid);
        selectedLettersGrid = findViewById(R.id.selectedLettersGrid);
        turnCount = findViewById(R.id.turnCount);
        currentScore = findViewById(R.id.currentScore);
        balance = findViewById(R.id.balance);
        turnRight = findViewById(R.id.turnRight);
        targetScore = findViewById(R.id.targetScore);
        selectedWordScoreTextView = findViewById(R.id.selectedWordScoreTextView);
        confirmWordButton = findViewById(R.id.confirmWordButton);
        replayButton = findViewById(R.id.replayButton);
        loadTokenButton = findViewById(R.id.loadTokenButton);
        timerTextView = findViewById(R.id.timerTextView);
        progressBar = findViewById(R.id.progressBar);

        replayButton.setEnabled(false);
        confirmWordButton.setOnClickListener(v -> checkAndConfirmWord());
        replayButton.setOnClickListener(v -> startNewGame());
    }

    private void startCountdown() {
        replayButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(30);

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished / 1000);
                timerTextView.setText(String.valueOf(progress));
                progressBar.setProgress(progress);
            }

            @Override
            public void onFinish() {
                timerTextView.setText("");
                progressBar.setVisibility(View.GONE);
                replayButton.setEnabled(true);
            }
        }.start();
    }

    private void startNewGame() {
        currentTurnCount = 1;
        userScore = 0;
        userBalance = 0;
        remainingTurns = totalTurns;
        currentTurnScore = 0;
        selectedWord.setLength(0);
        confirmWordButton.setEnabled(true);
        replayButton.setEnabled(false);
        setupGame();
        updateUI();
    }

    private void setupGame() {
        createLetterGrid();
        resetSelections();
    }

    private void createLetterGrid() {
        letterGrid.removeAllViews();
        List<String> lettersFromDb;
        try {
            lettersFromDb = databaseHelper.getAllLetters();
            if (lettersFromDb.isEmpty()) {
                TextView errorText = new TextView(this);
                errorText.setText("Veritabanında veri yok.");
                letterGrid.addView(errorText);
                return;
            }
        } catch (Exception e) {
            TextView errorText = new TextView(this);
            errorText.setText("Veritabanı bağlantısı başarısız: " + e.getMessage());
            letterGrid.addView(errorText);
            return;
        }

        Random random = new Random();
        for (int i = 0; i < 5 * 6; i++) {
            TextView cell = new TextView(this);
            String randomLetter = lettersFromDb.get(random.nextInt(lettersFromDb.size()));
            cell.setText(randomLetter);
            cell.setGravity(Gravity.CENTER);
            cell.setTextSize(18);
            cell.setTypeface(Typeface.DEFAULT_BOLD);
            cell.setTextColor(getResources().getColor(android.R.color.black));
            cell.setBackgroundResource(R.drawable.border);
            cell.setOnClickListener(v -> toggleLetterSelection(cell));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = (int) (getResources().getDisplayMetrics().density * 48);
            params.height = (int) (getResources().getDisplayMetrics().density * 48);
            params.setMargins(4, 4, 4, 4);
            cell.setLayoutParams(params);

            letterGrid.addView(cell);
        }
    }

    private void toggleLetterSelection(TextView cell) {
        String letter = cell.getText().toString();
        int score = databaseHelper.getLetterScore(letter);

        if (selectedLetterViews.containsKey(cell)) {
            selectedLetterViews.remove(cell);
            cell.setBackgroundResource(R.drawable.border);
            currentTurnScore -= score;
            removeLastOccurrenceOfLetter(letter);
        } else {
            selectedLetterViews.put(cell, letter);
            cell.setBackgroundColor(getColorForScore(score));
            currentTurnScore += score;
            selectedWord.append(letter);
        }

        selectedWordScoreTextView.setText(getString(R.string.letter_score, currentTurnScore));
        updateSelectedLettersGrid();
    }

    private void removeLastOccurrenceOfLetter(String letter) {
        int index = selectedWord.lastIndexOf(letter);
        if (index != -1) {
            selectedWord.deleteCharAt(index);
        }
    }

    private void updateSelectedLettersGrid() {
        selectedLettersGrid.removeAllViews();
        int maxLetters = 30;
        int letterSize = (int) (getResources().getDisplayMetrics().density * 20);

        for (int i = 0; i < Math.min(selectedWord.length(), maxLetters); i++) {
            TextView letterView = new TextView(this);
            letterView.setText(String.valueOf(selectedWord.charAt(i)));
            letterView.setGravity(Gravity.CENTER);
            letterView.setTextSize(16);
            letterView.setTypeface(Typeface.DEFAULT_BOLD);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = letterSize;
            params.height = letterSize;
            params.setMargins(2, 2, 2, 2);
            letterView.setLayoutParams(params);

            selectedLettersGrid.addView(letterView);
        }
    }

    private int getColorForScore(int score) {
        int maxScore = 15;
        int minScore = 1;
        int lightYellow = 0xFFFFF9C4;
        int darkYellow = 0xFFF57F17;
        float ratio = (float) (score - minScore) / (maxScore - minScore);
        return blendColors(lightYellow, darkYellow, ratio);
    }

    private int blendColors(int color1, int color2, float ratio) {
        int r = (int) ((1 - ratio) * ((color1 >> 16) & 0xFF) + ratio * ((color2 >> 16) & 0xFF));
        int g = (int) ((1 - ratio) * ((color1 >> 8) & 0xFF) + ratio * ((color2 >> 8) & 0xFF));
        int b = (int) ((1 - ratio) * (color1 & 0xFF) + ratio * (color2 & 0xFF));
        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }

    private void checkAndConfirmWord() {
        String word = TurkishUtils.turkishToLower(selectedWord.toString());
        if (databaseHelper.isWordExists(word)) {
            showConfirmationDialog("Tebrikler! Kazanacağınız Kelime Puanı: " + currentTurnScore);
        } else {
            showConfirmationDialog("Üzgünüz, bu kelime veritabanında bulunamadı.");
        }
    }

    private void showConfirmationDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setPositiveButton("Tamam", (dialog, which) -> startNewTurn())
                .setNegativeButton("İptal", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void startNewTurn() {
        if (remainingTurns > 1) {
            userScore += currentTurnScore;
            currentTurnCount++;
            remainingTurns--;
            currentTurnScore = 0;
            updateUI();
            selectedWord.setLength(0);
            updateSelectedLettersGrid();
            resetSelections();
            createLetterGrid();
        } else {
            endGame();
        }
    }

    private void endGame() {
        // Son turdaki harf puanını güncel skora ekle
        userScore += currentTurnScore;

        // Güncellenen skoru UI üzerinde göster
        currentScore.setText(getString(R.string.current_score, userScore));

        // Oyun bittiğinde kullanıcıya bildirim ver
        Toast.makeText(this, "Oyun bitti!", Toast.LENGTH_SHORT).show();
        confirmWordButton.setEnabled(false);

        // Eğer hedef puana ulaşılmadıysa sayaç ve progress bar'ı başlat, aksi halde replay butonunu etkinleştir
        if (userScore < targetScoreValue) {
            startCountdown();
        } else {
            replayButton.setEnabled(true);
        }
    }


    private void resetSelections() {
        for (TextView cell : selectedLetterViews.keySet()) {
            cell.setBackgroundResource(R.drawable.border);
        }
        selectedLetterViews.clear();
        selectedLettersGrid.removeAllViews();
    }

    private void updateUI() {
        turnCount.setText(getString(R.string.turn_count, currentTurnCount));
        currentScore.setText(getString(R.string.current_score, userScore));
        balance.setText(getString(R.string.balance, userBalance));
        turnRight.setText(getString(R.string.turn_right, remainingTurns));
        targetScore.setText(getString(R.string.target_score, targetScoreValue));
        selectedWordScoreTextView.setText(getString(R.string.letter_score, currentTurnScore));
    }
}
