#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#define SIZE 9

// ──────────────────────────────────────────────
//  CORE LOGIC (no printf / scanf)
// ──────────────────────────────────────────────

static bool isSafe(int grid[SIZE][SIZE], int row, int col, int num) {
    for (int x = 0; x < SIZE; x++)
        if (grid[row][x] == num || grid[x][col] == num) return false;
    int sr = row - row % 3, sc = col - col % 3;
    for (int i = 0; i < 3; i++)
        for (int j = 0; j < 3; j++)
            if (grid[i + sr][j + sc] == num) return false;
    return true;
}

static bool findEmptyCell(int grid[SIZE][SIZE], int *row, int *col) {
    for (*row = 0; *row < SIZE; (*row)++)
        for (*col = 0; *col < SIZE; (*col)++)
            if (grid[*row][*col] == 0) return true;
    return false;
}

static bool solveSudoku(int grid[SIZE][SIZE]) {
    int row, col;
    if (!findEmptyCell(grid, &row, &col)) return true;
    for (int num = 1; num <= SIZE; num++) {
        if (isSafe(grid, row, col, num)) {
            grid[row][col] = num;
            if (solveSudoku(grid)) return true;
            grid[row][col] = 0;
        }
    }
    return false;
}

static bool fillGrid(int grid[SIZE][SIZE]) {
    int row, col;
    if (!findEmptyCell(grid, &row, &col)) return true;
    int nums[SIZE] = {1,2,3,4,5,6,7,8,9};
    for (int i = SIZE - 1; i > 0; i--) {
        int j = rand() % (i + 1);
        int tmp = nums[i]; nums[i] = nums[j]; nums[j] = tmp;
    }
    for (int k = 0; k < SIZE; k++) {
        if (isSafe(grid, row, col, nums[k])) {
            grid[row][col] = nums[k];
            if (fillGrid(grid)) return true;
            grid[row][col] = 0;
        }
    }
    return false;
}

static bool validateInitialGrid(int grid[SIZE][SIZE]) {
    for (int r = 0; r < SIZE; r++) {
        for (int c = 0; c < SIZE; c++) {
            int num = grid[r][c];
            if (num != 0) {
                grid[r][c] = 0;
                if (!isSafe(grid, r, c, num)) return false;
                grid[r][c] = num;
            }
        }
    }
    return true;
}

// ──────────────────────────────────────────────
//  JNI HELPERS  (flat int array ↔ 2D grid)
// ──────────────────────────────────────────────

static void jArrayToGrid(JNIEnv *env, jintArray arr, int grid[SIZE][SIZE]) {
    jint *data = (*env)->GetIntArrayElements(env, arr, NULL);
    for (int i = 0; i < SIZE * SIZE; i++)
        grid[i / SIZE][i % SIZE] = data[i];
    (*env)->ReleaseIntArrayElements(env, arr, data, JNI_ABORT);
}

static jintArray gridToJArray(JNIEnv *env, int grid[SIZE][SIZE]) {
    jintArray result = (*env)->NewIntArray(env, SIZE * SIZE);
    jint tmp[SIZE * SIZE];
    for (int i = 0; i < SIZE * SIZE; i++)
        tmp[i] = grid[i / SIZE][i % SIZE];
    (*env)->SetIntArrayRegion(env, result, 0, SIZE * SIZE, tmp);
    return result;
}

// ──────────────────────────────────────────────
//  JNI EXPORTS
//  Package: com.sudoku.app
// ──────────────────────────────────────────────

// Generate a complete solved grid, return as flat int[81]
JNIEXPORT jintArray JNICALL
Java_com_sudoku_app_SudokuEngine_generateSolvedGrid(JNIEnv *env, jobject thiz) {
    srand((unsigned int)time(NULL));
    int grid[SIZE][SIZE] = {0};
    fillGrid(grid);
    return gridToJArray(env, grid);
}

// Given a solved grid + difficulty (0=easy,1=med,2=hard), return puzzle int[81]
JNIEXPORT jintArray JNICALL
Java_com_sudoku_app_SudokuEngine_generatePuzzle(JNIEnv *env, jobject thiz,
                                                 jintArray solvedArr, jint difficulty) {
    int cellsToHide;
    switch (difficulty) {
        case 0:  cellsToHide = 30; break;
        case 1:  cellsToHide = 45; break;
        default: cellsToHide = 55; break;
    }

    int grid[SIZE][SIZE];
    jArrayToGrid(env, solvedArr, grid);

    // Shuffle positions and blank out cellsToHide cells
    int positions[SIZE * SIZE];
    for (int i = 0; i < SIZE * SIZE; i++) positions[i] = i;
    for (int i = SIZE * SIZE - 1; i > 0; i--) {
        int j = rand() % (i + 1);
        int tmp = positions[i]; positions[i] = positions[j]; positions[j] = tmp;
    }
    for (int k = 0; k < cellsToHide; k++)
        grid[positions[k] / SIZE][positions[k] % SIZE] = 0;

    return gridToJArray(env, grid);
}

// Solve the given puzzle; returns solved int[81] or null if unsolvable
JNIEXPORT jintArray JNICALL
Java_com_sudoku_app_SudokuEngine_solve(JNIEnv *env, jobject thiz, jintArray puzzleArr) {
    int grid[SIZE][SIZE];
    jArrayToGrid(env, puzzleArr, grid);
    if (!solveSudoku(grid)) return NULL;
    return gridToJArray(env, grid);
}

// Returns 1 if placing `num` at (row,col) is safe, 0 otherwise
JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_isMoveValid(JNIEnv *env, jobject thiz,
                                              jintArray gridArr, jint row, jint col, jint num) {
    int grid[SIZE][SIZE];
    jArrayToGrid(env, gridArr, grid);
    return isSafe(grid, (int)row, (int)col, (int)num) ? 1 : 0;
}

// Validate an externally supplied initial grid; returns 1 if valid
JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_validateGrid(JNIEnv *env, jobject thiz, jintArray gridArr) {
    int grid[SIZE][SIZE];
    jArrayToGrid(env, gridArr, grid);
    return validateInitialGrid(grid) ? 1 : 0;
}

// Returns 1 if no empty cells remain (puzzle complete)
JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_isPuzzleComplete(JNIEnv *env, jobject thiz, jintArray gridArr) {
    int grid[SIZE][SIZE];
    jArrayToGrid(env, gridArr, grid);
    int row, col;
    return findEmptyCell(grid, &row, &col) ? 0 : 1;
}
