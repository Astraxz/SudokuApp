package com.sudoku.app;

public class SudokuEngine {
    static {
        System.loadLibrary("sudoku");
    }

    // --- State Management in C ---
    
    // Initialize a new automatic game
    public native void initNewGame(int difficulty);
    
    // Initialize user input mode
    public native void initUserInputMode();
    
    // Validate user input and start game. Returns: 1=Success, 0=Unsolvable, -1=Conflict
    public native int validateAndStart();
    
    // Process a number input. Returns: 1=Correct, -1=Wrong, -2=Fixed, 0=GameInactive
    public native int handleInput(int row, int col, int value);
    
    // Erase a cell
    public native void eraseCell(int row, int col);
    
    // Fill the board with the solution
    public native void autoSolve();
    
    // Get state from C
    public native int getCellValue(int row, int col);
    public native boolean isCellFixed(int row, int col);
    public native int getScore();
    public native int getMistakes();
    public native boolean isComplete();

    // --- Original Core Logic (Keep for utility if needed) ---
    public native int[] generateSolvedGrid();
    public native int[] generatePuzzle(int[] solvedGrid, int difficulty);
    public native int[] solve(int[] puzzle);
    public native int isMoveValid(int[] grid, int row, int col, int num);
    public native int validateGrid(int[] grid);
    public native int isPuzzleComplete(int[] grid);
}
