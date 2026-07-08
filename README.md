# Crossword Puzzle Game

A JavaFX-based Crossword Puzzle Game that generates and solves interlocking crossword puzzles using **Hashing** and **Backtracking**. Developed as a Data Structures & Algorithms (DSA) project, the application demonstrates the practical use of data structures and recursive search algorithms in generating valid crossword layouts.

---

## Features

- Interactive crossword puzzle interface built with JavaFX
- Automatic crossword generation using recursive backtracking
- Efficient word lookup through hash-based indexing
- Multiple built-in themed word packs
- Custom word list support
- Easy, Medium, and Hard difficulty levels
- Hint system with score deduction
- Real-time answer validation
- Puzzle timer and scoring system
- Numbered crossword grid with Across and Down clues
- Reveal solution option for testing and demonstration

---

## Technologies Used

- Java 17+
- JavaFX 21
- Maven
- Object-Oriented Programming
- Data Structures & Algorithms

---

## Data Structures & Algorithms

The project demonstrates the practical implementation of several core DSA concepts.

| Concept | Implementation |
|---------|----------------|
| **Hash Map** | `HashIndex.java` stores words using `length#position#letter` keys for efficient candidate retrieval. |
| **Hash Set** | Tracks previously placed words and stores indexed buckets to avoid duplicate placements. |
| **2D Array** | Represents the crossword grid. |
| **Recursion** | Recursive backtracking explores valid puzzle configurations. |
| **Backtracking** | Generates crossword layouts while reverting invalid placements. |
| **Lists** | Manage crossword slots and placed word metadata. |

---

## Project Structure

```
CrosswordGame
│
├── src
│   ├── CrosswordApp.java
│   ├── CrosswordGenerator.java
│   ├── Grid.java
│   ├── HashIndex.java
│   ├── PlacedWord.java
│   ├── WordEntry.java
│   └── Direction.java
│
├── pom.xml
└── README.md
```

---

## Algorithm Overview

### Hash-Based Word Indexing

Instead of checking every word character-by-character, the application builds a hash index where words are grouped by:

- Word length
- Character position
- Letter value

This allows candidate words to be retrieved using average **O(1)** hash lookups, significantly reducing unnecessary comparisons during puzzle generation.

---

### Backtracking Puzzle Generation

The crossword generator follows a recursive backtracking approach:

1. Identify all available crossword slots.
2. Select the next empty slot.
3. Retrieve candidate words from the hash index.
4. Attempt to place each valid candidate.
5. Continue recursively.
6. If no valid placement exists, backtrack and try another candidate.

This process continues until either:

- A complete crossword is generated, or
- All possibilities have been exhausted.

---

## Application Workflow

1. Load a word pack.
2. Generate crossword slots.
3. Build the hash index.
4. Generate the crossword using backtracking.
5. Display the puzzle and clues.
6. Allow users to solve the puzzle.
7. Validate answers and calculate the final score.

---

## Running the Project

### Requirements

- JDK 17 or later
- Maven

### Clone the Repository

```bash
git clone https://github.com/RafayImraan/Crossword-Puzzle-Game.git
cd Crossword-Puzzle-Game
```

### Run with Maven

```bash
mvn clean javafx:run
```

Maven will automatically download all required JavaFX dependencies during the first build.

---

## Using the Application

The application includes four predefined word packs:

- Data Structures
- Space
- Animals
- Sports

A **Custom** option also allows users to create their own crossword puzzles.

Custom words should be entered in the following format:

```
WORD:Clue
TREE:A data structure
ARRAY:Collection of elements
```

---

## Difficulty Levels

| Difficulty | Hints Available |
|------------|----------------|
| Easy | 10 |
| Medium | 4 |
| Hard | 0 |

---

## Scoring

The final score is calculated using:

```
Score = 1000 − (2 × Time in Seconds) − (50 × Hints Used)
```

The minimum possible score is **0**.

---

## Project Highlights

- Efficient hash-based word indexing
- Recursive backtracking algorithm
- Dynamic crossword generation
- Interactive JavaFX graphical interface
- Multiple difficulty levels
- Custom puzzle support
- Automatic clue generation and validation
- Clean object-oriented architecture

---

## Future Enhancements

Possible improvements include:

- Larger crossword grids
- Dictionary API integration
- Crossword import/export
- Save and resume puzzles
- Multiplayer mode
- Additional puzzle themes
- Leaderboards and achievements
- Improved crossword generation heuristics

---

## Academic Purpose

This project was developed as part of a **Data Structures & Algorithms** course to demonstrate the practical application of hashing, recursion, backtracking, and object-oriented design in solving a real-world problem.

---

## License

This project is intended for educational purposes.
