package com.sudoku.app;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
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
import androidx.core.content.res.ResourcesCompat;

import java.util.Arrays;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // ── Constants ──────────────────────────────
    private static final int SIZE = 9;
    private static final int[] TIME_LIMITS = {1800, 1200, 600}; // Easy/Med/Hard seconds
    private static final String[] DIFF_NAMES = {"Easy", "Medium", "Hard"};
    private static final int COLOR_BG         = 0xFF0F1117;
    private static final int COLOR_SURFACE    = 0xFF1A1D27;
    private static final int COLOR_CARD       = 0xFF22263A;
    private static final int COLOR_ACCENT     = 0xFF4FC3F7;
    private static final int COLOR_ACCENT2    = 0xFFFFB347;
    private static final int COLOR_FIXED      = 0xFFE0E0E0;
    private static final int COLOR_USER       = 0xFF4FC3F7;
    private static final int COLOR_ERROR      = 0xFFEF5350;
    private static final int COLOR_SUCCESS    = 0xFF66BB6A;
    private static final int COLOR_GRID_LINE  = 0xFF2E3250;
    private static final int COLOR_SELECTED   = 0x334FC3F7;
    private static final int COLOR_SAME_NUM   = 0x1A4FC3F7;

    // ── State ──────────────────────────────────
    private final SudokuEngine engine = new SudokuEngine();
    private int[] solvedGrid;
    private int[] puzzleGrid;       // current board (flat 81)
    private boolean[] fixedCells;   // original non-zero cells
    private int selectedIndex = -1;
    private int difficulty = 0;     // 0=Easy 1=Med 2=Hard
    private int score = 0;
    private int mistakeCount = 0;
    private CountDownTimer countDownTimer;
    private long timeRemainingMs;
    private boolean gameActive = false;

    // ── Views ──────────────────────────────────
    private TextView tvTimer, tvScore, tvMistakes, tvDifficulty;
    private TextView[][] cells = new TextView[SIZE][SIZE];
    private LinearLayout numPad;
    private Button btnNewGame, btnSolve, btnErase;

    // ──────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUI();
        showDifficultyDialog();
    }

    // ──────────────────────────────────────────
    //  UI CONSTRUCTION (programmatic, no XML needed)
    // ──────────────────────────────────────────
    @SuppressLint("SetTextI18n")
    private void buildUI() {
        // Root scroll
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(COLOR_BG);
        scroll.setFillViewport(true);
        setContentView(scroll);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setPadding(dp(16), dp(24), dp(16), dp(24));
        scroll.addView(root);

        // ── Title bar ─────────────────────────
        LinearLayout titleRow = row(root, LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("SUDOKU");
        title.setTextSize(28);
        title.setTextColor(COLOR_ACCENT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setLetterSpacing(0.15f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleRow.addView(title, titleParams);

        tvDifficulty = new TextView(this);
        tvDifficulty.setText("EASY");
        tvDifficulty.setTextSize(13);
        tvDifficulty.setTextColor(COLOR_ACCENT2);
        tvDifficulty.setTypeface(Typeface.DEFAULT_BOLD);
        tvDifficulty.setLetterSpacing(0.1f);
        titleRow.addView(tvDifficulty);

        space(root, 12);

        // ── Stats row ─────────────────────────
        LinearLayout statsRow = row(root, LinearLayout.HORIZONTAL);
        statsRow.setBackgroundColor(COLOR_SURFACE);
        statsRow.setPadding(dp(12), dp(10), dp(12), dp(10));
        setRoundBg(statsRow, COLOR_SURFACE, 12);

        tvTimer    = statView("30:00", "TIME");
        tvScore    = statView("0",     "SCORE");
        tvMistakes = statView("0",     "MISTAKES");

        statsRow.addView(wrapStat(tvTimer),    statParam());
        addDivider(statsRow);
        statsRow.addView(wrapStat(tvScore),    statParam());
        addDivider(statsRow);
        statsRow.addView(wrapStat(tvMistakes), statParam());

        space(root, 16);

        // ── Sudoku grid ───────────────────────
        LinearLayout gridWrapper = new LinearLayout(this);
        gridWrapper.setOrientation(LinearLayout.VERTICAL);
        gridWrapper.setBackgroundColor(COLOR_ACCENT);
        gridWrapper.setPadding(dp(2), dp(2), dp(2), dp(2));
        setRoundBg(gridWrapper, COLOR_ACCENT, 8);

        LinearLayout gridContainer = new LinearLayout(this);
        gridContainer.setOrientation(LinearLayout.VERTICAL);
        gridContainer.setBackgroundColor(COLOR_CARD);
        setRoundBg(gridContainer, COLOR_CARD, 6);
        gridWrapper.addView(gridContainer);

        for (int r = 0; r < SIZE; r++) {
            if (r > 0 && r % 3 == 0) {
                View thickLine = new View(this);
                thickLine.setBackgroundColor(COLOR_ACCENT);
                gridContainer.addView(thickLine, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(2)));
            }
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);

            for (int c = 0; c < SIZE; c++) {
                if (c > 0 && c % 3 == 0) {
                    View thickLine = new View(this);
                    thickLine.setBackgroundColor(COLOR_ACCENT);
                    rowLayout.addView(thickLine, new LinearLayout.LayoutParams(
                            dp(2), LinearLayout.LayoutParams.MATCH_PARENT));
                } else if (c > 0) {
                    View thinLine = new View(this);
                    thinLine.setBackgroundColor(COLOR_GRID_LINE);
                    rowLayout.addView(thinLine, new LinearLayout.LayoutParams(
                            dp(1), LinearLayout.LayoutParams.MATCH_PARENT));
                }

                TextView cell = new TextView(this);
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(18);
                cell.setTypeface(Typeface.DEFAULT_BOLD);
                cell.setTextColor(COLOR_FIXED);
                cell.setBackgroundColor(COLOR_CARD);

                final int index = r * SIZE + c;
                cell.setOnClickListener(v -> onCellClick(index));
                cells[r][c] = cell;

                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                        dp(40), 1f);
                rowLayout.addView(cell, cp);
            }
            gridContainer.addView(rowLayout, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            if (r < SIZE - 1 && (r + 1) % 3 != 0) {
                View thinLine = new View(this);
                thinLine.setBackgroundColor(COLOR_GRID_LINE);
                gridContainer.addView(thinLine, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
            }
        }

        LinearLayout.LayoutParams gwp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        root.addView(gridWrapper, gwp);

        space(root, 20);

        // ── Number pad ────────────────────────
        numPad = new LinearLayout(this);
        numPad.setOrientation(LinearLayout.HORIZONTAL);
        numPad.setGravity(Gravity.CENTER);

        for (int n = 1; n <= 9; n++) {
            Button nb = new Button(this);
            nb.setText(String.valueOf(n));
            nb.setTextSize(18);
            nb.setTextColor(COLOR_ACCENT);
            nb.setTypeface(Typeface.DEFAULT_BOLD);
            nb.setBackgroundColor(COLOR_CARD);
            nb.setPadding(0, dp(8), 0, dp(8));
            setRoundBg(nb, COLOR_CARD, 8);

            final int num = n;
            nb.setOnClickListener(v -> onNumPadClick(num));

            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(0,
                    dp(48), 1f);
            np.setMargins(dp(3), 0, dp(3), 0);
            numPad.addView(nb, np);
        }
        root.addView(numPad);

        space(root, 12);

        // ── Erase button ──────────────────────
        btnErase = new Button(this);
        btnErase.setText("⌫  ERASE");
        btnErase.setTextSize(14);
        btnErase.setTextColor(COLOR_ERROR);
        btnErase.setTypeface(Typeface.DEFAULT_BOLD);
        setRoundBg(btnErase, COLOR_SURFACE, 10);
        btnErase.setOnClickListener(v -> eraseCell());
        LinearLayout.LayoutParams eraseP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(44));
        eraseP.setMargins(0, 0, 0, 0);
        root.addView(btnErase, eraseP);

        space(root, 12);

        // ── Action buttons ────────────────────
        LinearLayout actionRow = row(root, LinearLayout.HORIZONTAL);

        btnNewGame = actionBtn("NEW GAME", COLOR_ACCENT);
        btnNewGame.setOnClickListener(v -> showDifficultyDialog());
        actionRow.addView(btnNewGame, new LinearLayout.LayoutParams(0, dp(48), 1f));

        space(actionRow, 10);

        btnSolve = actionBtn("AUTO SOLVE", COLOR_ACCENT2);
        btnSolve.setOnClickListener(v -> autoSolve());
        actionRow.addView(btnSolve, new LinearLayout.LayoutParams(0, dp(48), 1f));
    }

    // ──────────────────────────────────────────
    //  GAME FLOW
    // ──────────────────────────────────────────
    private void showDifficultyDialog() {
        if (countDownTimer != null) countDownTimer.cancel();
        gameActive = false;

        String[] options = {"🟢  Easy  (30 min)", "🟡  Medium  (20 min)", "🔴  Hard  (10 min)"};
        new AlertDialog.Builder(this)
                .setTitle("Select Difficulty")
                .setSingleChoiceItems(options, difficulty, (d, which) -> difficulty = which)
                .setPositiveButton("START", (d, w) -> startNewGame())
                .setCancelable(false)
                .show();
    }

    private void startNewGame() {
        score = 0;
        mistakeCount = 0;
        selectedIndex = -1;
        gameActive = true;

        tvDifficulty.setText(DIFF_NAMES[difficulty].toUpperCase(Locale.ROOT));
        updateScore();
        updateMistakes();

        // Generate via NDK
        solvedGrid  = engine.generateSolvedGrid();
        int[] puzzle = engine.generatePuzzle(solvedGrid, difficulty);
        puzzleGrid   = Arrays.copyOf(puzzle, puzzle.length);

        fixedCells = new boolean[SIZE * SIZE];
        for (int i = 0; i < SIZE * SIZE; i++)
            fixedCells[i] = (puzzleGrid[i] != 0);

        renderGrid();
        startTimer();
    }

    private void startTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timeRemainingMs = (long) TIME_LIMITS[difficulty] * 1000;

        countDownTimer = new CountDownTimer(timeRemainingMs, 1000) {
            @Override public void onTick(long ms) {
                timeRemainingMs = ms;
                long secs = ms / 1000;
                tvTimer.setText(String.format(Locale.ROOT, "%02d:%02d", secs / 60, secs % 60));
                if (secs <= 60) tvTimer.setTextColor(COLOR_ERROR);
                else if (secs <= 180) tvTimer.setTextColor(COLOR_ACCENT2);
                else tvTimer.setTextColor(COLOR_SUCCESS);
            }
            @Override public void onFinish() {
                tvTimer.setText("00:00");
                tvTimer.setTextColor(COLOR_ERROR);
                gameActive = false;
                showTimeUpDialog();
            }
        }.start();
    }

    private void onCellClick(int index) {
        if (!gameActive) return;
        selectedIndex = index;
        renderGrid();
    }

    private void onNumPadClick(int num) {
        if (!gameActive || selectedIndex < 0) return;
        if (fixedCells[selectedIndex]) {
            toast("Fixed cell — cannot change!");
            return;
        }

        int row = selectedIndex / SIZE;
        int col = selectedIndex % SIZE;

        // Temporarily clear the cell for validation
        int prev = puzzleGrid[selectedIndex];
        puzzleGrid[selectedIndex] = 0;
        int valid = engine.isMoveValid(puzzleGrid, row, col, num);
        puzzleGrid[selectedIndex] = prev;

        if (valid == 1) {
            puzzleGrid[selectedIndex] = num;
            score += 10;
            updateScore();
            animateCell(row, col, true);
            renderGrid();
            checkCompletion();
        } else {
            mistakeCount++;
            updateMistakes();
            animateCell(row, col, false);
            // Show wrong number briefly then clear
            cells[row][col].setText(String.valueOf(num));
            cells[row][col].setTextColor(COLOR_ERROR);
            cells[row][col].postDelayed(() -> renderGrid(), 600);
        }
    }

    private void eraseCell() {
        if (!gameActive || selectedIndex < 0) return;
        if (fixedCells[selectedIndex]) { toast("Cannot erase a fixed cell!"); return; }
        puzzleGrid[selectedIndex] = 0;
        renderGrid();
    }

    private void autoSolve() {
        if (countDownTimer != null) countDownTimer.cancel();
        gameActive = false;
        int[] result = engine.solve(puzzleGrid);
        if (result == null) { toast("This puzzle has no solution!"); return; }
        puzzleGrid = result;
        renderGrid();
        tvTimer.setText("SOLVED");
        tvTimer.setTextColor(COLOR_SUCCESS);
    }

    private void checkCompletion() {
        if (engine.isPuzzleComplete(puzzleGrid) == 1) {
            if (countDownTimer != null) countDownTimer.cancel();
            gameActive = false;
            long bonusSecs = timeRemainingMs / 1000;
            int totalScore = score + (int) bonusSecs;
            showWinDialog(totalScore, (int) bonusSecs);
        }
    }

    // ──────────────────────────────────────────
    //  RENDERING
    // ──────────────────────────────────────────
    private void renderGrid() {
        int selNum = (selectedIndex >= 0) ? puzzleGrid[selectedIndex] : 0;

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int idx = r * SIZE + c;
                int val = puzzleGrid[idx];
                TextView cell = cells[r][c];

                // Background
                int bg;
                if (idx == selectedIndex)          bg = COLOR_SELECTED;
                else if (selNum > 0 && val == selNum) bg = COLOR_SAME_NUM;
                else                                bg = COLOR_CARD;
                cell.setBackgroundColor(bg);

                // Text
                if (val == 0) {
                    cell.setText("");
                } else {
                    cell.setText(String.valueOf(val));
                    cell.setTextColor(fixedCells[idx] ? COLOR_FIXED : COLOR_USER);
                }
            }
        }
    }

    // ──────────────────────────────────────────
    //  DIALOGS
    // ──────────────────────────────────────────
    private void showWinDialog(int total, int timeBonus) {
        new AlertDialog.Builder(this)
                .setTitle("🎉 Puzzle Complete!")
                .setMessage(String.format(Locale.ROOT,
                        "Move Score:  +%d pts\nTime Bonus:  +%d pts\n\nTOTAL SCORE:  %d",
                        score, timeBonus, total))
                .setPositiveButton("New Game", (d, w) -> showDifficultyDialog())
                .setNegativeButton("Close", null)
                .setCancelable(false)
                .show();
    }

    private void showTimeUpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("⏰ Time's Up!")
                .setMessage("You ran out of time!\n\nFinal Score: " + score)
                .setPositiveButton("Try Again", (d, w) -> showDifficultyDialog())
                .setNegativeButton("Show Solution", (d, w) -> autoSolve())
                .setCancelable(false)
                .show();
    }

    // ──────────────────────────────────────────
    //  ANIMATIONS
    // ──────────────────────────────────────────
    private void animateCell(int r, int c, boolean success) {
        TextView cell = cells[r][c];
        if (success) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(cell, "scaleX", 1f, 1.25f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(cell, "scaleY", 1f, 1.25f, 1f);
            AnimatorSet set = new AnimatorSet();
            set.playTogether(scaleX, scaleY);
            set.setDuration(300);
            set.setInterpolator(new OvershootInterpolator());
            set.start();
        } else {
            ObjectAnimator shake = ObjectAnimator.ofFloat(cell, "translationX",
                    0f, -10f, 10f, -8f, 8f, -4f, 4f, 0f);
            shake.setDuration(400);
            shake.start();
        }
    }

    // ──────────────────────────────────────────
    //  HELPER BUILDERS
    // ──────────────────────────────────────────
    private LinearLayout row(LinearLayout parent, int orientation) {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(orientation);
        parent.addView(ll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return ll;
    }

    private void space(LinearLayout parent, int dpVal) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(dpVal)));
    }

    private TextView statView(String value, String label) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(20);
        tv.setTextColor(COLOR_ACCENT);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private LinearLayout wrapStat(TextView valueView) {
        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(Gravity.CENTER);
        wrap.addView(valueView);
        return wrap;
    }

    private LinearLayout.LayoutParams statParam() {
        return new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
    }

    private void addDivider(LinearLayout row) {
        View d = new View(this);
        d.setBackgroundColor(COLOR_GRID_LINE);
        row.addView(d, new LinearLayout.LayoutParams(dp(1),
                LinearLayout.LayoutParams.MATCH_PARENT));
    }

    private Button actionBtn(String text, int color) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTextColor(COLOR_BG);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setLetterSpacing(0.08f);
        setRoundBg(b, color, 10);
        return b;
    }

    private void setRoundBg(View v, int color, int radiusDp) {
        android.graphics.drawable.GradientDrawable gd =
                new android.graphics.drawable.GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(radiusDp));
        v.setBackground(gd);
    }

    private void updateScore() {
        tvScore.setText(String.valueOf(score));
    }

    private void updateMistakes() {
        tvMistakes.setText(String.valueOf(mistakeCount));
        if (mistakeCount >= 3) tvMistakes.setTextColor(COLOR_ERROR);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int val) {
        return Math.round(val * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
    }
}
