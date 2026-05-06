package com.sudoku.app;

public class SudokuEngine {
    static {
        System.loadLibrary("sudoku");
    }

    // Returns a flat int[81] of a fully solved grid
    public native int[] generateSolvedGrid();

    // Given solved int[81] and difficulty (0=Easy,1=Medium,2=Hard),
    // returns puzzle int[81] with cells blanked out
    public native int[] generatePuzzle(int[] solvedGrid, int difficulty);

    // Returns solved int[81] or null if unsolvable
    public native int[] solve(int[] puzzle);

    // 1 = valid move, 0 = conflict
    public native int isMoveValid(int[] grid, int row, int col, int num);

    // 1 = valid starting grid, 0 = conflicts present
    public native int validateGrid(int[] grid);

    // 1 = puzzle complete (no empty cells), 0 = cells remain
    public native int isPuzzleComplete(int[] grid);
}
