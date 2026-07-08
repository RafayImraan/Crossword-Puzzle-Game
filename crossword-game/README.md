# Crossword Puzzle Game

Data Structures & Algorithms project for building an interlocking crossword puzzle generator and solver using hashing, backtracking, and a JavaFX UI.

## Proposal Coverage

| Proposal requirement | Implementation |
| --- | --- |
| Load words and clues | `PuzzleLoader` reads `WORD = clue` text files from resources or disk |
| Hash table candidate lookup | `HashIndexedWordBank` indexes words by length and by `(length, letter index, letter)` |
| Backtracking / CSP generator | `CrosswordGenerator` recursively places every word, backtracks on conflicts, and preserves crossing letters during undo |
| 2D grid | `Grid` stores the solution in a `char[][]` with O(1) indexed access |
| Slot metadata and numbering | `Slot` stores row, column, direction, length, clue word, and crossword number |
| Solution validation | `SolutionValidator` checks cells, counts correct cells, and detects solved puzzles |
| Interactive UI | `CrosswordApp` renders the numbered grid, Across/Down clues, timer, checking, reveal, hint, and clear actions |

## Project Structure

```text
src/main/java/com/dsa/crossword/
  datastructures/HashIndexedWordBank.java
  generator/CrosswordGenerator.java
  model/Direction.java
  model/Grid.java
  model/Slot.java
  model/Word.java
  ui/CrosswordApp.java
  util/PuzzleLoader.java
  util/SolutionValidator.java
  test/SmokeTest.java
  test/AllPuzzlesTest.java

src/main/resources/
  com/dsa/crossword/ui/style.css
  puzzles/sample1.txt
  puzzles/sample2.txt
  puzzles/sample3.txt
```

## Requirements

- JDK 17 or newer
- Maven

## Run the JavaFX App

```powershell
mvn javafx:run
```

## Build

```powershell
mvn package
```

## Headless Tests

These tests exercise loading, hashing, generation, and validation without opening the JavaFX UI.

```powershell
mvn -q compile exec:java "-Dexec.mainClass=com.dsa.crossword.test.SmokeTest"
mvn -q compile exec:java "-Dexec.mainClass=com.dsa.crossword.test.AllPuzzlesTest"
```

If the Maven exec plugin is unavailable, compile with Maven and run through the generated classpath from your IDE.

## Puzzle File Format

Each puzzle file is a plain text word bank:

```text
TREE = A hierarchical data structure with a root and child nodes
ARRAY = A contiguous block of memory storing elements of the same type
```

Blank lines and lines beginning with `#` are ignored.

## Implemented Features

- Generates a crossword from built-in or custom word lists.
- Places all words when a valid interlocking layout exists.
- Uses recursive backtracking and undo on failed placements.
- Shows numbered Across and Down clue panels.
- Accepts one letter per grid cell.
- Validates letters in real time with correct/wrong highlighting.
- Shows progress, elapsed time, and completion dialog.
- Supports hint, check, reveal solution, clear, and new puzzle controls.
