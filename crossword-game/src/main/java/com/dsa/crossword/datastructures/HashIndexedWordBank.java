package com.dsa.crossword.datastructures;

import com.dsa.crossword.model.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HashIndexedWordBank
 * --------------------
 * Indexes the word bank in two ways for O(1)-average lookup:
 *
 *  1) byLength   : length -> list of words of that length
 *  2) byLengthLetterIndex : (length, letterIndex, letter) -> list of words
 *     (used for fast pattern matching: "give me all 5-letter words
 *      whose 2nd letter is 'R'")
 *
 * Plus a HashSet of already-placed words so we never place a word twice.
 *
 * This is the HASH TABLE component of the project.
 */
public class HashIndexedWordBank {

    /** length -> all words of that length */
    private final Map<Integer, List<Word>> byLength = new HashMap<>();

    /** (length, letterIndex, upper-case letter) -> words matching at that position */
    private final Map<LengthLetterKey, List<Word>> byLengthLetterIndex = new HashMap<>();

    /** Tracks words already placed on the grid to prevent duplicates */
    private final Set<String> usedWords = new HashSet<>();

    /** Insert a single word into the indices. */
    public void add(Word w) {
        String text = w.getText();
        int len = text.length();
        byLength.computeIfAbsent(len, k -> new ArrayList<>()).add(w);
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            if (c == ' ') continue; // skip blanks within the word
            LengthLetterKey key = new LengthLetterKey(len, i, c);
            byLengthLetterIndex.computeIfAbsent(key, k -> new ArrayList<>()).add(w);
        }
    }

    /** Bulk insert from a list. */
    public void addAll(List<Word> words) {
        for (Word w : words) add(w);
    }

    /** Get all words of a given length. */
    public List<Word> getByLength(int length) {
        return byLength.getOrDefault(length, Collections.emptyList());
    }

    /** Return every word currently indexed by this bank. */
    public List<Word> getAllWords() {
        List<Word> words = new ArrayList<>();
        for (List<Word> list : byLength.values()) words.addAll(list);
        return words;
    }

    /**
     * Get all words of {@code length} whose character at position
     * {@code letterIndex} equals {@code letter}.
     * This is the lookup that drives the backtracking generator:
     * for a partially filled slot, the generator knows the pattern of
     * filled letters and asks for matching candidates in O(1) average.
     */
    public List<Word> getMatching(int length, int letterIndex, char letter) {
        LengthLetterKey key = new LengthLetterKey(length, letterIndex, Character.toUpperCase(letter));
        List<Word> list = byLengthLetterIndex.get(key);
        return list == null ? Collections.emptyList() : list;
    }

    /** Mark a word as used (placed on the grid). */
    public void markUsed(Word w) {
        usedWords.add(w.getText());
    }

    /** Unmark a word (when backtracking). */
    public void markUnused(Word w) {
        usedWords.remove(w.getText());
    }

    /** Unmark by text. */
    public void markUnused(String text) {
        usedWords.remove(text);
    }

    public boolean isUsed(Word w) {
        return usedWords.contains(w.getText());
    }

    public int totalWords() {
        int total = 0;
        for (List<Word> list : byLength.values()) total += list.size();
        return total;
    }

    public Set<String> getUsedWords() {
        return Collections.unmodifiableSet(usedWords);
    }

    /** Composite key: (length, letterIndex, letter). */
    public static final class LengthLetterKey {
        private final int length;
        private final int letterIndex;
        private final char letter;

        public LengthLetterKey(int length, int letterIndex, char letter) {
            this.length = length;
            this.letterIndex = letterIndex;
            this.letter = Character.toUpperCase(letter);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LengthLetterKey)) return false;
            LengthLetterKey k = (LengthLetterKey) o;
            return length == k.length && letterIndex == k.letterIndex && letter == k.letter;
        }

        @Override
        public int hashCode() {
            int h = length;
            h = 31 * h + letterIndex;
            h = 31 * h + letter;
            return h;
        }

        @Override
        public String toString() {
            return "Key(" + length + "," + letterIndex + ",'" + letter + "')";
        }
    }
}
