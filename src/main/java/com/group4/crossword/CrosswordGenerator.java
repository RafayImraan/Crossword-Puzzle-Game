package com.group4.crossword;

import java.util.*;

/**
 * Generates an interlocking crossword grid from a word list using
 * backtracking / constraint-satisfaction:
 *
 *   - Each word is a "variable".
 *   - Its domain is the set of legal (row, col, direction) placements
 *     that intersect an already-placed word on a matching letter.
 *   - The constraint is that every crossing letter must match, and no
 *     placement may create an invalid accidental adjacency.
 *   - On a dead end the last placement is undone (backtrack) and the
 *     next candidate is tried.
 *
 * The word list is sorted longest-first so the most-constrained word
 * (hardest to place later) anchors the grid first.
 */
public class CrosswordGenerator {

    private final HashIndex hashIndex = new HashIndex();
    private final List<WordEntry> words;
    private final int size;
    private Grid grid;
    private final List<PlacedWord> placements = new ArrayList<>();
    // Hash Set: tracks already-placed words so the same word is never placed twice.
    private final Set<WordEntry> usedWords = new HashSet<>();
    private final List<String> log = new ArrayList<>();

    public CrosswordGenerator(List<WordEntry> inputWords, int size) {
        // Longest word first -> most constrained, best anchor.
        this.words = new ArrayList<>(inputWords);
        this.words.sort((a, b) -> b.length() - a.length());
        this.size = size;
        this.hashIndex.build(this.words);
    }

    public List<PlacedWord> getPlacements() {
        return placements;
    }

    public Grid getGrid() {
        return grid;
    }

    public List<String> getLog() {
        return log;
    }

    /**
     * Runs the backtracking generator. Returns true if every word in the
     * list could be placed into a single interlocking grid.
     */
    public boolean generate() {
        grid = new Grid(size);
        placements.clear();
        usedWords.clear();
        log.clear();

        if (words.isEmpty()) return false;

        // Place the anchor word (longest) DOWN through the centre of the grid.
        WordEntry anchor = words.get(0);
        int startRow = Math.max(0, size / 2 - anchor.length() / 2);
        int col = size / 2;
        PlacedWord anchorPlacement = new PlacedWord(anchor, startRow, col, Direction.DOWN);
        place(anchorPlacement);
        placements.add(anchorPlacement);
        usedWords.add(anchor);
        log.add(words.size() + " words indexed. Anchor: " + anchor.getWord() + " placed DOWN.");

        boolean ok = backtrack(1);
        if (!ok) {
            log.add("FAILED: could not interlock all words into the grid.");
        } else {
            log.add("All " + words.size() + " words placed.");
        }
        return ok;
    }

    private boolean backtrack(int index) {
        if (index == words.size()) {
            return true;
        }

        WordEntry word = words.get(index);
        if (usedWords.contains(word)) {
            // Already placed elsewhere (defensive check against duplicate words) -> skip.
            return backtrack(index + 1);
        }

        List<PlacedWord> candidates = generateCandidates(word);

        for (PlacedWord candidate : candidates) {
            if (fits(candidate)) {
                place(candidate);
                placements.add(candidate);
                usedWords.add(word);
                log.add(word.getWord() + " fits at (" + candidate.getRow() + "," + candidate.getCol()
                        + ") " + candidate.getDirection());

                if (backtrack(index + 1)) {
                    return true;
                }

                // Dead end further down the tree -> undo and try next candidate.
                log.add(word.getWord() + " conflict later -> backtrack");
                unplace(candidate);
                placements.remove(placements.size() - 1);
                usedWords.remove(word);
            }
        }
        return false;
    }

    /**
     * Finds every position where `word` could cross an already-placed word
     * on a shared letter (its domain of legal placements).
     */
    private List<PlacedWord> generateCandidates(WordEntry word) {
        List<PlacedWord> candidates = new ArrayList<>();
        // Snapshot so we don't concurrently modify while iterating.
        List<PlacedWord> existingSnapshot = new ArrayList<>(placements);

        for (PlacedWord existing : existingSnapshot) {
            Direction newDirection = existing.getDirection() == Direction.DOWN
                    ? Direction.ACROSS : Direction.DOWN;

            for (int i = 0; i < existing.length(); i++) {
                char sharedLetter = existing.getWord().charAt(i);
                int r = existing.rowOf(i);
                int c = existing.colOf(i);

                for (int j = 0; j < word.length(); j++) {
                    // O(1) average hash lookup ("does `word` have `sharedLetter`
                    // at position j?") instead of a raw character comparison —
                    // this is the hash-index doing the pattern matching.
                    if (!hashIndex.matchesPattern(word, j, sharedLetter)) continue;

                    int startRow, startCol;
                    if (newDirection == Direction.ACROSS) {
                        startRow = r;
                        startCol = c - j;
                    } else {
                        startCol = c;
                        startRow = r - j;
                    }

                    PlacedWord candidate = new PlacedWord(word, startRow, startCol, newDirection);
                    if (inBoundsForWord(candidate)) {
                        candidates.add(candidate);
                    }
                }
            }
        }
        return candidates;
    }

