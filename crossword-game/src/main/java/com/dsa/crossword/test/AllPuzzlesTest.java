package com.dsa.crossword.test;

import com.dsa.crossword.datastructures.HashIndexedWordBank;
import com.dsa.crossword.generator.CrosswordGenerator;
import com.dsa.crossword.model.Grid;
import com.dsa.crossword.model.Word;
import com.dsa.crossword.util.PuzzleLoader;
import com.dsa.crossword.util.SolutionValidator;

import java.util.List;

public class AllPuzzlesTest {
    public static void main(String[] args) throws Exception {
        for (String puzzle : new String[]{"sample1.txt", "sample2.txt", "sample3.txt"}) {
            System.out.println("\n========== " + puzzle + " ==========");
            List<Word> words = PuzzleLoader.fromResource("/puzzles/" + puzzle);
            System.out.println("Loaded " + words.size() + " words.");

            HashIndexedWordBank bank = new HashIndexedWordBank();
            bank.addAll(words);
            CrosswordGenerator gen = new CrosswordGenerator(bank);
            CrosswordGenerator.GenerationResult res = gen.generate();

            if (!res.success) {
                System.out.println("FAILED: " + res.message);
                continue;
            }
            System.out.println("Anchor: " + res.anchorWord.getText());
            System.out.println("Backtrack steps: " + res.backtrackSteps);
            System.out.println(res.grid.render());
            System.out.println("Slots:");
            for (var s : res.slots) {
                System.out.println("  #" + s.getNumber() + " " + s.getDirection()
                        + " @ (" + s.getRow() + "," + s.getCol() + "): "
                        + s.getWord().getText() + " - " + s.getWord().getClue());
            }
            // Verify solution
            char[][] user = new char[res.grid.rows()][res.grid.cols()];
            for (int r = 0; r < res.grid.rows(); r++)
                for (int c = 0; c < res.grid.cols(); c++)
                    user[r][c] = res.grid.get(r, c) == Grid.BLOCKED ? ' ' : res.grid.get(r, c);
            System.out.println("isSolved: " + SolutionValidator.isSolved(res.grid, user));
        }
    }
}
