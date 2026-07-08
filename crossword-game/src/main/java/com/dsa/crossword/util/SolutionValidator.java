package com.dsa.crossword.util;

import com.dsa.crossword.model.Grid;
import com.dsa.crossword.model.Slot;

/**
 * Validates a user-filled grid against the solution grid.
 * A cell is "correct" if userGrid[r][c] == solutionGrid[r][c] (or both are blank).
 * The puzzle is solved when every open cell of the solution matches.
 */
public final class SolutionValidator {

    private SolutionValidator() {}

    public static boolean isCellCorrect(Grid solution, char[][] userGrid, int r, int c) {
        if (!solution.inBounds(r, c)) return false;
        char sol = solution.get(r, c);
        char usr = userGrid[r][c];
        if (sol == Grid.BLOCKED) return true;
        return Character.toUpperCase(usr) == sol;
    }

    public static boolean isSolved(Grid solution, char[][] userGrid) {
        for (int r = 0; r < solution.rows(); r++) {
            for (int c = 0; c < solution.cols(); c++) {
                if (!isCellCorrect(solution, userGrid, r, c)) return false;
            }
        }
        return true;
    }

    /** Count correct cells, useful for a score. */
    public static int countCorrect(Grid solution, char[][] userGrid) {
        int n = 0;
        for (int r = 0; r < solution.rows(); r++) {
            for (int c = 0; c < solution.cols(); c++) {
                if (solution.get(r, c) != Grid.BLOCKED && isCellCorrect(solution, userGrid, r, c)) n++;
            }
        }
        return n;
    }

    /** Count total open cells in the solution. */
    public static int countOpen(Grid solution) {
        int n = 0;
        for (int r = 0; r < solution.rows(); r++)
            for (int c = 0; c < solution.cols(); c++)
                if (solution.get(r, c) != Grid.BLOCKED) n++;
        return n;
    }
}
