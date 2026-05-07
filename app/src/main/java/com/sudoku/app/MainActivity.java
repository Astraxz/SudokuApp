package com.sudoku.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // --- Constants ---
    private static final int SIZE = 9;
    private static final int[] TIME_LIMITS = {1800, 1200, 600};
    private static final String[] DIFF_NAMES = {"Easy", "Medium", "Hard"};
    private static final int COLOR_BG      = 0xFF0F1117;
    private static final int COLOR_SURFACE = 0xFF1A1D27;
    private static final int COLOR_CARD    = 0xFF22263A;
    private static final int COLOR_ACCENT  = 0xFF4FC3F7;
    private static final int COLOR_ACCENT2 = 0xFFFFB347;
    private static final int COLOR_FIXED   = 0xFFE0E0E0;
    private static final int COLOR_USER    = 0xFF4FC3F7;
    private static final int COLOR_ERROR   = 0xFFEF5350;
    private static final int COLOR_SUCCESS = 0xFF66BB6A;
    private static final int COLOR_GRID_LN = 0xFF2E3250;
    private static final int COLOR_SEL     = 0x334FC3F7;
    private static final int COLOR_SAME    = 0x1A4FC3F7;

    // --- State ---
    private final SudokuEngine engine = new SudokuEngine();
    private int selectedIndex = -1;
    private int difficulty = 0;
    private CountDownTimer timer;
    private long timeRemainingMs;
    private boolean gameActive = false;
    private boolean isUserInputMode = false;

    // --- UI Views ---
    private TextView tvTimer, tvScore, tvMistakes, tvDifficulty;
    private TextView[][] cells = new TextView[SIZE][SIZE];
    private LinearLayout gameLayout, homeLayout, numPad;
    private Button btnValidate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUI();
        showHomeScreen();
    }

    private void showHomeScreen() {
        if (timer != null) timer.cancel();
        gameActive = false;
        gameLayout.setVisibility(View.GONE);
        homeLayout.setVisibility(View.VISIBLE);
    }

    private void buildUI() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(COLOR_BG);
        setContentView(root);

        // --- Home Screen ---
        homeLayout = new LinearLayout(this);
        homeLayout.setOrientation(LinearLayout.VERTICAL);
        homeLayout.setGravity(Gravity.CENTER);
        homeLayout.setPadding(dp(40), 0, dp(40), 0);

        TextView title = new TextView(this);
        title.setText("SUDOKU");
        title.setTextSize(48);
        title.setTextColor(COLOR_ACCENT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        homeLayout.addView(title);

        space(homeLayout, 60);

        addButton(homeLayout, "PLAY GAME", COLOR_ACCENT, v -> showDifficultyDialog());
        space(homeLayout, 16);
        addButton(homeLayout, "USER INPUT", COLOR_ACCENT2, v -> startUserInputMode());
        space(homeLayout, 16);
        addButton(homeLayout, "LEADERBOARD", 0xFF9575CD, v -> showLeaderboard());

        root.addView(homeLayout);

        // --- Game Screen ---
        gameLayout = new LinearLayout(this);
        gameLayout.setOrientation(LinearLayout.VERTICAL);
        gameLayout.setPadding(dp(16), dp(16), dp(16), dp(16));
        gameLayout.setVisibility(View.GONE);

        // Header
        LinearLayout header = new LinearLayout(this);
        tvDifficulty = new TextView(this);
        tvDifficulty.setTextColor(COLOR_ACCENT2);
        tvDifficulty.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(tvDifficulty, new LinearLayout.LayoutParams(0, -2, 1f));
        
        Button btnBack = new Button(this);
        btnBack.setText("BACK");
        btnBack.setOnClickListener(v -> showHomeScreen());
        header.addView(btnBack);
        gameLayout.addView(header);

        space(gameLayout, 12);

        // Stats
        LinearLayout stats = new LinearLayout(this);
        setRoundBg(stats, COLOR_SURFACE, 12);
        stats.setPadding(dp(12), dp(10), dp(12), dp(10));
        tvTimer = statView("00:00");
        tvScore = statView("0");
        tvMistakes = statView("0");
        stats.addView(wrapStat(tvTimer, "TIME"), new LinearLayout.LayoutParams(0, -2, 1f));
        stats.addView(wrapStat(tvScore, "SCORE"), new LinearLayout.LayoutParams(0, -2, 1f));
        stats.addView(wrapStat(tvMistakes, "MISTAKES"), new LinearLayout.LayoutParams(0, -2, 1f));
        gameLayout.addView(stats);

        space(gameLayout, 16);

        // Grid
        LinearLayout gridBox = new LinearLayout(this);
        gridBox.setOrientation(LinearLayout.VERTICAL);
        gridBox.setBackgroundColor(COLOR_ACCENT);
        gridBox.setPadding(dp(2), dp(2), dp(2), dp(2));
        setRoundBg(gridBox, COLOR_ACCENT, 8);
        
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setBackgroundColor(COLOR_CARD);
        setRoundBg(grid, COLOR_CARD, 6);
        gridBox.addView(grid);

        for (int r = 0; r < SIZE; r++) {
            if (r > 0 && r % 3 == 0) addLine(grid, true);
            LinearLayout row = new LinearLayout(this);
            for (int c = 0; c < SIZE; c++) {
                if (c > 0 && c % 3 == 0) addLine(row, false);
                TextView cell = new TextView(this);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(20);
                cell.setTypeface(Typeface.DEFAULT_BOLD);
                final int idx = r * SIZE + c;
                cell.setOnClickListener(v -> onCellClick(idx));
                cells[r][c] = cell;
                row.addView(cell, new LinearLayout.LayoutParams(0, dp(44), 1f));
            }
            grid.addView(row);
        }
        gameLayout.addView(gridBox);

        space(gameLayout, 20);

        // Num Pad
        numPad = new LinearLayout(this);
        for (int i = 1; i <= 9; i++) {
            final int n = i;
            Button b = new Button(this);
            b.setText(String.valueOf(n));
            b.setOnClickListener(v -> onNumClick(n));
            numPad.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1f));
        }
        gameLayout.addView(numPad);

        space(gameLayout, 12);

        btnValidate = new Button(this);
        btnValidate.setText("VALIDATE & START");
        btnValidate.setOnClickListener(v -> validateUserInput());
        btnValidate.setVisibility(View.GONE);
        gameLayout.addView(btnValidate);

        Button btnErase = new Button(this);
        btnErase.setText("ERASE");
        btnErase.setOnClickListener(v -> eraseCell());
        gameLayout.addView(btnErase);

        root.addView(gameLayout);
    }

    private void showDifficultyDialog() {
        String[] opts = {"Easy", "Medium", "Hard"};
        new AlertDialog.Builder(this).setTitle("Difficulty")
            .setItems(opts, (d, i) -> startNewGame(i)).show();
    }

    private void startNewGame(int diff) {
        difficulty = diff;
        isUserInputMode = false;
        engine.initNewGame(diff);
        setupGameUI();
        startTimer();
    }

    private void startUserInputMode() {
        isUserInputMode = true;
        engine.initUserInputMode();
        setupGameUI();
        tvTimer.setText("SETUP");
    }

    private void setupGameUI() {
        homeLayout.setVisibility(View.GONE);
        gameLayout.setVisibility(View.VISIBLE);
        btnValidate.setVisibility(isUserInputMode ? View.VISIBLE : View.GONE);
        tvDifficulty.setText(isUserInputMode ? "CUSTOM" : DIFF_NAMES[difficulty].toUpperCase());
        selectedIndex = -1;
        gameActive = true;
        refreshGrid();
        updateStats();
    }

    private void validateUserInput() {
        int res = engine.validateAndStart();
        if (res == 1) {
            isUserInputMode = false;
            btnValidate.setVisibility(View.GONE);
            startTimer();
            toast("Puzzle Validated! Start!");
        } else if (res == -1) {
            toast("Conflicts detected!");
        } else {
            toast("Unsolvable puzzle!");
        }
    }

    private void onCellClick(int idx) {
        selectedIndex = idx;
        refreshGrid();
    }

    private void onNumClick(int n) {
        if (!gameActive || selectedIndex < 0) return;
        int r = selectedIndex / SIZE, c = selectedIndex % SIZE;
        int res = engine.handleInput(r, c, n);
        
        if (res == 1) {
            animateCell(r, c, true);
            if (!isUserInputMode && engine.isComplete()) win();
        } else if (res == -1) {
            animateCell(r, c, false);
        }
        refreshGrid();
        updateStats();
    }

    private void eraseCell() {
        if (selectedIndex < 0) return;
        engine.eraseCell(selectedIndex / SIZE, selectedIndex % SIZE);
        refreshGrid();
    }

    private void refreshGrid() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int val = engine.getCellValue(r, c);
                boolean fixed = engine.isCellFixed(r, c);
                TextView tv = cells[r][c];
                tv.setText(val == 0 ? "" : String.valueOf(val));
                tv.setTextColor(fixed ? COLOR_FIXED : COLOR_USER);
                
                int idx = r * SIZE + c;
                if (idx == selectedIndex) tv.setBackgroundColor(COLOR_SEL);
                else tv.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    private void updateStats() {
        tvScore.setText(String.valueOf(engine.getScore()));
        tvMistakes.setText(String.valueOf(engine.getMistakes()));
    }

    private void startTimer() {
        if (timer != null) timer.cancel();
        timeRemainingMs = TIME_LIMITS[difficulty] * 1000;
        timer = new CountDownTimer(timeRemainingMs, 1000) {
            public void onTick(long ms) {
                timeRemainingMs = ms;
                long s = ms / 1000;
                tvTimer.setText(String.format("%02d:%02d", s/60, s%60));
            }
            public void onFinish() { gameActive = false; toast("Time Up!"); }
        }.start();
    }

    private void win() {
        gameActive = false;
        if (timer != null) timer.cancel();
        int finalScore = engine.getScore() + (int)(timeRemainingMs/1000);
        saveScore(finalScore);
        new AlertDialog.Builder(this).setTitle("You Win!")
            .setMessage("Score: " + finalScore).setPositiveButton("OK", (d, w) -> showHomeScreen()).show();
    }

    private void saveScore(int s) {
        SharedPreferences pref = getSharedPreferences("sudoku", MODE_PRIVATE);
        String saved = pref.getString("scores", "");
        saved += s + ",";
        pref.edit().putString("scores", saved).apply();
    }

    private void showLeaderboard() {
        SharedPreferences pref = getSharedPreferences("sudoku", MODE_PRIVATE);
        String[] parts = pref.getString("scores", "").split(",");
        List<Integer> scores = new ArrayList<>();
        for (String p : parts) if (!p.isEmpty()) scores.add(Integer.parseInt(p));
        Collections.sort(scores, Collections.reverseOrder());
        
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<Math.min(scores.size(), 10); i++) 
            sb.append(i+1).append(". ").append(scores.get(i)).append("\n");
            
        new AlertDialog.Builder(this).setTitle("Leaderboard")
            .setMessage(sb.length() > 0 ? sb.toString() : "No scores yet").show();
    }

    // --- Helpers ---
    private void addButton(LinearLayout p, String t, int c, View.OnClickListener l) {
        Button b = new Button(this); b.setText(t); b.setBackgroundColor(c); b.setOnClickListener(l);
        p.addView(b, new LinearLayout.LayoutParams(-1, dp(56)));
    }
    private void space(LinearLayout p, int d) {
        View v = new View(this); p.addView(v, new LinearLayout.LayoutParams(1, dp(d)));
    }
    private TextView statView(String v) {
        TextView t = new TextView(this); t.setText(v); t.setTextSize(18); 
        t.setTextColor(COLOR_ACCENT); t.setGravity(Gravity.CENTER); return t;
    }
    private LinearLayout wrapStat(TextView v, String l) {
        LinearLayout ll = new LinearLayout(this); ll.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this); label.setText(l); label.setTextSize(10);
        label.setGravity(Gravity.CENTER); label.setTextColor(Color.GRAY);
        ll.addView(v); ll.addView(label); return ll;
    }
    private void addLine(LinearLayout p, boolean h) {
        View v = new View(this); v.setBackgroundColor(COLOR_GRID_LN);
        p.addView(v, h ? new LinearLayout.LayoutParams(-1, dp(1)) : new LinearLayout.LayoutParams(dp(1), -1));
    }
    private void setRoundBg(View v, int c, int r) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(c); g.setCornerRadius(dp(r)); v.setBackground(g);
    }
    private void animateCell(int r, int c, boolean s) {
        TextView tv = cells[r][c];
        ObjectAnimator a = ObjectAnimator.ofFloat(tv, "translationX", 0, -10, 10, 0);
        if (s) a = ObjectAnimator.ofFloat(tv, "scaleX", 1f, 1.2f, 1f);
        a.setDuration(300).start();
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
