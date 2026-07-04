package com.group4.crossword;

/**
 * The crossword grid itself: a 2D array giving O(1) indexed access to any cell.
 */
public class Grid {

    public static final char EMPTY = '.';

    private final char[][] cells;
    private final int size;

    public Grid(int size) {
        this.size = size;
        this.cells = new char[size][size];
        for (char[] row : cells) {
            java.util.Arrays.fill(row, EMPTY);
        }
    }

    public int size() {
        return size;
    }

    public boolean inBounds(int row, int col) {
        return row >= 0 && row < size && col >= 0 && col < size;
    }

    public char get(int row, int col) {
        if (!inBounds(row, col)) return EMPTY;
        return cells[row][col];
    }

    public void set(int row, int col, char c) {
        cells[row][col] = c;
    }

    public boolean isEmpty(int row, int col) {
        return get(row, col) == EMPTY;
    }
}
