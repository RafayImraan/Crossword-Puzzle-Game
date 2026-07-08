package com.dsa.crossword.model;

/**
 * Direction in which a word is placed on the grid.
 * Using an enum keeps the direction type-safe and limited to two values.
 */
public enum Direction {
    ACROSS,   // horizontal (row constant, col increases)
    DOWN;     // vertical   (col constant, row increases)

    public Direction opposite() {
        return this == ACROSS ? DOWN : ACROSS;
    }
}
