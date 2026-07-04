package com.group4.crossword;

import java.util.Objects;

/**
 * A single dictionary entry: the answer word plus its clue text.
 * Words are normalized to uppercase, no spaces.
 */
public class WordEntry {

    private final String word;
    private final String clue;

    public WordEntry(String word, String clue) {
        this.word = word.trim().toUpperCase().replace(" ", "");
        this.clue = clue.trim();
    }

    public String getWord() {
        return word;
    }

    public String getClue() {
        return clue;
    }

    public int length() {
        return word.length();
    }

    public char charAt(int i) {
        return word.charAt(i);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WordEntry)) return false;
        WordEntry other = (WordEntry) o;
        return word.equals(other.word);
    }

    @Override
    public int hashCode() {
        return Objects.hash(word);
    }

    @Override
    public String toString() {
        return word + " (" + clue + ")";
    }
}
