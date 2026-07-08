package com.dsa.crossword.model;

/**
 * A Slot is a position on the grid where a word can be placed.
 * It is created with a direction and length; once placed, {@link #getWord()}
 * holds the actual word occupying it.
 */
public class Slot {
    private final int row;
    private final int col;
    private final Direction direction;
    private final int length;
    private Word word;            // null until placed
    private int number;           // crossword number, assigned in numbering pass

    public Slot(int row, int col, Direction direction, int length) {
        this.row = row;
        this.col = col;
        this.direction = direction;
        this.length = length;
    }

    public int        getRow()        { return row; }
    public int        getCol()        { return col; }
    public Direction  getDirection()  { return direction; }
    public int        getLength()     { return length; }
    public Word       getWord()       { return word; }
    public int        getNumber()     { return number; }

    public void setWord(Word word)   { this.word = word; }
    public void clearWord()          { this.word = null; }
    public boolean isEmpty()          { return word == null; }
    public void    setNumber(int n)  { this.number = n; }

    /** Return the (row,col) of the i-th cell of this slot. */
    public int[] cellAt(int i) {
        if (direction == Direction.ACROSS) return new int[]{row, col + i};
        else                              return new int[]{row + i, col};
    }

    @Override
    public String toString() {
        return "Slot(" + row + "," + col + "," + direction + "," + length + ")";
    }
}
