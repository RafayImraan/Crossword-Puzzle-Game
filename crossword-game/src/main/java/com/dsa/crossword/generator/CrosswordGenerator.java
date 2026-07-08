package com.dsa.crossword.generator;

import com.dsa.crossword.datastructures.HashIndexedWordBank;
import com.dsa.crossword.model.Direction;
import com.dsa.crossword.model.Grid;
import com.dsa.crossword.model.Slot;
import com.dsa.crossword.model.Word;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * CrosswordGenerator
 * ------------------
 * Uses recursive backtracking / constraint satisfaction to place every word
 * from the hash-indexed word bank on one interlocking crossword grid.
 *
 * DSA topics demonstrated:
 *   - Hash table lookup  (HashIndexedWordBank)
 *   - Backtracking        (recursive search with undo)
 *   - 2D array indexing   (Grid)
 *   - HashSet             (used-word tracking)
 *   - Slot list           (row, column, direction, length, clue metadata)
 */
public class CrosswordGenerator {

    private static final int MAX_BACKTRACK_STEPS = 80_000;
    private static final int MAX_GRID_SIZE = 25;

    private final HashIndexedWordBank bank;
    private final Random random = new Random();
    private int backtrackSteps;

    public CrosswordGenerator(HashIndexedWordBank bank) {
        this.bank = bank;
    }

    /** Generate a crossword, trying several anchor orders before failing. */
    public GenerationResult generate() {
        return generate(100);
    }

    public GenerationResult generate(int attempts) {
        List<Word> baseWords = new ArrayList<>(bank.getAllWords());
        if (baseWords.isEmpty()) return GenerationResult.failure("No words in word bank.");

        baseWords.sort(Comparator.comparingInt(Word::length).reversed());
        int gridSize = chooseGridSize(baseWords);

        for (int attempt = 0; attempt < attempts; attempt++) {
            clearUsedWords();
            List<Word> words = new ArrayList<>(baseWords);
            shuffleSameLengthGroups(words);
            Collections.rotate(words, attempt % words.size());

            GenerationResult result = generateOnce(words, gridSize);
            if (result.success) return result;
        }

        clearUsedWords();
        return GenerationResult.failure("Failed to place all words after " + attempts + " attempts.");
    }

    private GenerationResult generateOnce(List<Word> words, int gridSize) {
        backtrackSteps = 0;

        Grid grid = new Grid(gridSize, gridSize);
        List<Slot> slots = new ArrayList<>();
        Word anchor = words.get(0);
        Slot anchorSlot = new Slot(gridSize / 2, (gridSize - anchor.length()) / 2,
                Direction.ACROSS, anchor.length());

        place(grid, anchorSlot, anchor);
        bank.markUsed(anchor);
        slots.add(anchorSlot);

        boolean solved = placeRemaining(grid, words, 1, slots);
        if (!solved) {
            clearUsedWords();
            return GenerationResult.failure("Could not fit all words for anchor " + anchor.getText() + ".");
        }

        assignNumbers(grid, slots);
        return GenerationResult.success(grid, slots, anchorSlot, anchor, backtrackSteps);
    }

    private int chooseGridSize(List<Word> words) {
        int longest = words.stream().mapToInt(Word::length).max().orElse(9);
        int totalLetters = words.stream().mapToInt(Word::length).sum();
        int estimated = (int) Math.ceil(Math.sqrt(totalLetters * 3.0)) + longest / 2;
        return Math.min(MAX_GRID_SIZE, Math.max(Math.max(15, longest + 4), estimated));
    }

    private void shuffleSameLengthGroups(List<Word> words) {
        int start = 0;
        while (start < words.size()) {
            int end = start + 1;
            while (end < words.size() && words.get(end).length() == words.get(start).length()) end++;
            Collections.shuffle(words.subList(start, end), random);
            start = end;
        }
    }

    private boolean placeRemaining(Grid grid, List<Word> words, int index, List<Slot> slots) {
        if (index == words.size()) return true;
        if (backtrackSteps > MAX_BACKTRACK_STEPS) return false;

        Word word = words.get(index);
        List<Candidate> candidates = findCandidates(grid, word);
        if (candidates.isEmpty()) return false;

        for (Candidate candidate : candidates) {
            if (bank.isUsed(word)) continue;
            if (!fits(grid, candidate.slot, word, true)) continue;

            List<int[]> changedCells = place(grid, candidate.slot, word);
            bank.markUsed(word);
            slots.add(candidate.slot);
            backtrackSteps++;

            if (placeRemaining(grid, words, index + 1, slots)) return true;

            slots.remove(slots.size() - 1);
            remove(grid, candidate.slot, changedCells);
            bank.markUnused(word);
        }

        return false;
    }

