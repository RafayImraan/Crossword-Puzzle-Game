package com.dsa.crossword.test;

import com.dsa.crossword.datastructures.HashIndexedWordBank;
import com.dsa.crossword.generator.CrosswordGenerator;
import com.dsa.crossword.model.Grid;
import com.dsa.crossword.model.Slot;
import com.dsa.crossword.model.Word;
import com.dsa.crossword.util.PuzzleLoader;
import com.dsa.crossword.util.SolutionValidator;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Headless smoke test - exercises the DSA pipeline without JavaFX.
 * Run with:
 *   java -cp out:resources com.dsa.crossword.test.SmokeTest
 */
public class SmokeTest {

    public static void main(String[] args) throws Exception {
        // Load via the loader (uses classpath / resources/...)
        InputStream is = SmokeTest.class.getResourceAsStream("/puzzles/sample1.txt");
        if (is == null) {
            System.out.println("Resource not found. Working dir: " + System.getProperty("user.dir"));
            return;
        }
        List<Word> words = PuzzleLoader.fromResource("/puzzles/sample1.txt");
        System.out.println("Loaded " + words.size() + " words:");
        for (Word w : words) System.out.println("  " + w);

        HashIndexedWordBank bank = new HashIndexedWordBank();
        bank.addAll(words);
        System.out.println("\nHash bank total: " + bank.totalWords());
        System.out.println("Sample length-4 list size: " + bank.getByLength(4).size());
        System.out.println("Words with letter 'R' at position 1 (len 4): "
                + bank.getMatching(4, 1, 'R').size());
        for (Word w : bank.getMatching(4, 1, 'R')) {
            System.out.println("  match -> " + w);
        }

        CrosswordGenerator gen = new CrosswordGenerator(bank);
        CrosswordGenerator.GenerationResult res = gen.generate();

        if (!res.success) {
            System.out.println("\nGeneration FAILED: " + res.message);
            return;
        }

        System.out.println("\nAnchor: " + res.anchorWord.getText()
                + " (" + res.anchorWord.getClue() + ")");
        System.out.println("Backtrack steps: " + res.backtrackSteps);
        System.out.println("Words placed: " + res.slots.size());

        System.out.println("\nGrid:");
        System.out.println(res.grid.render());

        System.out.println("\nSlots:");
        for (Slot s : res.slots) {
            System.out.println("  #" + s.getNumber() + " " + s.getDirection()
                    + " @ (" + s.getRow() + "," + s.getCol() + "): "
                    + s.getWord().getText() + " - " + s.getWord().getClue());
        }

        // Validation: fill user grid with solution and check
        char[][] user = new char[res.grid.rows()][res.grid.cols()];
        for (int r = 0; r < res.grid.rows(); r++)
            for (int c = 0; c < res.grid.cols(); c++) {
                if (res.grid.get(r, c) != Grid.BLOCKED) {
                    user[r][c] = res.grid.get(r, c);
                } else {
                    user[r][c] = ' ';
                }
            }
        System.out.println("\nValidation:");
        System.out.println("  isSolved = " + SolutionValidator.isSolved(res.grid, user));
        System.out.println("  correct = " + SolutionValidator.countCorrect(res.grid, user));
        System.out.println("  open    = " + SolutionValidator.countOpen(res.grid));
    }
}
