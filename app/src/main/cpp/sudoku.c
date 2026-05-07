#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

#define SIZE 9

// ── Game State ──────────────────────────────
typedef struct {
    int grid[SIZE][SIZE];
    int solved[SIZE][SIZE];
    bool fixed[SIZE][SIZE];
    int score;
    int mistakes;
    int difficulty;
    bool active;
    bool is_user_input;
} GameState;

static GameState g_state;

// ── Core Sudoku Logic ───────────────────────

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

// ── JNI Helpers ─────────────────────────────

static void jArrayToGrid(JNIEnv *env, jintArray arr, int grid[SIZE][SIZE]) {
    jint *data = (*env)->GetIntArrayElements(env, arr, NULL);
    for (int i = 0; i < SIZE * SIZE; i++)
        grid[i / SIZE][i % SIZE] = (int)data[i];
    (*env)->ReleaseIntArrayElements(env, arr, data, JNI_ABORT);
}

static jintArray gridToJArray(JNIEnv *env, int grid[SIZE][SIZE]) {
    jintArray result = (*env)->NewIntArray(env, SIZE * SIZE);
    jint tmp[SIZE * SIZE];
    for (int i = 0; i < SIZE * SIZE; i++)
        tmp[i] = (jint)grid[i / SIZE][i % SIZE];
    (*env)->SetIntArrayRegion(env, result, 0, SIZE * SIZE, tmp);
    return result;
}

// ── JNI Exports (Game Session Controller) ───

JNIEXPORT void JNICALL
Java_com_sudoku_app_SudokuEngine_initNewGame(JNIEnv *env, jobject thiz, jint difficulty) {
    srand((unsigned int)time(NULL));
    memset(&g_state, 0, sizeof(GameState));

    fillGrid(g_state.solved);

    int cellsToHide;
    switch (difficulty) {
        case 0:  cellsToHide = 30; break;
        case 1:  cellsToHide = 45; break;
        default: cellsToHide = 55; break;
    }

    memcpy(g_state.grid, g_state.solved, sizeof(g_state.grid));

    int positions[SIZE * SIZE];
    for (int i = 0; i < SIZE * SIZE; i++) positions[i] = i;
    for (int i = SIZE * SIZE - 1; i > 0; i--) {
        int j = rand() % (i + 1);
        int tmp = positions[i]; positions[i] = positions[j]; positions[j] = tmp;
    }
    for (int k = 0; k < cellsToHide; k++) {
        int r = positions[k] / SIZE;
        int c = positions[k] % SIZE;
        g_state.grid[r][c] = 0;
    }

    for (int r = 0; r < SIZE; r++)
        for (int c = 0; c < SIZE; c++)
            g_state.fixed[r][c] = (g_state.grid[r][c] != 0);

    g_state.difficulty = difficulty;
    g_state.active = true;
    g_state.is_user_input = false;
}

JNIEXPORT void JNICALL
Java_com_sudoku_app_SudokuEngine_initUserInputMode(JNIEnv *env, jobject thiz) {
    memset(&g_state, 0, sizeof(GameState));
    g_state.active = true;
    g_state.is_user_input = true;
}

JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_validateAndStart(JNIEnv *env, jobject thiz) {
    for (int r = 0; r < SIZE; r++) {
        for (int c = 0; c < SIZE; c++) {
            int num = g_state.grid[r][c];
            if (num != 0) {
                g_state.grid[r][c] = 0;
                if (!isSafe(g_state.grid, r, c, num)) {
                    g_state.grid[r][c] = num;
                    return -1;
                }
                g_state.grid[r][c] = num;
            }
        }
    }

    int tempGrid[SIZE][SIZE];
    memcpy(tempGrid, g_state.grid, sizeof(tempGrid));
    if (!solveSudoku(tempGrid)) return 0;

    memcpy(g_state.solved, tempGrid, sizeof(g_state.solved));
    for (int r = 0; r < SIZE; r++)
        for (int c = 0; c < SIZE; c++)
            g_state.fixed[r][c] = (g_state.grid[r][c] != 0);

    g_state.is_user_input = false;
    return 1;
}

JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_handleInput(JNIEnv *env, jobject thiz, jint r, jint c, jint val) {
    if (!g_state.active) return 0;
    if (g_state.is_user_input) {
        g_state.grid[r][c] = val;
        return 1;
    }
    if (g_state.fixed[r][c]) return -2;

    if (g_state.solved[r][c] == val) {
        if (g_state.grid[r][c] == 0) g_state.score += 10;
        g_state.grid[r][c] = val;
        return 1;
    } else {
        g_state.mistakes++;
        return -1;
    }
}

JNIEXPORT void JNICALL
Java_com_sudoku_app_SudokuEngine_eraseCell(JNIEnv *env, jobject thiz, jint r, jint c) {
    if (g_state.is_user_input || !g_state.fixed[r][c]) {
        g_state.grid[r][c] = 0;
    }
}

JNIEXPORT void JNICALL
Java_com_sudoku_app_SudokuEngine_autoSolve(JNIEnv *env, jobject thiz) {
    memcpy(g_state.grid, g_state.solved, sizeof(g_state.grid));
    g_state.active = false;
}

JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_getCellValue(JNIEnv *env, jobject thiz, jint r, jint c) {
    return (jint)g_state.grid[r][c];
}

JNIEXPORT jboolean JNICALL
Java_com_sudoku_app_SudokuEngine_isCellFixed(JNIEnv *env, jobject thiz, jint r, jint c) {
    return (jboolean)g_state.fixed[r][c];
}

JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_getScore(JNIEnv *env, jobject thiz) { return (jint)g_state.score; }

JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_getMistakes(JNIEnv *env, jobject thiz) { return (jint)g_state.mistakes; }

JNIEXPORT jboolean JNICALL
Java_com_sudoku_app_SudokuEngine_isComplete(JNIEnv *env, jobject thiz) {
    for (int r = 0; r < SIZE; r++)
        for (int c = 0; c < SIZE; c++)
            if (g_state.grid[r][c] == 0) return false;
    return true;
}

// Legacy compatibility
JNIEXPORT jintArray JNICALL
Java_com_sudoku_app_SudokuEngine_generateSolvedGrid(JNIEnv *env, jobject thiz) {
    int grid[SIZE][SIZE] = {0}; fillGrid(grid); return gridToJArray(env, grid);
}
JNIEXPORT jintArray JNICALL
Java_com_sudoku_app_SudokuEngine_generatePuzzle(JNIEnv *env, jobject thiz, jintArray s, jint d) {
    int g[SIZE][SIZE]; jArrayToGrid(env, s, g);
    int h = (d==0?30:(d==1?45:55));
    int p[81]; for(int i=0;i<81;i++) p[i]=i;
    for(int i=80;i>0;i--){int j=rand()%(i+1);int t=p[i];p[i]=p[j];p[j]=t;}
    for(int k=0;k<h;k++) g[p[k]/9][p[k]%9]=0;
    return gridToJArray(env, g);
}
JNIEXPORT jintArray JNICALL
Java_com_sudoku_app_SudokuEngine_solve(JNIEnv *env, jobject thiz, jintArray p) {
    int g[SIZE][SIZE]; jArrayToGrid(env, p, g);
    if(!solveSudoku(g)) return NULL; return gridToJArray(env, g);
}
JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_isMoveValid(JNIEnv *env, jobject thiz, jintArray a, jint r, jint c, jint n) {
    int g[SIZE][SIZE]; jArrayToGrid(env, a, g); return isSafe(g, r, c, n) ? 1 : 0;
}
JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_validateGrid(JNIEnv *env, jobject thiz, jintArray a) {
    int g[SIZE][SIZE]; jArrayToGrid(env, a, g);
    for(int r=0;r<9;r++)for(int c=0;c<9;c++){int n=g[r][c];if(n==0)continue;g[r][c]=0;if(!isSafe(g,r,c,n)){g[r][c]=n;return 0;}g[r][c]=n;}
    return 1;
}
JNIEXPORT jint JNICALL
Java_com_sudoku_app_SudokuEngine_isPuzzleComplete(JNIEnv *env, jobject thiz, jintArray a) {
    int g[SIZE][SIZE]; jArrayToGrid(env, a, g);
    for(int r=0;r<9;r++)for(int c=0;c<9;c++)if(g[r][c]==0)return 0; return 1;
}