    private List<Candidate> findCandidates(Grid grid, Word word) {
        List<Candidate> candidates = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) {
                char gridChar = grid.get(r, c);
                if (gridChar == Grid.BLOCKED) continue;

                for (int i = 0; i < word.length(); i++) {
                    if (word.charAt(i) != gridChar) continue;
                    addCandidate(grid, word, new Slot(r, c - i, Direction.ACROSS, word.length()), seen, candidates);
                    addCandidate(grid, word, new Slot(r - i, c, Direction.DOWN, word.length()), seen, candidates);
                }
            }
        }

        candidates.sort(Comparator.comparingInt(Candidate::crossings).reversed());
        return candidates;
    }

    private void addCandidate(Grid grid, Word word, Slot slot, Set<String> seen, List<Candidate> candidates) {
        String key = slot.getRow() + "," + slot.getCol() + "," + slot.getDirection() + "," + slot.getLength();
        if (!seen.add(key)) return;
        if (!fits(grid, slot, word, true)) return;
        candidates.add(new Candidate(slot, countCrossings(grid, slot)));
    }

    private int countCrossings(Grid grid, Slot slot) {
        int crossings = 0;
        for (int i = 0; i < slot.getLength(); i++) {
            int[] cell = slot.cellAt(i);
            if (grid.get(cell[0], cell[1]) != Grid.BLOCKED) crossings++;
        }
        return crossings;
    }

    /** Check bounds, matching crossings, and standard crossword adjacency rules. */
    private boolean fits(Grid grid, Slot slot, Word word, boolean requireCrossing) {
        if (slot.getLength() != word.length()) return false;
        if (!grid.inBounds(slot.getRow(), slot.getCol())) return false;

        int[] last = slot.cellAt(slot.getLength() - 1);
        if (!grid.inBounds(last[0], last[1])) return false;

        Direction d = slot.getDirection();
        int beforeRow = slot.getRow() - (d == Direction.DOWN ? 1 : 0);
        int beforeCol = slot.getCol() - (d == Direction.ACROSS ? 1 : 0);
        int afterRow = last[0] + (d == Direction.DOWN ? 1 : 0);
        int afterCol = last[1] + (d == Direction.ACROSS ? 1 : 0);
        if (grid.inBounds(beforeRow, beforeCol) && !grid.isBlocked(beforeRow, beforeCol)) return false;
        if (grid.inBounds(afterRow, afterCol) && !grid.isBlocked(afterRow, afterCol)) return false;

        int crossings = 0;
        for (int i = 0; i < word.length(); i++) {
            int[] cell = slot.cellAt(i);
            int r = cell[0], c = cell[1];
            char existing = grid.get(r, c);

            if (existing != Grid.BLOCKED) {
                if (existing != word.charAt(i)) return false;
                crossings++;
                continue;
            }

            if (d == Direction.ACROSS) {
                if (grid.inBounds(r - 1, c) && !grid.isBlocked(r - 1, c)) return false;
                if (grid.inBounds(r + 1, c) && !grid.isBlocked(r + 1, c)) return false;
            } else {
                if (grid.inBounds(r, c - 1) && !grid.isBlocked(r, c - 1)) return false;
                if (grid.inBounds(r, c + 1) && !grid.isBlocked(r, c + 1)) return false;
            }
        }

        return !requireCrossing || crossings > 0;
    }

    /** Write only new letters and return those cells for precise undo. */
    private List<int[]> place(Grid grid, Slot slot, Word word) {
        List<int[]> changed = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            int[] cell = slot.cellAt(i);
            if (grid.get(cell[0], cell[1]) == Grid.BLOCKED) {
                grid.set(cell[0], cell[1], word.charAt(i));
                changed.add(cell);
            }
        }
        slot.setWord(word);
        return changed;
    }

    private void remove(Grid grid, Slot slot, List<int[]> changedCells) {
        for (int[] cell : changedCells) grid.set(cell[0], cell[1], Grid.BLOCKED);
        slot.clearWord();
    }

    /** Assign crossword numbers in row-major order. Across/down at same start share one number. */
    private void assignNumbers(Grid grid, List<Slot> slots) {
        slots.sort(Comparator.comparingInt(Slot::getRow)
                .thenComparingInt(Slot::getCol)
                .thenComparing(s -> s.getDirection() == Direction.ACROSS ? 0 : 1));

        int next = 1;
        int[][] numbers = new int[grid.rows()][grid.cols()];
        for (Slot slot : slots) {
            int r = slot.getRow();
            int c = slot.getCol();
            if (numbers[r][c] == 0) numbers[r][c] = next++;
            slot.setNumber(numbers[r][c]);
        }
    }

    private void clearUsedWords() {
        for (String word : new ArrayList<>(bank.getUsedWords())) bank.markUnused(word);
    }

    private record Candidate(Slot slot, int crossings) {}

    public static final class GenerationResult {
        public final boolean success;
        public final Grid grid;
        public final List<Slot> slots;
        public final Slot anchorSlot;
        public final Word anchorWord;
        public final int backtrackSteps;
        public final String message;

        private GenerationResult(boolean success, Grid grid, List<Slot> slots,
                                Slot anchorSlot, Word anchorWord,
                                int backtrackSteps, String message) {
            this.success = success;
            this.grid = grid;
            this.slots = slots;
            this.anchorSlot = anchorSlot;
            this.anchorWord = anchorWord;
            this.backtrackSteps = backtrackSteps;
            this.message = message;
        }

        public static GenerationResult success(Grid grid, List<Slot> slots, Slot anchor, Word word, int steps) {
            return new GenerationResult(true, grid, slots, anchor, word, steps, "OK");
        }

        public static GenerationResult failure(String message) {
            return new GenerationResult(false, null, null, null, null, 0, message);
        }
    }
}
