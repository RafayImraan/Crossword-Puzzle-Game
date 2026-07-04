package com.group4.crossword;

import java.util.*;

/**
 * Hash-table based index over the word bank.
 *
 * Two hash maps are maintained:
 *  - byLength: length -> list of words of that length
 *  - byLetterPosition: "length#position#letter" -> list of words matching
 *    that letter at that position, for that length.
 *
 * This gives O(1) average lookup of candidate words that could fill a
 * partially-known slot pattern, instead of scanning the whole word bank
 * for every placement attempt.
 */
public class HashIndex {

    private final Map<Integer, List<WordEntry>> byLength = new HashMap<>();
    // Hash Set as the bucket value -> true O(1) average "does this word have
    // letter L at position P" membership check, not just an O(n) list scan.
    private final Map<String, Set<WordEntry>> byLetterPosition = new HashMap<>();

    public void build(List<WordEntry> words) {
        for (WordEntry w : words) {
            byLength.computeIfAbsent(w.length(), k -> new ArrayList<>()).add(w);
            for (int i = 0; i < w.length(); i++) {
                String key = keyFor(w.length(), i, w.charAt(i));
                byLetterPosition.computeIfAbsent(key, k -> new HashSet<>()).add(w);
            }
        }
    }

    private String keyFor(int length, int pos, char letter) {
        return length + "#" + pos + "#" + letter;
    }

    public List<WordEntry> allOfLength(int length) {
        return byLength.getOrDefault(length, Collections.emptyList());
    }

    /** Candidate words of the given length that have `letter` at position `pos`. */
    public Set<WordEntry> candidatesFor(int length, int pos, char letter) {
        return byLetterPosition.getOrDefault(keyFor(length, pos, letter), Collections.emptySet());
    }

    /** O(1) average check: does `word` have `letter` at position `pos`? */
    public boolean matchesPattern(WordEntry word, int pos, char letter) {
        return candidatesFor(word.length(), pos, letter).contains(word);
    }
}
