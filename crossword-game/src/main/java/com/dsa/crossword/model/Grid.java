package com.dsa.crossword.model;

/**
 * 2D char array representing the crossword grid.
 * ' ' (space) means blocked cell, a letter means filled.
 * O(1) indexed access to any cell.
 */
public class Grid {
    public static final char BLOCKED = ' ';
    private final int rows;
    private final int cols;
    private final char[][] cells;

    public Grid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new char[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                this.cells[r][c] = BLOCKED;
    }

    public int rows() { return rows; }
    public int cols() { return cols; }

    public char get(int r, int c)             { return cells[r][c]; }
    public void set(int r, int c, char ch)     { cells[r][c] = ch; }
    public boolean isBlocked(int r, int c)     { return cells[r][c] == BLOCKED; }

    public boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    /** Pretty-print the grid for debugging and console display. */
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (int c = 0; c < cols; c++) sb.append('-');
        sb.append("+\n");
        for (int r = 0; r < rows; r++) {
            sb.append('|');
            for (int c = 0; c < cols; c++) {
                char ch = cells[r][c];
                sb.append(ch == BLOCKED ? '.' : ch);
            }
            sb.append("|\n");
        }
        sb.append("+");
        for (int c = 0; c < cols; c++) sb.append('-');
        sb.append('+');
        return sb.toString();
    }
}
