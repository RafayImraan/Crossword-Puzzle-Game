package com.group4.crossword;

/**
 * Per-word slot metadata once a word has been placed on the grid:
 * its origin position, direction, length and (later) its clue number.
 */
public class PlacedWord {

    private final WordEntry word;
    private final int row;
    private final int col;
    private final Direction direction;
    private int number = -1; // assigned during numbering phase

    public PlacedWord(WordEntry word, int row, int col, Direction direction) {
        this.word = word;
        this.row = row;
        this.col = col;
        this.direction = direction;
    }

    public WordEntry getWord() {
        return word;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    /** Row of the i-th letter of this word. */
    public int rowOf(int i) {
        return direction == Direction.DOWN ? row + i : row;
    }

    /** Column of the i-th letter of this word. */
    public int colOf(int i) {
        return direction == Direction.ACROSS ? col + i : col;
    }

    public int length() {
        return word.length();
    }

    @Override
    public String toString() {
        return word.getWord() + " @ (" + row + "," + col + ") " + direction;
    }
}
