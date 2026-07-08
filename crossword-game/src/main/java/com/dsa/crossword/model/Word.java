package com.dsa.crossword.model;

/**
 * Represents a single word entry with its clue.
 * Model layer - a plain data holder.
 */
public class Word {
    private final String text;
    private final String clue;
    private final int number;   // crossword number, assigned during grid layout

    public Word(String text, String clue) {
        this.text = text == null ? "" : text.trim().toUpperCase();
        this.clue = clue == null ? "" : clue.trim();
        this.number = 0;
    }

    public Word(int number, String text, String clue) {
        this.number = number;
        this.text = text == null ? "" : text.trim().toUpperCase();
        this.clue = clue == null ? "" : clue.trim();
    }

    public String getText()     { return text; }
    public String getClue()     { return clue; }
    public int    getNumber()   { return number; }
    public int    length()      { return text.length(); }
    public char   charAt(int i) { return text.charAt(i); }

    @Override
    public String toString() {
        return text + " (" + clue + ")";
    }
}