    private boolean inBoundsForWord(PlacedWord p) {
        for (int i = 0; i < p.length(); i++) {
            if (!grid.inBounds(p.rowOf(i), p.colOf(i))) return false;
        }
        return true;
    }

    /**
     * Validates that a candidate placement is legal:
     *  - every crossing letter matches what's already on the grid
     *  - empty cells it passes through have empty perpendicular neighbours
     *    (so it doesn't silently graze another word and create an
     *    unintended extra word)
     *  - the cell immediately before/after the word (along its own axis)
     *    is empty or off-grid, so words don't run together
     *  - it actually intersects at least one existing word
     */
    private boolean fits(PlacedWord p) {
        if (!inBoundsForWord(p)) return false;

        boolean hasIntersection = false;

        for (int i = 0; i < p.length(); i++) {
            int r = p.rowOf(i);
            int c = p.colOf(i);
            char newCh = p.getWord().charAt(i);
            char existingCh = grid.get(r, c);

            if (existingCh != Grid.EMPTY) {
                if (existingCh != newCh) return false; // letter conflict
                hasIntersection = true;
            } else {
                // Empty cell: check perpendicular neighbours are also empty,
                // otherwise this letter would silently touch another word.
                if (p.getDirection() == Direction.ACROSS) {
                    if (!grid.isEmpty(r - 1, c) || !grid.isEmpty(r + 1, c)) return false;
                } else {
                    if (!grid.isEmpty(r, c - 1) || !grid.isEmpty(r, c + 1)) return false;
                }
            }
        }

        // Cell just before the start and just after the end must be empty/off-grid.
        int beforeRow = p.getDirection() == Direction.DOWN ? p.getRow() - 1 : p.getRow();
        int beforeCol = p.getDirection() == Direction.ACROSS ? p.getCol() - 1 : p.getCol();
        int afterRow = p.rowOf(p.length() - 1) + (p.getDirection() == Direction.DOWN ? 1 : 0);
        int afterCol = p.colOf(p.length() - 1) + (p.getDirection() == Direction.ACROSS ? 1 : 0);

        if (!grid.isEmpty(beforeRow, beforeCol)) return false;
        if (!grid.isEmpty(afterRow, afterCol)) return false;

        return hasIntersection;
    }

    private void place(PlacedWord p) {
        for (int i = 0; i < p.length(); i++) {
            grid.set(p.rowOf(i), p.colOf(i), p.getWord().charAt(i));
        }
    }

    /** Clears cells of `p` unless another still-placed word also occupies them. */
    private void unplace(PlacedWord p) {
        for (int i = 0; i < p.length(); i++) {
            int r = p.rowOf(i);
            int c = p.colOf(i);
            if (!ownedByOther(r, c, p)) {
                grid.set(r, c, Grid.EMPTY);
            }
        }
    }

    private boolean ownedByOther(int row, int col, PlacedWord exclude) {
        for (PlacedWord other : placements) {
            if (other == exclude) continue;
            for (int i = 0; i < other.length(); i++) {
                if (other.rowOf(i) == row && other.colOf(i) == col) return true;
            }
        }
        return false;
    }

    /**
     * Assigns standard crossword numbering: scan the grid top-to-bottom,
     * left-to-right; any cell that starts an Across or Down word gets the
     * next number.
     */
    public void numberSlots() {
        int number = 1;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                List<PlacedWord> startingHere = new ArrayList<>();
                for (PlacedWord p : placements) {
                    if (p.getRow() == r && p.getCol() == c) {
                        startingHere.add(p);
                    }
                }
                if (!startingHere.isEmpty()) {
                    for (PlacedWord p : startingHere) {
                        p.setNumber(number);
                    }
                    number++;
                }
            }
        }
    }
}
