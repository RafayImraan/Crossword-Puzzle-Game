package com.dsa.crossword.test;

import com.dsa.crossword.datastructures.HashIndexedWordBank;
import com.dsa.crossword.model.Direction;
import com.dsa.crossword.model.Grid;
import com.dsa.crossword.model.Slot;
import com.dsa.crossword.model.Word;
import com.dsa.crossword.util.PuzzleLoader;
import com.dsa.crossword.util.SolutionValidator;

import java.lang.reflect.Field;
import java.util.List;

public class Diag3 {
    public static void main(String[] args) throws Exception {
        List<Word> words = PuzzleLoader.fromResource("/puzzles/sample1.txt");
        HashIndexedWordBank bank = new HashIndexedWordBank();
        bank.addAll(words);

        // Try to build a tiny crossword manually
        // Place 4-letter anchor TREE
        Grid grid = new Grid(11, 11);
        Word anchor = bank.getByLength(4).get(0); // pick first
        int r = 5, c = 3;
        for (int i = 0; i < 4; i++) grid.set(r, c + i, anchor.charAt(i));
        System.out.println("Anchor placed: " + anchor.getText() + " at (" + r + "," + c + ")");
        System.out.println(grid.render());

        // What's the longest 4-letter word with T at position 0 (TREE's letter at offset 0)?
        // That's TREE's T. Words starting with T (length 4): TREE.
        // What's the longest 4-letter word with R at position 1? TREE.
        // For now just see all 4-letter matches per anchor position.
        for (int i = 0; i < 4; i++) {
            char l = anchor.charAt(i);
            List<Word> matches = bank.getMatching(4, i, l);
            System.out.println("Slot crossing col " + (c + i) + " (anchor letter '" + l + "'):");
            for (Word w : matches) System.out.println("  " + w);
        }
    }
}
